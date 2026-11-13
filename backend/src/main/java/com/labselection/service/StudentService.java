package com.labselection.service;

import com.labselection.auth.AuthContext;
import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.domain.entity.AdjustmentRecord;
import com.labselection.domain.entity.Application;
import com.labselection.domain.entity.ApplicationIntent;
import com.labselection.domain.entity.FinalAssignment;
import com.labselection.domain.entity.Interview;
import com.labselection.domain.entity.Lab;
import com.labselection.domain.entity.Mentor;
import com.labselection.domain.entity.Offer;
import com.labselection.domain.entity.Publicity;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.entity.Student;
import com.labselection.domain.entity.SupplementIntent;
import com.labselection.domain.entity.SupplementRound;
import com.labselection.domain.enums.AdjustmentPreference;
import com.labselection.domain.enums.ApplicationStatus;
import com.labselection.domain.enums.AssignmentStatus;
import com.labselection.domain.enums.BatchStatus;
import com.labselection.domain.enums.LabStatus;
import com.labselection.domain.enums.OfferStatus;
import com.labselection.domain.enums.PublicityStatus;
import com.labselection.domain.enums.SupplementStatus;
import com.labselection.dto.ApplicationSaveRequest;
import com.labselection.dto.SupplementApplyRequest;
import com.labselection.mapper.AdjustmentMapper;
import com.labselection.mapper.ApplicationMapper;
import com.labselection.mapper.AssignmentMapper;
import com.labselection.mapper.IntentMapper;
import com.labselection.mapper.InterviewMapper;
import com.labselection.mapper.LabMapper;
import com.labselection.mapper.MentorMapper;
import com.labselection.mapper.OfferMapper;
import com.labselection.mapper.PublicityMapper;
import com.labselection.mapper.StudentMapper;
import com.labselection.mapper.SupplementMapper;
import com.labselection.util.CsvUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StageGuard stageGuard;
    private final PublicService publicService;
    private final StudentMapper studentMapper;
    private final ApplicationMapper applicationMapper;
    private final IntentMapper intentMapper;
    private final InterviewMapper interviewMapper;
    private final OfferMapper offerMapper;
    private final AssignmentMapper assignmentMapper;
    private final SupplementMapper supplementMapper;
    private final AdjustmentMapper adjustmentMapper;
    private final PublicityMapper publicityMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final SelectionService selectionService;
    private final AuditService auditService;

    public Student currentStudent() {
        Student student = studentMapper.findByUserId(AuthContext.userId());
        if (student == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未关联学生信息");
        }
        return student;
    }

    public Map<String, Object> dashboard() {
        Student student = currentStudent();
        SelectionBatch batch = stageGuard.current();
        Application application = applicationMapper.findByBatchAndStudent(batch.getId(), student.getId());
        if (application != null) {
            application.setIntents(intentMapper.findByApplicationId(application.getId()));
            application.setExcludedLabIdList(CsvUtils.parseIds(application.getExcludedLabIds()));
        }
        List<Offer> offers = offerMapper.findByStudent(batch.getId(), student.getId());
        FinalAssignment assignment = assignmentMapper.findByStudentAndBatch(batch.getId(), student.getId());
        List<Interview> interviews = application == null
                ? List.of() : interviewMapper.findByApplication(application.getId());
        SupplementRound openRound = supplementMapper.findOpenRound(batch.getId());
        List<SupplementIntent> supplementIntents = openRound == null
                ? List.of() : supplementMapper.findIntentsByStudent(openRound.getId(), student.getId());
        Publicity publicity = publicityMapper.findPublishedOrClosed(batch.getId());
        List<AdjustmentRecord> adjustments = adjustmentMapper.findByStudent(batch.getId(), student.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("stages", publicService.stages(batch));
        result.put("student", student);
        result.put("application", application);
        result.put("interviews", interviews);
        result.put("offers", offers);
        result.put("assignment", assignment);
        result.put("publicity", publicity);
        result.put("adjustments", adjustments);
        result.put("openSupplementRound", openRound);
        result.put("supplementIntents", supplementIntents);
        result.put("eligible", isEligible(batch, student));
        result.put("intentOpen", batch.getStatus() == BatchStatus.INTENT_OPEN);
        return result;
    }

    public Application application() {
        Student student = currentStudent();
        SelectionBatch batch = stageGuard.current();
        Application application = applicationMapper.findByBatchAndStudent(batch.getId(), student.getId());
        if (application != null) {
            application.setIntents(intentMapper.findByApplicationId(application.getId()));
            application.setExcludedLabIdList(CsvUtils.parseIds(application.getExcludedLabIds()));
        }
        return application;
    }

    @Transactional
    public Application saveApplication(ApplicationSaveRequest request, boolean submit) {
        Student student = currentStudent();
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertIntentWindow(batch);
        if (!isEligible(batch, student)) {
            throw new BusinessException(ErrorCode.NOT_ELIGIBLE);
        }

        List<Long> mentorIds = request.mentorIds() == null
                ? List.of() : new ArrayList<>(new LinkedHashSet<>(request.mentorIds()));
        if (mentorIds.size() > 3) {
            throw new BusinessException(ErrorCode.INTENT_LIMIT);
        }
        if (submit && mentorIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请至少选择 1 个导师意向");
        }
        List<Long> excludedLabIds = request.excludedLabIds() == null
                ? List.of() : new ArrayList<>(new LinkedHashSet<>(request.excludedLabIds()));

        List<Mentor> mentors = new ArrayList<>();
        Set<Long> selectedLabs = new LinkedHashSet<>();
        for (Long mentorId : mentorIds) {
            Mentor mentor = mentorMapper.findById(mentorId);
            if (mentor == null || !Boolean.TRUE.equals(mentor.getEnabled())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "所选导师不存在或已停用");
            }
            Lab lab = labMapper.findById(mentor.getLabId());
            if (lab == null || lab.getStatus() != LabStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "导师「" + mentor.getName() + "」所属实训室已停用");
            }
            if (!Objects.equals(lab.getCollegeId(), student.getCollegeId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "只能选择本学院实训室的导师");
            }
            if (excludedLabIds.contains(mentor.getLabId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "不能选择已明确拒绝调剂的实训室下的导师");
            }
            mentors.add(mentor);
            selectedLabs.add(mentor.getLabId());
        }

        Application existing = applicationMapper.findByBatchAndStudent(batch.getId(), student.getId());
        if (existing != null && existing.getStatus() == ApplicationStatus.LOCKED) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "报名已锁定，不能再修改");
        }
        if (existing != null && existing.getStatus() == ApplicationStatus.SUBMITTED && !submit) {
            // 已提交的报名再次保存时保持“已提交”，避免误降级为草稿
            submit = true;
        }

        Application application = existing == null ? new Application() : existing;
        application.setBatchId(batch.getId());
        application.setStudentId(student.getId());
        application.setIntro(trim(request.intro()));
        application.setSkills(trim(request.skills()));
        application.setProjectExperience(trim(request.projectExperience()));
        application.setAdjustmentPreference(request.adjustmentPreference() == null
                ? AdjustmentPreference.NONE : request.adjustmentPreference());
        application.setExcludedLabIds(CsvUtils.joinIds(excludedLabIds));
        application.setStatus(submit ? ApplicationStatus.SUBMITTED : ApplicationStatus.DRAFT);
        application.setSubmittedAt(submit ? LocalDateTime.now() : null);

        if (existing == null) {
            applicationMapper.insert(application);
        } else {
            if (request.version() != null && !Objects.equals(request.version(), existing.getVersion())) {
                throw new BusinessException(ErrorCode.CONFLICT);
            }
            application.setVersion(existing.getVersion());
            int updated = applicationMapper.update(application);
            if (updated == 0) {
                throw new BusinessException(ErrorCode.CONFLICT);
            }
        }

        intentMapper.deleteByApplicationId(application.getId());
        int sortNo = 1;
        for (Mentor mentor : mentors) {
            ApplicationIntent intent = new ApplicationIntent();
            intent.setApplicationId(application.getId());
            intent.setBatchId(batch.getId());
            intent.setStudentId(student.getId());
            intent.setMentorId(mentor.getId());
            intent.setSortNo(sortNo++);
            intent.setStatus(com.labselection.domain.enums.IntentStatus.PENDING);
            intentMapper.insert(intent);
        }

        auditService.log(batch.getId(), submit ? "APPLICATION_SUBMIT" : "APPLICATION_SAVE",
                "APPLICATION", application.getId(),
                (submit ? "提交" : "保存") + "报名，意向数 " + mentors.size()
                        + "，调剂意愿 " + application.getAdjustmentPreference().getLabel(),
                null);
        Application detail = applicationMapper.findDetailById(application.getId());
        detail.setIntents(intentMapper.findByApplicationId(application.getId()));
        detail.setExcludedLabIdList(CsvUtils.parseIds(detail.getExcludedLabIds()));
        return detail;
    }

    @Transactional
    public void withdraw() {
        Student student = currentStudent();
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertIntentWindow(batch);
        Application application = applicationMapper.findByBatchAndStudent(batch.getId(), student.getId());
        if (application == null || application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "暂无可撤回的报名");
        }
        if (application.getStatus() == ApplicationStatus.LOCKED) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "报名已锁定，不能撤回");
        }
        applicationMapper.updateStatus(application.getId(), ApplicationStatus.WITHDRAWN.name(), null);
        intentMapper.updateStatusByApplication(application.getId(),
                com.labselection.domain.enums.IntentStatus.RELEASED.name());
        auditService.log(batch.getId(), "APPLICATION_WITHDRAW", "APPLICATION", application.getId(),
                "学生撤回报名", null);
    }

    public List<Interview> interviews() {
        Application application = application();
        return application == null ? List.of() : interviewMapper.findByApplication(application.getId());
    }

    public List<Offer> offers() {
        Student student = currentStudent();
        SelectionBatch batch = stageGuard.current();
        return offerMapper.findByStudent(batch.getId(), student.getId());
    }

    @Transactional
    public Map<String, Object> confirmOffer(Long offerId, String reason) {
        Student student = currentStudent();
        FinalAssignment assignment = selectionService.confirmOffer(offerId, student.getId(), reason);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assignment", assignment);
        result.put("offers", offerMapper.findByStudent(assignment.getBatchId(), student.getId()));
        return result;
    }

    @Transactional
    public void declineOffer(Long offerId, String reason) {
        Student student = currentStudent();
        selectionService.declineOffer(offerId, student.getId(), reason);
    }

    @Transactional
    public int declineAll(String reason) {
        Student student = currentStudent();
        return selectionService.declineAll(student.getId(), reason);
    }

    public Map<String, Object> result() {
        Student student = currentStudent();
        SelectionBatch batch = stageGuard.current();
        FinalAssignment assignment = assignmentMapper.findByStudentAndBatch(batch.getId(), student.getId());
        Publicity publicity = publicityMapper.findPublishedOrClosed(batch.getId());
        List<AdjustmentRecord> adjustments = adjustmentMapper.findByStudent(batch.getId(), student.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("assignment", assignment);
        result.put("publicity", publicity);
        result.put("adjustments", adjustments);
        result.put("locked", assignment != null && assignment.getStatus() == AssignmentStatus.LOCKED);
        return result;
    }

    public List<Map<String, Object>> history() {
        Student student = currentStudent();
        List<Application> applications = applicationMapper.findByStudent(student.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Application application : applications) {
            FinalAssignment assignment = assignmentMapper.findByStudentAndBatch(
                    application.getBatchId(), student.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("application", application);
            row.put("assignment", assignment);
            result.add(row);
        }
        return result;
    }

    public Map<String, Object> supplementState() {
        Student student = currentStudent();
        SelectionBatch batch = stageGuard.current();
        SupplementRound round = supplementMapper.findOpenRound(batch.getId());
        if (round == null) {
            round = supplementMapper.findRoundsByBatch(batch.getId()).stream()
                    .reduce((a, b) -> b).orElse(null);
        }
        List<SupplementIntent> intents = round == null
                ? List.of() : supplementMapper.findIntentsByStudent(round.getId(), student.getId());
        List<Offer> offers = offerMapper.findByStudent(batch.getId(), student.getId()).stream()
                .filter(o -> o.getSource() == com.labselection.domain.enums.OfferSource.SUPPLEMENT
                        || o.getSource() == com.labselection.domain.enums.OfferSource.PROMOTION)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("round", round);
        result.put("intents", intents);
        result.put("offers", offers);
        result.put("finalAssignment", assignmentMapper.findByStudentAndBatch(batch.getId(), student.getId()));
        return result;
    }

    @Transactional
    public Map<String, Object> applySupplement(SupplementApplyRequest request) {
        Student student = currentStudent();
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.SUPPLEMENT);
        stageGuard.assertNotLocked(batch.getId());
        if (assignmentMapper.findByStudentAndBatch(batch.getId(), student.getId()) != null) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "你已完成录取，无需参加补录");
        }
        SupplementRound round = supplementMapper.findRoundById(request.roundId());
        if (round == null || round.getStatus() != SupplementStatus.OPEN
                || !Objects.equals(round.getBatchId(), batch.getId())) {
            throw new BusinessException(ErrorCode.SUPPLEMENT_CLOSED);
        }
        stageGuard.assertWindow(round.getStartAt(), round.getEndAt(), "补录填报");

        List<Long> mentorIds = new ArrayList<>(new LinkedHashSet<>(request.mentorIds()));
        if (mentorIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请至少选择 1 个补录意向");
        }
        if (mentorIds.size() > 3) {
            throw new BusinessException(ErrorCode.INTENT_LIMIT);
        }
        for (Long mentorId : mentorIds) {
            Mentor mentor = mentorMapper.findById(mentorId);
            if (mentor == null || !Boolean.TRUE.equals(mentor.getEnabled())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "所选导师不存在或已停用");
            }
            Lab lab = labMapper.findById(mentor.getLabId());
            if (lab == null || lab.getStatus() != LabStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "导师「" + mentor.getName() + "」所属实训室已停用");
            }
            if (!Objects.equals(lab.getCollegeId(), student.getCollegeId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "只能选择本学院实训室的导师");
            }
        }

        supplementMapper.deleteIntentsByRoundAndStudent(round.getId(), student.getId());
        int sortNo = 1;
        for (Long mentorId : mentorIds) {
            SupplementIntent intent = new SupplementIntent();
            intent.setRoundId(round.getId());
            intent.setBatchId(batch.getId());
            intent.setStudentId(student.getId());
            intent.setMentorId(mentorId);
            intent.setSortNo(sortNo++);
            intent.setStatus(com.labselection.domain.enums.SupplementIntentStatus.PENDING);
            supplementMapper.insertIntent(intent);
        }
        auditService.log(batch.getId(), "SUPPLEMENT_APPLY", "SUPPLEMENT_ROUND", round.getId(),
                "学生填报补录意向 " + mentorIds.size() + " 个", null);
        return supplementState();
    }

    public boolean isEligible(SelectionBatch batch, Student student) {
        if (batch.getGradeScope() != null && !batch.getGradeScope().isBlank()) {
            boolean matched = false;
            for (String grade : batch.getGradeScope().split(",")) {
                if (grade.trim().equals(student.getGrade())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        if (batch.getMajorScope() != null && !batch.getMajorScope().isBlank()) {
            boolean matched = false;
            for (String major : batch.getMajorScope().split(",")) {
                if (major.trim().equals(student.getMajor())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
