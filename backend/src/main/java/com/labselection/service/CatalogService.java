package com.labselection.service;

import com.labselection.auth.AuthContext;
import com.labselection.auth.CurrentUser;
import com.labselection.auth.PasswordService;
import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.common.PageResult;
import com.labselection.domain.entity.Announcement;
import com.labselection.domain.entity.Lab;
import com.labselection.domain.entity.Mentor;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.entity.Student;
import com.labselection.domain.entity.SysUser;
import com.labselection.domain.enums.AnnouncementStatus;
import com.labselection.domain.enums.LabStatus;
import com.labselection.domain.enums.Role;
import com.labselection.dto.MentorSaveRequest;
import com.labselection.dto.StudentSaveRequest;
import com.labselection.mapper.AnnouncementMapper;
import com.labselection.mapper.AssignmentMapper;
import com.labselection.mapper.LabMapper;
import com.labselection.mapper.MentorMapper;
import com.labselection.mapper.OfferMapper;
import com.labselection.mapper.StudentMapper;
import com.labselection.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final StageGuard stageGuard;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final OfferMapper offerMapper;
    private final AssignmentMapper assignmentMapper;
    private final AnnouncementMapper announcementMapper;
    private final PasswordService passwordService;
    private final AuditService auditService;

    public List<Lab> labs() {
        SelectionBatch batch = stageGuard.current();
        Long collegeId = currentCollegeId();
        List<Lab> labs = labMapper.listWithStats(batch.getId(), collegeId);
        for (Lab lab : labs) {
            lab.setMentors(mentorMapper.listWithStats(batch.getId(), collegeId, lab.getId()));
            long used = Math.max(offerMapper.countActiveByLab(lab.getId(), batch.getId()),
                    assignmentMapper.countByLab(lab.getId(), batch.getId()));
            lab.setRemainingQuota(Math.max(0, lab.getTotalQuota() - (int) used));
        }
        return labs;
    }

    @Transactional
    public Lab saveLab(Lab request, Long id) {
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertNotLocked(batch.getId());
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实训室名称不能为空");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "实训室编码不能为空");
        }
        if (request.getTotalQuota() == null || request.getTotalQuota() < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "总名额不能为负数");
        }
        Lab sameCode = labMapper.findByCode(request.getCode().trim());
        if (sameCode != null && !Objects.equals(sameCode.getId(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "实训室编码已存在");
        }
        int mentorQuotaSum = mentorMapper.findByLabId(id == null ? -1L : id).stream()
                .filter(m -> Boolean.TRUE.equals(m.getEnabled()))
                .mapToInt(Mentor::getQuota).sum();
        if (id != null && mentorQuotaSum > request.getTotalQuota()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "总名额不能小于现有导师名额之和（" + mentorQuotaSum + "）");
        }

        if (id == null) {
            request.setCollegeId(currentCollegeId());
            request.setStatus(request.getStatus() == null ? LabStatus.ACTIVE : request.getStatus());
            labMapper.insert(request);
            auditService.log(batch.getId(), "LAB_CREATE", "LAB", request.getId(),
                    "新建实训室：" + request.getName(), null);
            return labMapper.findById(request.getId());
        }
        Lab existing = labMapper.findById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "实训室不存在");
        }
        assertSameCollege(existing.getCollegeId());
        request.setCollegeId(existing.getCollegeId());
        request.setId(id);
        request.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        try {
            labMapper.update(request);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "实训室编码已存在");
        }
        auditService.log(batch.getId(), "LAB_UPDATE", "LAB", id,
                "修改实训室：" + request.getName(), null);
        return labMapper.findById(id);
    }

    @Transactional
    public void toggleLab(Long id, boolean enabled) {
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertNotLocked(batch.getId());
        Lab lab = labMapper.findById(id);
        if (lab == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "实训室不存在");
        }
        assertSameCollege(lab.getCollegeId());
        lab.setStatus(enabled ? LabStatus.ACTIVE : LabStatus.DISABLED);
        labMapper.update(lab);
        auditService.log(batch.getId(), "LAB_STATUS", "LAB", id,
                enabled ? "启用实训室" : "停用实训室", null);
    }

    public PageResult<Mentor> mentors(String keyword, Long labId, int page, int size) {
        SelectionBatch batch = stageGuard.current();
        List<Mentor> all = mentorMapper.listWithStats(batch.getId(), currentCollegeId(), labId);
        if (keyword != null && !keyword.isBlank()) {
            String key = keyword.trim();
            all = all.stream()
                    .filter(m -> contains(m.getName(), key)
                            || contains(m.getMentorNo(), key)
                            || contains(m.getResearchDirection(), key))
                    .toList();
        }
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        int from = Math.min(all.size(), (safePage - 1) * safeSize);
        int to = Math.min(all.size(), from + safeSize);
        return PageResult.of(new ArrayList<>(all.subList(from, to)), all.size(), safePage, safeSize);
    }

    @Transactional
    public Mentor createMentor(MentorSaveRequest request) {
        String account = request.account() == null || request.account().isBlank()
                ? nextMentorNo() : request.account().trim();
        if (userMapper.findByAccount(account) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "账号已存在");
        }
        Lab lab = labMapper.findById(request.labId());
        if (lab == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "实训室不存在");
        }
        assertSameCollege(lab.getCollegeId());
        assertMentorQuota(lab, request.quota(), null);
        String password = request.password() == null || request.password().isBlank()
                ? "123456" : request.password();
        passwordService.assertStrength(password);

        SysUser user = new SysUser();
        user.setAccount(account);
        user.setPasswordHash(passwordService.encode(password));
        user.setDisplayName(request.name());
        user.setRole(Role.MENTOR);
        user.setCollegeId(lab.getCollegeId());
        user.setEnabled(request.enabled() == null || request.enabled());
        userMapper.insert(user);

        Mentor mentor = new Mentor();
        mentor.setUserId(user.getId());
        mentor.setMentorNo(request.mentorNo() == null || request.mentorNo().isBlank()
                ? account.toUpperCase() : request.mentorNo());
        mentor.setName(request.name());
        mentor.setTitle(request.title());
        mentor.setLabId(request.labId());
        mentor.setResearchDirection(request.researchDirection());
        mentor.setProfile(request.profile());
        mentor.setQuota(request.quota());
        mentor.setEnabled(request.enabled() == null || request.enabled());
        try {
            mentorMapper.insert(mentor);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "导师工号已存在");
        }
        auditService.log(stageGuard.current().getId(), "MENTOR_CREATE", "MENTOR", mentor.getId(),
                "新增导师：" + mentor.getName(), null);
        return mentorMapper.findById(mentor.getId());
    }

    @Transactional
    public Mentor updateMentor(Long id, MentorSaveRequest request) {
        Mentor existing = mentorMapper.findById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "导师不存在");
        }
        Lab lab = labMapper.findById(request.labId());
        if (lab == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "实训室不存在");
        }
        assertSameCollege(existing.getCollegeId());
        assertSameCollege(lab.getCollegeId());
        assertMentorQuota(lab, request.quota(), id);
        existing.setName(request.name());
        existing.setTitle(request.title());
        existing.setLabId(request.labId());
        existing.setResearchDirection(request.researchDirection());
        existing.setProfile(request.profile());
        existing.setQuota(request.quota());
        existing.setEnabled(request.enabled() == null || request.enabled());
        mentorMapper.update(existing);
        SysUser user = userMapper.findById(existing.getUserId());
        if (user != null && !Objects.equals(user.getDisplayName(), request.name())) {
            user.setDisplayName(request.name());
            userMapper.updateDisplayName(user.getId(), request.name());
        }
        if (user != null) {
            userMapper.updateEnabled(user.getId(), existing.getEnabled());
        }
        auditService.log(stageGuard.current().getId(), "MENTOR_UPDATE", "MENTOR", id,
                "修改导师：" + request.name(), null);
        return mentorMapper.findById(id);
    }

    @Transactional
    public void toggleMentor(Long id, boolean enabled) {
        Mentor mentor = mentorMapper.findById(id);
        if (mentor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "导师不存在");
        }
        assertSameCollege(mentor.getCollegeId());
        mentor.setEnabled(enabled);
        mentorMapper.update(mentor);
        userMapper.updateEnabled(mentor.getUserId(), enabled);
        auditService.log(stageGuard.current().getId(), "MENTOR_STATUS", "MENTOR", id,
                enabled ? "启用导师账号" : "停用导师账号", null);
    }

    @Transactional
    public void resetMentorPassword(Long id, String password) {
        Mentor mentor = mentorMapper.findById(id);
        if (mentor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "导师不存在");
        }
        assertSameCollege(mentor.getCollegeId());
        passwordService.assertStrength(password);
        userMapper.updatePassword(mentor.getUserId(), passwordService.encode(password));
        auditService.log(stageGuard.current().getId(), "PASSWORD_RESET", "MENTOR", id,
                "重置导师登录密码", null);
    }

    public PageResult<Student> students(String keyword, String grade, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        List<Student> records = studentMapper.search(keyword, grade, null, currentCollegeId(),
                (safePage - 1) * safeSize, safeSize);
        long total = studentMapper.countSearch(keyword, grade, null, currentCollegeId());
        return PageResult.of(records, total, safePage, safeSize);
    }

    @Transactional
    public Student createStudent(StudentSaveRequest request) {
        String account = request.account() == null || request.account().isBlank()
                ? request.studentNo() : request.account().trim();
        if (userMapper.findByAccount(account) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "账号已存在");
        }
        if (studentMapper.findByStudentNo(request.studentNo()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "学号已存在");
        }
        String password = request.password() == null || request.password().isBlank()
                ? "123456" : request.password();
        passwordService.assertStrength(password);

        SysUser user = new SysUser();
        user.setAccount(account);
        user.setPasswordHash(passwordService.encode(password));
        user.setDisplayName(request.name());
        user.setRole(Role.STUDENT);
        user.setCollegeId(currentCollegeId());
        user.setEnabled(true);
        userMapper.insert(user);

        Student student = new Student();
        student.setUserId(user.getId());
        student.setCollegeId(currentCollegeId());
        student.setStudentNo(request.studentNo());
        student.setName(request.name());
        student.setGender(request.gender());
        student.setMajor(request.major());
        student.setClassName(request.className());
        student.setGrade(request.grade());
        student.setPhone(request.phone());
        student.setEmail(request.email());
        try {
            studentMapper.insert(student);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "学号已存在");
        }
        auditService.log(stageGuard.current().getId(), "STUDENT_CREATE", "STUDENT", student.getId(),
                "新增学生：" + student.getName() + "（" + student.getStudentNo() + "）", null);
        return studentMapper.findById(student.getId());
    }

    @Transactional
    public Student updateStudent(Long id, StudentSaveRequest request) {
        Student student = studentMapper.findById(id);
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学生不存在");
        }
        assertSameCollege(student.getCollegeId());
        student.setName(request.name());
        student.setGender(request.gender());
        student.setMajor(request.major());
        student.setClassName(request.className());
        student.setGrade(request.grade());
        student.setPhone(request.phone());
        student.setEmail(request.email());
        studentMapper.update(student);
        SysUser user = userMapper.findById(student.getUserId());
        if (user != null) {
            userMapper.updateDisplayName(user.getId(), request.name());
        }
        auditService.log(stageGuard.current().getId(), "STUDENT_UPDATE", "STUDENT", id,
                "修改学生：" + request.name(), null);
        return studentMapper.findById(id);
    }

    @Transactional
    public void resetStudentPassword(Long id, String password) {
        Student student = studentMapper.findById(id);
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学生不存在");
        }
        assertSameCollege(student.getCollegeId());
        passwordService.assertStrength(password);
        userMapper.updatePassword(student.getUserId(), passwordService.encode(password));
        auditService.log(stageGuard.current().getId(), "PASSWORD_RESET", "STUDENT", id,
                "重置学生登录密码", null);
    }

    @Transactional
    public void toggleStudent(Long id, boolean enabled) {
        Student student = studentMapper.findById(id);
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学生不存在");
        }
        assertSameCollege(student.getCollegeId());
        userMapper.updateEnabled(student.getUserId(), enabled);
        auditService.log(stageGuard.current().getId(), "STUDENT_STATUS", "STUDENT", id,
                enabled ? "启用学生账号" : "停用学生账号", null);
    }

    public List<Announcement> announcements(Long batchId) {
        return batchId == null ? announcementMapper.findAll() : announcementMapper.findByBatch(batchId);
    }

    @Transactional
    public Announcement saveAnnouncement(Announcement request, Long id) {
        SelectionBatch batch = request.getBatchId() == null
                ? stageGuard.current() : stageGuard.require(request.getBatchId());
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告标题不能为空");
        }
        if (id == null) {
            request.setBatchId(batch.getId());
            request.setStatus(request.getStatus() == null ? AnnouncementStatus.DRAFT : request.getStatus());
            request.setCreatedBy(com.labselection.auth.AuthContext.userId());
            if (request.getStatus() == AnnouncementStatus.PUBLISHED && request.getPublishStart() == null) {
                request.setPublishStart(LocalDateTime.now());
            }
            announcementMapper.insert(request);
            auditService.log(batch.getId(), "ANNOUNCEMENT_CREATE", "ANNOUNCEMENT", request.getId(),
                    "新建公告：" + request.getTitle(), null);
            return announcementMapper.findById(request.getId());
        }
        Announcement existing = announcementMapper.findById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        request.setId(id);
        request.setCreatedBy(existing.getCreatedBy());
        announcementMapper.update(request);
        auditService.log(batch.getId(), "ANNOUNCEMENT_UPDATE", "ANNOUNCEMENT", id,
                "修改公告：" + request.getTitle(), null);
        return announcementMapper.findById(id);
    }

    @Transactional
    public void publishAnnouncement(Long id) {
        Announcement announcement = announcementMapper.findById(id);
        if (announcement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        if (announcement.getPublishStart() == null) {
            announcement.setPublishStart(LocalDateTime.now());
        }
        if (announcement.getPublishEnd() == null) {
            announcement.setPublishEnd(LocalDateTime.now().plusDays(7));
        }
        announcementMapper.update(announcement);
        auditService.log(announcement.getBatchId(), "ANNOUNCEMENT_PUBLISH", "ANNOUNCEMENT", id,
                "发布公告：" + announcement.getTitle(), null);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementMapper.findById(id);
        if (announcement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        announcementMapper.deleteById(id);
        auditService.log(announcement.getBatchId(), "ANNOUNCEMENT_DELETE", "ANNOUNCEMENT", id,
                "删除公告：" + announcement.getTitle(), null);
    }

    private void assertMentorQuota(Lab lab, int quota, Long excludeMentorId) {
        int sum = mentorMapper.findByLabId(lab.getId()).stream()
                .filter(m -> Boolean.TRUE.equals(m.getEnabled()))
                .filter(m -> !Objects.equals(m.getId(), excludeMentorId))
                .mapToInt(Mentor::getQuota).sum();
        if (sum + quota > lab.getTotalQuota()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "导师名额之和不能超过实训室总名额（剩余可分配 "
                            + Math.max(0, lab.getTotalQuota() - sum) + " 人）");
        }
    }

    private String nextMentorNo() {
        long count = mentorMapper.count() + 1001;
        String account = "t" + count;
        while (userMapper.findByAccount(account) != null) {
            count++;
            account = "t" + count;
        }
        return account;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private Long currentCollegeId() {
        CurrentUser current = AuthContext.get();
        if (current.collegeId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未绑定学院");
        }
        return current.collegeId();
    }

    private void assertSameCollege(Long collegeId) {
        if (collegeId == null || !collegeId.equals(currentCollegeId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能管理本学院的实训室与人员");
        }
    }
}
