package com.labselection.service;

import com.labselection.auth.AuthContext;
import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.common.PageResult;
import com.labselection.domain.entity.Application;
import com.labselection.domain.entity.ApplicationIntent;
import com.labselection.domain.entity.AuditLog;
import com.labselection.domain.entity.FinalAssignment;
import com.labselection.domain.entity.Interview;
import com.labselection.domain.entity.Lab;
import com.labselection.domain.entity.Mentor;
import com.labselection.domain.entity.Offer;
import com.labselection.domain.entity.Publicity;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.enums.BatchStatus;
import com.labselection.domain.enums.OfferStatus;
import com.labselection.domain.enums.PublicityStatus;
import com.labselection.dto.StageRequest;
import com.labselection.mapper.ApplicationMapper;
import com.labselection.mapper.AssignmentMapper;
import com.labselection.mapper.AuditMapper;
import com.labselection.mapper.BatchMapper;
import com.labselection.mapper.IntentMapper;
import com.labselection.mapper.InterviewMapper;
import com.labselection.mapper.LabMapper;
import com.labselection.mapper.MentorMapper;
import com.labselection.mapper.OfferMapper;
import com.labselection.mapper.PublicityMapper;
import com.labselection.mapper.StudentMapper;
import com.labselection.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final StageGuard stageGuard;
    private final PublicService publicService;
    private final PublicityService publicityService;
    private final SupplementService supplementService;
    private final SelectionService selectionService;
    private final BatchMapper batchMapper;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final ApplicationMapper applicationMapper;
    private final IntentMapper intentMapper;
    private final InterviewMapper interviewMapper;
    private final OfferMapper offerMapper;
    private final AssignmentMapper assignmentMapper;
    private final PublicityMapper publicityMapper;
    private final AuditMapper auditMapper;
    private final AuditService auditService;

    public Map<String, Object> dashboard() {
        SelectionBatch batch = stageGuard.current();
        Long collegeId = currentCollegeId();
        List<Lab> labs = labMapper.listWithStats(batch.getId(), collegeId);
        List<Mentor> mentors = mentorMapper.listWithStats(batch.getId(), collegeId, null);
        Set<Long> labIds = labs.stream().map(Lab::getId).collect(java.util.stream.Collectors.toSet());
        List<Offer> allOffers = offerMapper.findByBatch(batch.getId(), null).stream()
                .filter(offer -> labIds.contains(offer.getLabId())).toList();
        List<FinalAssignment> allAssignments = assignmentMapper.findByBatch(batch.getId()).stream()
                .filter(item -> labIds.contains(item.getLabId())).toList();
        List<ApplicationIntent> intents = intentMapper.findByBatch(batch.getId()).stream()
                .filter(intent -> labIds.contains(intent.getLabId())).toList();
        long applications = applicationMapper.countSearch(batch.getId(), "SUBMITTED", null, collegeId)
                + applicationMapper.countSearch(batch.getId(), "LOCKED", null, collegeId);
        long offers = allOffers.stream().filter(o -> o.getStatus() == OfferStatus.OFFERED).count();
        long admitted = allAssignments.size();
        long pending = intents.stream().filter(i -> i.getStatus() == com.labselection.domain.enums.IntentStatus.PENDING).count();
        long waitlist = intents.stream().filter(i -> i.getStatus() == com.labselection.domain.enums.IntentStatus.WAITLISTED).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("stages", publicService.stages(batch));
        result.put("batches", batches());
        result.put("college", currentCollegeName());
        result.put("stats", Map.of(
                "studentCount", studentMapper.countByCollege(collegeId),
                "mentorCount", mentors.size(),
                "labCount", labs.size(),
                "applicationCount", applications,
                "pendingOffers", offers,
                "admittedCount", admitted,
                "pendingDecisions", pending,
                "waitlistCount", waitlist
        ));
        result.put("labs", labs);
        result.put("mentors", mentors);
        result.put("review", review(batch.getId()));
        result.put("recentAudits", auditMapper.search(null, null, 0, 40).stream()
                .filter(log -> !"LOGIN".equals(log.getAction()))
                .limit(8)
                .toList());
        result.put("publicity", publicityMapper.findCurrentByBatch(batch.getId()));
        result.put("supplementRounds", supplementService.rounds(batch.getId()));
        return result;
    }

    public List<SelectionBatch> batches() {
        Long collegeId = currentCollegeId();
        List<SelectionBatch> batches = batchMapper.findAll();
        for (SelectionBatch batch : batches) {
            List<Lab> labs = labMapper.listWithStats(batch.getId(), collegeId);
            Set<Long> labIds = labs.stream().map(Lab::getId).collect(java.util.stream.Collectors.toSet());
            batch.setApplicationCount((int) (applicationMapper.countSearch(batch.getId(), "SUBMITTED", null, collegeId)
                    + applicationMapper.countSearch(batch.getId(), "LOCKED", null, collegeId)));
            batch.setOfferCount((int) offerMapper.findByBatch(batch.getId(), null).stream()
                    .filter(offer -> labIds.contains(offer.getLabId())).count());
            batch.setAdmittedCount((int) assignmentMapper.findByBatch(batch.getId()).stream()
                    .filter(item -> labIds.contains(item.getLabId())).count());
            batch.setLabCount(labs.size());
            batch.setMentorCount((int) mentorMapper.listWithStats(batch.getId(), collegeId, null).size());
        }
        return batches;
    }

    public Map<String, Object> batchDetail(Long id) {
        SelectionBatch batch = stageGuard.require(id);
        Long collegeId = currentCollegeId();
        List<Lab> labs = labMapper.listWithStats(id, collegeId);
        List<Mentor> mentors = mentorMapper.listWithStats(id, collegeId, null);
        Set<Long> labIds = labs.stream().map(Lab::getId).collect(java.util.stream.Collectors.toSet());
        batch.setApplicationCount((int) (applicationMapper.countSearch(id, "SUBMITTED", null, collegeId)
                + applicationMapper.countSearch(id, "LOCKED", null, collegeId)));
        batch.setOfferCount((int) offerMapper.findByBatch(id, null).stream()
                .filter(offer -> labIds.contains(offer.getLabId())).count());
        batch.setAdmittedCount((int) assignmentMapper.findByBatch(id).stream()
                .filter(item -> labIds.contains(item.getLabId())).count());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("stages", publicService.stages(batch));
        result.put("labs", labs);
        result.put("mentors", mentors);
        result.put("rounds", supplementService.rounds(id));
        result.put("publicities", publicityMapper.findByBatch(id));
        result.put("applications", applicationMapper.findByBatchWithStudent(id, collegeId));
        result.put("offers", offerMapper.findByBatch(id, null).stream()
                .filter(offer -> labIds.contains(offer.getLabId())).toList());
        result.put("assignments", assignmentMapper.findByBatch(id).stream()
                .filter(item -> labIds.contains(item.getLabId())).toList());
        result.put("review", review(id));
        return result;
    }

    @Transactional
    public SelectionBatch createBatch(SelectionBatch request) {
        validateBatch(request);
        request.setName(request.getName().trim());
        request.setStatus(request.getStatus() == null ? BatchStatus.DRAFT : request.getStatus());
        request.setCreatedBy(AuthContext.userId());
        batchMapper.insert(request);
        auditService.log(request.getId(), "BATCH_CREATE", "BATCH", request.getId(),
                "创建选拔批次：" + request.getName(), null);
        return batchMapper.findById(request.getId());
    }

    @Transactional
    public SelectionBatch updateBatch(Long id, SelectionBatch request) {
        SelectionBatch existing = stageGuard.require(id);
        validateBatch(request);
        request.setId(id);
        request.setStatus(existing.getStatus());
        request.setCreatedBy(existing.getCreatedBy());
        batchMapper.update(request);
        auditService.log(id, "BATCH_UPDATE", "BATCH", id,
                "修改选拔批次：" + request.getName(), null);
        return batchMapper.findById(id);
    }

    @Transactional
    public SelectionBatch changeStage(Long id, StageRequest request) {
        SelectionBatch batch = stageGuard.require(id);
        BatchStatus target;
        try {
            target = BatchStatus.valueOf(request.status().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的选拔阶段");
        }
        if (target == batch.getStatus()) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "当前已处于该阶段");
        }
        Publicity publicity = publicityMapper.findCurrentByBatch(id);
        if (publicity != null
                && (publicity.getStatus() == PublicityStatus.PUBLISHED
                || publicity.getStatus() == PublicityStatus.CLOSED)
                && target != BatchStatus.PUBLICITY && target != BatchStatus.FINISHED) {
            throw new BusinessException(ErrorCode.BATCH_LOCKED,
                    "公示已发布或结果已锁定，请先撤回公示再调整阶段");
        }

        batchMapper.updateStatus(id, target.name());
        SelectionBatch updated = batchMapper.findById(id);
        String extra = "";
        if (target == BatchStatus.CONFIRM) {
            int auto = selectionService.autoConfirmSingleOffers(id);
            extra = "，自动确认唯一拟录取 " + auto + " 人";
        } else if (target == BatchStatus.SUPPLEMENT) {
            supplementService.ensureFirstRound(id);
            extra = "，已准备第一轮补录";
        } else if (target == BatchStatus.PUBLICITY) {
            if (publicityMapper.findCurrentByBatch(id) == null) {
                try {
                    Publicity draft = new Publicity();
                    draft.setBatchId(id);
                    draft.setTitle(updated.getName() + "拟录取名单公示");
                    draft.setContent("经面试考核、拟录取确认、补录与统筹调剂，现将拟录取名单予以公示。");
                    stageGuard.assertNotLocked(id);
                    publicityService.saveDraft(draft);
                    extra = "，已生成公示草稿";
                } catch (BusinessException ignored) {
                    // 暂无录取结果时允许先进入公示阶段，稍后手动生成
                }
            }
        } else if (target == BatchStatus.FINISHED) {
            if (publicity != null && publicity.getStatus() == PublicityStatus.PUBLISHED) {
                publicityService.close(publicity.getId());
                extra = "，公示关闭并锁定结果";
            }
        }
        auditService.log(id, "BATCH_STAGE_CHANGE", "BATCH", id,
                "阶段由「" + batch.getStatus().getLabel() + "」变更为「" + target.getLabel() + "」" + extra,
                request.reason());
        return batchMapper.findById(id);
    }

    public Map<String, Object> review(Long batchId) {
        SelectionBatch batch = stageGuard.require(batchId);
        Long collegeId = currentCollegeId();
        List<Map<String, Object>> issues = new ArrayList<>();
        List<Lab> labs = labMapper.listWithStats(batchId, collegeId);
        List<Mentor> mentors = mentorMapper.listWithStats(batchId, collegeId, null);
        Set<Long> labIds = labs.stream().map(Lab::getId).collect(java.util.stream.Collectors.toSet());
        for (Lab lab : labs) {
            long used = Math.max(offerMapper.countActiveByLab(lab.getId(), batchId),
                    assignmentMapper.countByLab(lab.getId(), batchId));
            if (used > lab.getTotalQuota()) {
                issues.add(issue("ERROR", "LAB_OVER_QUOTA",
                        "实训室「" + lab.getName() + "」已用名额 " + used + " 超过总名额 " + lab.getTotalQuota(),
                        "LAB", lab.getId()));
            }
        }
        for (Mentor mentor : mentors) {
            long used = Math.max(offerMapper.countActiveByMentor(mentor.getId(), batchId),
                    assignmentMapper.countByMentor(mentor.getId(), batchId));
            if (used > mentor.getQuota()) {
                issues.add(issue("ERROR", "MENTOR_OVER_QUOTA",
                        "导师「" + mentor.getName() + "」已用名额 " + used + " 超过其名额 " + mentor.getQuota(),
                        "MENTOR", mentor.getId()));
            }
            long pending = intentMapper.countPendingByMentor(mentor.getId(), batchId);
            if (pending > 0) {
                issues.add(issue("INFO", "PENDING_DECISION",
                        "导师「" + mentor.getName() + "」还有 " + pending + " 条意向未给出初选结论",
                        "MENTOR", mentor.getId()));
            }
        }

        List<Offer> offers = offerMapper.findByBatch(batchId, null).stream()
                .filter(offer -> labIds.contains(offer.getLabId())).toList();
        int offerWithoutInterview = 0;
        for (Offer offer : offers) {
            if (offer.getStatus() != OfferStatus.OFFERED && offer.getStatus() != OfferStatus.CONFIRMED) {
                continue;
            }
            if (offer.getApplicationId() != null
                    && interviewMapper.existsCompleted(offer.getApplicationId(), offer.getLabId()) == 0) {
                offerWithoutInterview++;
                issues.add(issue("ERROR", "OFFER_WITHOUT_INTERVIEW",
                        "学生「" + offer.getStudentName() + "」的拟录取缺少已完成面试记录",
                        "OFFER", offer.getId()));
            }
        }
        long pendingOffers = offers.stream().filter(o -> o.getStatus() == OfferStatus.OFFERED).count();
        if (pendingOffers > 0) {
            issues.add(issue("WARN", "OFFER_PENDING_CONFIRM",
                    "仍有 " + pendingOffers + " 条拟录取意向待学生确认", "BATCH", batchId));
        }
        long multiOfferStudents = offers.stream()
                .filter(o -> o.getStatus() == OfferStatus.OFFERED)
                .collect(java.util.stream.Collectors.groupingBy(Offer::getStudentId, java.util.stream.Collectors.counting()))
                .values().stream().filter(c -> c > 1).count();
        if (multiOfferStudents > 0) {
            issues.add(issue("INFO", "MULTI_OFFER",
                    multiOfferStudents + " 名学生收到多个拟录取意向，等待其自主确认", "BATCH", batchId));
        }
        long admitted = assignmentMapper.findByBatch(batchId).stream()
                .filter(item -> labIds.contains(item.getLabId())).count();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("batch", batch);
        summary.put("issueCount", issues.size());
        summary.put("errorCount", issues.stream().filter(i -> "ERROR".equals(i.get("level"))).count());
        summary.put("pendingOffers", pendingOffers);
        summary.put("offerWithoutInterview", offerWithoutInterview);
        summary.put("admittedCount", admitted);
        summary.put("issues", issues);
        return summary;
    }

    @Transactional
    public int autoConfirm(Long batchId) {
        return selectionService.autoConfirmSingleOffers(batchId);
    }

    @Transactional
    public int expireOffers(Long batchId, String reason) {
        return selectionService.expirePendingOffers(batchId,
                reason == null || reason.isBlank() ? "确认期限届满，按放弃处理" : reason);
    }

    public PageResult<AuditLog> auditLogs(Long batchId, String action, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        List<AuditLog> records = auditMapper.search(batchId, action,
                (safePage - 1) * safeSize, safeSize);
        long total = auditMapper.countSearch(batchId, action);
        return PageResult.of(records, total, safePage, safeSize);
    }

    public Map<String, Object> applications(Long batchId, String status, String keyword, int page, int size) {
        SelectionBatch batch = batchId == null ? stageGuard.current() : stageGuard.require(batchId);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        List<Application> records = applicationMapper.search(batch.getId(), status, keyword,
                currentCollegeId(), (safePage - 1) * safeSize, safeSize);
        for (Application application : records) {
            application.setIntents(intentMapper.findByApplicationId(application.getId()));
            application.setExcludedLabIdList(com.labselection.util.CsvUtils.parseIds(application.getExcludedLabIds()));
        }
        long total = applicationMapper.countSearch(batch.getId(), status, keyword, currentCollegeId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("records", records);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    public Map<String, Object> applicationDetail(Long id) {
        Application application = applicationMapper.findDetailById(id);
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报名记录不存在");
        }
        if (!Objects.equals(application.getCollegeId(), currentCollegeId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能查看其他学院的报名");
        }
        application.setIntents(intentMapper.findByApplicationId(id));
        application.setExcludedLabIdList(com.labselection.util.CsvUtils.parseIds(application.getExcludedLabIds()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("application", application);
        result.put("interviews", interviewMapper.findByApplication(id));
        result.put("offers", offerMapper.findByStudent(application.getBatchId(), application.getStudentId()));
        result.put("assignment", assignmentMapper.findByStudentAndBatch(
                application.getBatchId(), application.getStudentId()));
        return result;
    }

    public Map<String, Object> offers(Long batchId) {
        SelectionBatch batch = batchId == null ? stageGuard.current() : stageGuard.require(batchId);
        Long collegeId = currentCollegeId();
        Set<Long> labIds = labMapper.listWithStats(batch.getId(), collegeId).stream()
                .map(Lab::getId).collect(java.util.stream.Collectors.toSet());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("offers", offerMapper.findByBatch(batch.getId(), null).stream()
                .filter(offer -> labIds.contains(offer.getLabId())).toList());
        result.put("assignments", assignmentMapper.findByBatch(batch.getId()).stream()
                .filter(item -> labIds.contains(item.getLabId())).toList());
        result.put("interviews", interviewMapper.findByBatch(batch.getId()).stream()
                .filter(item -> labIds.contains(item.getLabId())).toList());
        return result;
    }

    public Map<String, Object> stateMachine() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statuses", java.util.Arrays.stream(BatchStatus.values())
                .map(s -> Map.of("value", s.name(), "label", s.getLabel()))
                .toList());
        result.put("stages", List.of(
                "DRAFT", "ANNOUNCED", "INTENT_OPEN", "INTENT_CLOSED", "INTERVIEW",
                "CONFIRM", "SUPPLEMENT", "ADJUSTMENT", "PUBLICITY", "FINISHED"));
        return result;
    }

    private Long currentCollegeId() {
        Long collegeId = AuthContext.get().collegeId();
        if (collegeId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未绑定学院");
        }
        return collegeId;
    }

    private String currentCollegeName() {
        return AuthContext.get().collegeName();
    }

    private void validateBatch(SelectionBatch request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "批次名称不能为空");
        }
        validateRange(request.getOpenDayStart(), request.getOpenDayEnd(), "实训室开放日");
        validateRange(request.getIntentStart(), request.getIntentEnd(), "意向填报");
        validateRange(request.getInterviewStart(), request.getInterviewEnd(), "面试考核");
        validateRange(request.getConfirmStart(), request.getConfirmEnd(), "拟录取确认");
        validateRange(request.getSupplementStart(), request.getSupplementEnd(), "分轮补录");
        validateRange(request.getAdjustStart(), request.getAdjustEnd(), "统筹调剂");
        validateRange(request.getPublicityStart(), request.getPublicityEnd(), "结果公示");
    }

    private void validateRange(LocalDateTime start, LocalDateTime end, String label) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "的开始时间不能晚于结束时间");
        }
    }

    private Map<String, Object> issue(String level, String type, String message, String targetType, Long targetId) {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("level", level);
        issue.put("type", type);
        issue.put("message", message);
        issue.put("targetType", targetType);
        issue.put("targetId", targetId);
        return issue;
    }
}
