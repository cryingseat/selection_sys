package com.labselection.service;

import com.labselection.auth.AuthContext;
import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.domain.entity.Application;
import com.labselection.domain.entity.ApplicationIntent;
import com.labselection.domain.entity.FinalAssignment;
import com.labselection.domain.entity.Interview;
import com.labselection.domain.entity.Lab;
import com.labselection.domain.entity.Mentor;
import com.labselection.domain.entity.Offer;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.entity.SupplementIntent;
import com.labselection.domain.entity.SupplementRound;
import com.labselection.domain.enums.BatchStatus;
import com.labselection.domain.enums.IntentStatus;
import com.labselection.domain.enums.InterviewConclusion;
import com.labselection.domain.enums.InterviewStatus;
import com.labselection.domain.enums.OfferStatus;
import com.labselection.domain.enums.SupplementIntentStatus;
import com.labselection.domain.enums.SupplementStatus;
import com.labselection.dto.DecisionRequest;
import com.labselection.dto.InterviewRequest;
import com.labselection.dto.SupplementDecisionRequest;
import com.labselection.mapper.ApplicationMapper;
import com.labselection.mapper.AssignmentMapper;
import com.labselection.mapper.IntentMapper;
import com.labselection.mapper.InterviewMapper;
import com.labselection.mapper.LabMapper;
import com.labselection.mapper.MentorMapper;
import com.labselection.mapper.OfferMapper;
import com.labselection.mapper.SupplementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MentorService {

    private final StageGuard stageGuard;
    private final PublicService publicService;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final ApplicationMapper applicationMapper;
    private final IntentMapper intentMapper;
    private final InterviewMapper interviewMapper;
    private final OfferMapper offerMapper;
    private final AssignmentMapper assignmentMapper;
    private final SupplementMapper supplementMapper;
    private final SelectionService selectionService;
    private final AuditService auditService;

    public Mentor currentMentor() {
        Mentor mentor = mentorMapper.findByUserId(AuthContext.userId());
        if (mentor == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未关联导师信息");
        }
        return mentor;
    }

    public Map<String, Object> dashboard() {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        Lab lab = labMapper.findById(mentor.getLabId());
        long applicants = intentMapper.countByMentorAndBatch(mentor.getId(), batch.getId(), null, null);
        long pending = intentMapper.countPendingByMentor(mentor.getId(), batch.getId());
        long waitlist = intentMapper.countByMentorAndStatus(mentor.getId(), batch.getId(), "WAITLISTED");
        long interviews = interviewMapper.countCompletedByMentor(mentor.getId(), batch.getId());
        long activeOffers = offerMapper.countActiveByMentor(mentor.getId(), batch.getId());
        long confirmed = assignmentMapper.countByMentor(mentor.getId(), batch.getId());
        long used = Math.max(activeOffers, confirmed);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mentor", mentor);
        result.put("lab", lab);
        result.put("batch", batch);
        result.put("stages", publicService.stages(batch));
        result.put("stats", Map.of(
                "applicantCount", applicants,
                "pendingCount", pending,
                "waitlistCount", waitlist,
                "interviewCount", interviews,
                "offerCount", activeOffers,
                "confirmedCount", confirmed,
                "quota", mentor.getQuota(),
                "remainingQuota", Math.max(0, mentor.getQuota() - used)
        ));
        result.put("pendingApplicants", pendingApplicants(mentor, batch));
        return result;
    }

    private List<Map<String, Object>> pendingApplicants(Mentor mentor, SelectionBatch batch) {
        List<ApplicationIntent> intents = intentMapper.findByMentorAndBatch(
                mentor.getId(), batch.getId(), "PENDING", null, 0, 8);
        return intents.stream().map(i -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("intentId", i.getId());
            row.put("studentName", i.getStudentName());
            row.put("studentNo", i.getStudentNo());
            row.put("major", i.getMajor());
            row.put("interview", interviewMapper.findByApplicationAndLab(i.getApplicationId(), i.getLabId()));
            return row;
        }).toList();
    }

    public Map<String, Object> applicants(String status, String keyword, int page, int size) {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        List<ApplicationIntent> intents = intentMapper.findByMentorAndBatch(
                mentor.getId(), batch.getId(), status, keyword, (safePage - 1) * safeSize, safeSize);
        long total = intentMapper.countByMentorAndBatch(mentor.getId(), batch.getId(), status, keyword);

        List<Map<String, Object>> records = new ArrayList<>();
        for (ApplicationIntent intent : intents) {
            records.add(applicantRow(intent, batch));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("batch", batch);
        return result;
    }

    public Map<String, Object> applicantDetail(Long applicationId) {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        List<ApplicationIntent> intents = intentMapper.findByApplicationId(applicationId);
        ApplicationIntent own = intents.stream()
                .filter(i -> Objects.equals(i.getMentorId(), mentor.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "该学生未报名你"));
        Application application = applicationMapper.findById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报名记录不存在");
        }
        if (!Objects.equals(application.getBatchId(), batch.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能查看其他批次的报名");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("intent", own);
        result.put("application", application);
        result.put("interview", interviewMapper.findByApplicationAndLab(applicationId, mentor.getLabId()));
        result.put("otherIntentCount", Math.max(0, intents.size() - 1));
        result.put("offer", offerMapper.findByStudent(batch.getId(), application.getStudentId()).stream()
                .filter(o -> Objects.equals(o.getMentorId(), mentor.getId()))
                .findFirst().orElse(null));
        // 刻意不返回其他平行意向的导师信息，避免影响独立评价
        return result;
    }

    private Map<String, Object> applicantRow(ApplicationIntent intent, SelectionBatch batch) {
        Application application = applicationMapper.findById(intent.getApplicationId());
        List<ApplicationIntent> all = intentMapper.findByApplicationId(intent.getApplicationId());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("intent", intent);
        row.put("application", application);
        row.put("interview", interviewMapper.findByApplicationAndLab(intent.getApplicationId(), intent.getLabId()));
        row.put("offer", offerMapper.findByStudent(batch.getId(), intent.getStudentId()).stream()
                .filter(o -> Objects.equals(o.getIntentId(), intent.getId()))
                .findFirst().orElse(null));
        row.put("otherIntentCount", Math.max(0, all.size() - 1));
        return row;
    }

    public List<Interview> interviews() {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        return interviewMapper.findByMentor(mentor.getId(), batch.getId());
    }

    @Transactional
    public Interview saveInterview(InterviewRequest request) {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.INTERVIEW);
        stageGuard.assertNotLocked(batch.getId());

        Application application = applicationMapper.findById(request.applicationId());
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报名记录不存在");
        }
        List<ApplicationIntent> intents = intentMapper.findByApplicationId(application.getId());
        boolean appliedToLab = intents.stream().anyMatch(i -> Objects.equals(i.getLabId(), mentor.getLabId()));
        if (!appliedToLab) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该学生未报名本实训室");
        }

        InterviewConclusion conclusion = request.conclusion() == null
                ? InterviewConclusion.PENDING : request.conclusion();
        if (conclusion != InterviewConclusion.PENDING && request.score() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "提交面试结论时必须填写评分");
        }
        InterviewStatus status = conclusion == InterviewConclusion.PENDING
                ? InterviewStatus.SCHEDULED : InterviewStatus.COMPLETED;

        Interview interview = interviewMapper.findByApplicationAndLab(application.getId(), mentor.getLabId());
        boolean create = interview == null;
        if (create) {
            interview = new Interview();
            interview.setBatchId(batch.getId());
            interview.setApplicationId(application.getId());
            interview.setLabId(mentor.getLabId());
        }
        interview.setMentorId(mentor.getId());
        interview.setScheduledAt(request.scheduledAt() == null ? LocalDateTime.now() : request.scheduledAt());
        interview.setLocation(request.location());
        interview.setScore(request.score());
        interview.setComment(request.comment());
        interview.setConclusion(conclusion);
        interview.setStatus(status);
        if (status == InterviewStatus.COMPLETED) {
            interview.setRecordedBy(AuthContext.userId());
            interview.setRecordedAt(LocalDateTime.now());
        }
        if (create) {
            interviewMapper.insert(interview);
        } else {
            interviewMapper.update(interview);
        }
        auditService.log(batch.getId(), "INTERVIEW_SAVE", "INTERVIEW", interview.getId(),
                "记录面试考核：" + application.getStudentName() + "，结论 " + conclusion.getLabel()
                        + (request.score() == null ? "" : "，评分 " + request.score()),
                request.comment());
        return interviewMapper.findById(interview.getId());
    }

    public Map<String, Object> decisions() {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        List<ApplicationIntent> intents = intentMapper.findByMentorAndBatch(
                mentor.getId(), batch.getId(), null, null, 0, 500);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ApplicationIntent intent : intents) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("intent", intent);
            row.put("interview", interviewMapper.findByApplicationAndLab(intent.getApplicationId(), intent.getLabId()));
            row.put("canOffer", intent.getStatus() == IntentStatus.PENDING
                    || intent.getStatus() == IntentStatus.WAITLISTED);
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("quota", mentor.getQuota());
        result.put("activeOffers", offerMapper.countActiveByMentor(mentor.getId(), batch.getId()));
        result.put("rows", rows);
        return result;
    }

    @Transactional
    public ApplicationIntent decide(DecisionRequest request) {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.INTERVIEW);
        stageGuard.assertNotLocked(batch.getId());

        ApplicationIntent intent = intentMapper.findById(request.intentId());
        if (intent == null || !Objects.equals(intent.getMentorId(), mentor.getId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "意向记录不存在或不属于你");
        }
        if (!Objects.equals(intent.getBatchId(), batch.getId())) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "该意向不属于当前批次");
        }
        if (interviewMapper.existsCompleted(intent.getApplicationId(), intent.getLabId()) == 0) {
            throw new BusinessException(ErrorCode.INTERVIEW_REQUIRED);
        }

        String decision = request.decision().trim().toUpperCase();
        List<Offer> offers = offerMapper.findByStudent(batch.getId(), intent.getStudentId());
        Offer activeOffer = offers.stream()
                .filter(o -> o.getStatus() == OfferStatus.OFFERED || o.getStatus() == OfferStatus.CONFIRMED)
                .filter(o -> Objects.equals(o.getIntentId(), intent.getId()))
                .findFirst().orElse(null);
        if (activeOffer != null) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "该意向已有生效的拟录取记录，请勿重复操作");
        }

        switch (decision) {
            case "OFFER" -> {
                selectionService.createOfferForIntent(intent,
                        com.labselection.domain.enums.OfferSource.INITIAL, null, request.reason());
            }
            case "WAITLIST" -> {
                if (request.waitlistRank() == null || request.waitlistRank() <= 0) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "候补必须填写大于 0 的顺位");
                }
                List<ApplicationIntent> waitlisted = intentMapper.findByMentorAndBatch(
                        mentor.getId(), batch.getId(), "WAITLISTED", null, 0, 500);
                boolean duplicatedRank = waitlisted.stream()
                        .anyMatch(i -> !Objects.equals(i.getId(), intent.getId())
                                && Objects.equals(i.getWaitlistRank(), request.waitlistRank()));
                if (duplicatedRank) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "该候补顺位已被占用，请调整");
                }
                try {
                    intentMapper.updateDecision(intent.getId(), IntentStatus.WAITLISTED.name(),
                            request.waitlistRank(), request.comment(), AuthContext.userId(), LocalDateTime.now());
                } catch (DuplicateKeyException ex) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "该候补顺位已被占用，请调整");
                }
                auditService.log(batch.getId(), "DECISION_WAITLIST", "INTENT", intent.getId(),
                        "学生 " + intent.getStudentName() + " 列入候补，顺位 " + request.waitlistRank(),
                        request.reason());
            }
            case "REJECT" -> {
                intentMapper.updateDecision(intent.getId(), IntentStatus.REJECTED.name(),
                        null, request.comment(), AuthContext.userId(), LocalDateTime.now());
                auditService.log(batch.getId(), "DECISION_REJECT", "INTENT", intent.getId(),
                        "学生 " + intent.getStudentName() + " 初选不录取", request.reason());
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的初选结论");
        }
        return intentMapper.findById(intent.getId());
    }

    public List<Offer> offers() {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        return offerMapper.findByMentor(batch.getId(), mentor.getId());
    }

    public List<FinalAssignment> finalStudents() {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        return assignmentMapper.findByMentor(mentor.getId(), batch.getId());
    }

    public Map<String, Object> supplementIntents() {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        List<SupplementIntent> intents = supplementMapper.findIntentsByMentor(
                mentor.getId(), batch.getId(), null);
        SupplementRound round = supplementMapper.findOpenRound(batch.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("round", round);
        result.put("intents", intents);
        return result;
    }

    @Transactional
    public SupplementIntent decideSupplement(SupplementDecisionRequest request) {
        Mentor mentor = currentMentor();
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.SUPPLEMENT);
        stageGuard.assertNotLocked(batch.getId());

        SupplementIntent intent = supplementMapper.findIntentById(request.supplementIntentId());
        if (intent == null || !Objects.equals(intent.getMentorId(), mentor.getId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "补录意向不存在或不属于你");
        }
        SupplementRound round = supplementMapper.findRoundById(intent.getRoundId());
        if (round == null || round.getStatus() != SupplementStatus.OPEN) {
            throw new BusinessException(ErrorCode.SUPPLEMENT_CLOSED);
        }
        stageGuard.assertWindow(round.getStartAt(), round.getEndAt(), "补录考核");

        List<Offer> offers = offerMapper.findByStudent(batch.getId(), intent.getStudentId());
        boolean hasActive = offers.stream().anyMatch(o -> o.getStatus() == OfferStatus.OFFERED
                && Objects.equals(o.getSupplementIntentId(), intent.getId()));
        if (hasActive) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "该补录意向已发放拟录取");
        }

        String decision = request.decision().trim().toUpperCase();
        switch (decision) {
            case "OFFER" -> selectionService.createOfferForSupplementIntent(
                    intent, round.getRoundNo(), request.reason());
            case "WAITLIST" -> {
                if (request.waitlistRank() == null || request.waitlistRank() <= 0) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "候补必须填写大于 0 的顺位");
                }
                try {
                    supplementMapper.updateIntentStatus(intent.getId(), SupplementIntentStatus.WAITLISTED.name(),
                            request.waitlistRank(), request.comment(), AuthContext.userId(), LocalDateTime.now());
                } catch (DuplicateKeyException ex) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "该候补顺位已被占用，请调整");
                }
                auditService.log(batch.getId(), "SUPPLEMENT_WAITLIST", "SUPPLEMENT_INTENT", intent.getId(),
                        "补录学生 " + intent.getStudentName() + " 列入候补，顺位 " + request.waitlistRank(),
                        request.reason());
            }
            case "REJECT" -> {
                supplementMapper.updateIntentStatus(intent.getId(), SupplementIntentStatus.REJECTED.name(),
                        null, request.comment(), AuthContext.userId(), LocalDateTime.now());
                auditService.log(batch.getId(), "SUPPLEMENT_REJECT", "SUPPLEMENT_INTENT", intent.getId(),
                        "补录学生 " + intent.getStudentName() + " 未录取", request.reason());
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的补录结论");
        }
        return supplementMapper.findIntentById(intent.getId());
    }
}
