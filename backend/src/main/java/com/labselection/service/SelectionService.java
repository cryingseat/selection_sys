package com.labselection.service;

import com.labselection.auth.AuthContext;
import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.domain.entity.Application;
import com.labselection.domain.entity.ApplicationIntent;
import com.labselection.domain.entity.FinalAssignment;
import com.labselection.domain.entity.Lab;
import com.labselection.domain.entity.Mentor;
import com.labselection.domain.entity.Offer;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.entity.SupplementIntent;
import com.labselection.domain.entity.SupplementRound;
import com.labselection.domain.enums.AssignmentSource;
import com.labselection.domain.enums.AssignmentStatus;
import com.labselection.domain.enums.BatchStatus;
import com.labselection.domain.enums.IntentStatus;
import com.labselection.domain.enums.OfferSource;
import com.labselection.domain.enums.OfferStatus;
import com.labselection.domain.enums.SupplementIntentStatus;
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
import java.util.stream.Collectors;

/**
 * 拟录取、确认、放弃、逾期与名额控制的核心服务。
 * 所有写操作使用事务 + 行锁 + 唯一约束共同保证不会超额、不会一人多组。
 */
@Service
@RequiredArgsConstructor
public class SelectionService {

    private final StageGuard stageGuard;
    private final ApplicationMapper applicationMapper;
    private final IntentMapper intentMapper;
    private final InterviewMapper interviewMapper;
    private final OfferMapper offerMapper;
    private final AssignmentMapper assignmentMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final SupplementMapper supplementMapper;
    private final AuditService auditService;

    @Transactional
    public Offer createOfferForIntent(ApplicationIntent intent, OfferSource source,
                                      Integer roundNo, String reason) {
        Application application = applicationMapper.findById(intent.getApplicationId());
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报名记录不存在");
        }
        if (application.getStatus() != com.labselection.domain.enums.ApplicationStatus.SUBMITTED
                && application.getStatus() != com.labselection.domain.enums.ApplicationStatus.LOCKED) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "该报名尚未提交，不能发放拟录取");
        }
        if (interviewMapper.existsCompleted(application.getId(), intent.getLabId()) == 0) {
            throw new BusinessException(ErrorCode.INTERVIEW_REQUIRED);
        }
        if (assignmentMapper.findByStudentAndBatch(intent.getBatchId(), intent.getStudentId()) != null) {
            throw new BusinessException(ErrorCode.OFFER_CONFLICT, "该学生已完成录取，不能重复发放拟录取");
        }
        List<Offer> existing = offerMapper.findByStudent(intent.getBatchId(), intent.getStudentId());
        boolean hasConfirmed = existing.stream().anyMatch(o -> o.getStatus() == OfferStatus.CONFIRMED);
        if (hasConfirmed) {
            throw new BusinessException(ErrorCode.OFFER_CONFLICT);
        }
        boolean alreadyOffered = existing.stream()
                .anyMatch(o -> o.getStatus() == OfferStatus.OFFERED && Objects.equals(o.getIntentId(), intent.getId()));
        if (alreadyOffered) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "该意向已发放拟录取，请勿重复操作");
        }
        lockAndCheckQuota(intent.getMentorId(), intent.getLabId(), intent.getBatchId());

        Offer offer = new Offer();
        offer.setBatchId(intent.getBatchId());
        offer.setStudentId(intent.getStudentId());
        offer.setMentorId(intent.getMentorId());
        offer.setLabId(intent.getLabId());
        offer.setApplicationId(intent.getApplicationId());
        offer.setIntentId(intent.getId());
        offer.setSource(source);
        offer.setStatus(OfferStatus.OFFERED);
        offer.setRoundNo(roundNo);
        offer.setCreatedBy(AuthContext.userId());
        offerMapper.insert(offer);

        intentMapper.updateDecision(intent.getId(), IntentStatus.OFFERED.name(), null,
                intent.getComment(), AuthContext.userId(), LocalDateTime.now());

        auditService.log(intent.getBatchId(), "OFFER_CREATE", "OFFER", offer.getId(),
                "向学生发放拟录取意向（" + source.getLabel() + "）", reason);
        return offer;
    }

    @Transactional
    public Offer createOfferForSupplementIntent(SupplementIntent intent, Integer roundNo, String reason) {
        if (assignmentMapper.findByStudentAndBatch(intent.getBatchId(), intent.getStudentId()) != null) {
            throw new BusinessException(ErrorCode.OFFER_CONFLICT);
        }
        List<Offer> existing = offerMapper.findByStudent(intent.getBatchId(), intent.getStudentId());
        if (existing.stream().anyMatch(o -> o.getStatus() == OfferStatus.CONFIRMED)) {
            throw new BusinessException(ErrorCode.OFFER_CONFLICT);
        }
        boolean duplicated = existing.stream().anyMatch(o -> o.getStatus() == OfferStatus.OFFERED
                && Objects.equals(o.getSupplementIntentId(), intent.getId()));
        if (duplicated) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "该补录意向已发放拟录取，请勿重复操作");
        }
        lockAndCheckQuota(intent.getMentorId(), intent.getLabId(), intent.getBatchId());

        Offer offer = new Offer();
        offer.setBatchId(intent.getBatchId());
        offer.setStudentId(intent.getStudentId());
        offer.setMentorId(intent.getMentorId());
        offer.setLabId(intent.getLabId());
        offer.setApplicationId(null);
        offer.setSupplementIntentId(intent.getId());
        offer.setSource(OfferSource.SUPPLEMENT);
        offer.setStatus(OfferStatus.OFFERED);
        offer.setRoundNo(roundNo);
        offer.setCreatedBy(AuthContext.userId());
        offerMapper.insert(offer);
        supplementMapper.updateIntentStatus(intent.getId(), SupplementIntentStatus.OFFERED.name(),
                null, intent.getComment(), AuthContext.userId(), LocalDateTime.now());
        auditService.log(intent.getBatchId(), "SUPPLEMENT_OFFER_CREATE", "OFFER", offer.getId(),
                "补录阶段向学生发放拟录取意向", reason);
        return offer;
    }

    @Transactional
    public FinalAssignment confirmOffer(Long offerId, Long studentId, String reason) {
        Offer offer = offerMapper.lockById(offerId);
        if (offer == null) {
            throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
        }
        if (!Objects.equals(offer.getStudentId(), studentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (offer.getStatus() != OfferStatus.OFFERED) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "该拟录取记录已不是待确认状态");
        }
        SelectionBatch batch = stageGuard.require(offer.getBatchId());
        stageGuard.assertStage(batch, BatchStatus.CONFIRM, BatchStatus.SUPPLEMENT);
        stageGuard.assertNotLocked(batch.getId());
        if (batch.getStatus() == BatchStatus.CONFIRM) {
            stageGuard.assertWindow(batch.getConfirmStart(), batch.getConfirmEnd(), "拟录取确认");
        } else if (offer.getRoundNo() != null) {
            SupplementRound round = supplementMapper.findRoundByBatchAndNo(
                    offer.getBatchId(), offer.getRoundNo());
            if (round != null) {
                stageGuard.assertWindow(round.getStartAt(), round.getEndAt(), "补录确认");
            }
        }

        FinalAssignment existing = assignmentMapper.findByStudentAndBatch(offer.getBatchId(), studentId);
        if (existing != null) {
            throw new BusinessException(ErrorCode.CONFIRM_CONFLICT, "你已完成录取确认，不能重复确认");
        }

        AssignmentSource source = switch (offer.getSource()) {
            case SUPPLEMENT -> AssignmentSource.SUPPLEMENT;
            case PROMOTION -> AssignmentSource.PROMOTION;
            default -> AssignmentSource.CONFIRM;
        };
        FinalAssignment assignment = new FinalAssignment();
        assignment.setBatchId(offer.getBatchId());
        assignment.setStudentId(offer.getStudentId());
        assignment.setApplicationId(offer.getApplicationId());
        assignment.setMentorId(offer.getMentorId());
        assignment.setLabId(offer.getLabId());
        assignment.setSource(source);
        assignment.setStatus(AssignmentStatus.CONFIRMED);
        assignment.setConfirmedAt(LocalDateTime.now());
        assignment.setRemark(reason);
        try {
            assignmentMapper.insert(assignment);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFIRM_CONFLICT, "你已完成录取确认，不能重复确认");
        }

        offerMapper.updateStatus(offer.getId(), OfferStatus.CONFIRMED.name(), LocalDateTime.now());
        releaseOtherOffers(offer, studentId);
        auditService.log(offer.getBatchId(), "OFFER_CONFIRM", "ASSIGNMENT", assignment.getId(),
                "学生确认拟录取：" + offer.getMentorName() + " / " + offer.getLabName(), reason);
        return assignment;
    }

    private void releaseOtherOffers(Offer chosen, Long studentId) {
        List<Offer> offers = offerMapper.findByStudent(chosen.getBatchId(), studentId);
        for (Offer other : offers) {
            if (Objects.equals(other.getId(), chosen.getId())) {
                continue;
            }
            if (other.getStatus() == OfferStatus.OFFERED) {
                offerMapper.updateStatus(other.getId(), OfferStatus.RELEASED.name(), null);
                if (other.getIntentId() != null) {
                    intentMapper.updateStatus(other.getIntentId(), IntentStatus.RELEASED.name());
                }
                if (other.getSupplementIntentId() != null) {
                    supplementMapper.updateIntentStatus(other.getSupplementIntentId(),
                            SupplementIntentStatus.DECLINED.name(), null, "学生已确认其他意向",
                            AuthContext.userId(), LocalDateTime.now());
                }
            }
        }
        ApplicationIntent chosenIntent = chosen.getIntentId() == null ? null : intentMapper.findById(chosen.getIntentId());
        if (chosenIntent != null) {
            intentMapper.updateStatus(chosenIntent.getId(), IntentStatus.CONFIRMED.name());
            // 同一报名下未被选中的其他意向统一释放
            List<ApplicationIntent> intents = intentMapper.findByApplicationId(chosenIntent.getApplicationId());
            for (ApplicationIntent intent : intents) {
                if (!Objects.equals(intent.getId(), chosenIntent.getId())
                        && intent.getStatus() != IntentStatus.RELEASED
                        && intent.getStatus() != IntentStatus.DECLINED) {
                    intentMapper.updateStatus(intent.getId(), IntentStatus.RELEASED.name());
                }
            }
        }
        if (chosen.getSupplementIntentId() != null) {
            supplementMapper.updateIntentStatus(chosen.getSupplementIntentId(),
                    SupplementIntentStatus.CONFIRMED.name(), null, "学生确认补录拟录取",
                    AuthContext.userId(), LocalDateTime.now());
        }
    }

    @Transactional
    public void declineOffer(Long offerId, Long studentId, String reason) {
        Offer offer = offerMapper.lockById(offerId);
        if (offer == null) {
            throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
        }
        if (!Objects.equals(offer.getStudentId(), studentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (offer.getStatus() != OfferStatus.OFFERED) {
            throw new BusinessException(ErrorCode.STATE_INVALID);
        }
        SelectionBatch batch = stageGuard.require(offer.getBatchId());
        stageGuard.assertStage(batch, BatchStatus.CONFIRM, BatchStatus.SUPPLEMENT);
        stageGuard.assertNotLocked(batch.getId());

        offerMapper.updateStatus(offer.getId(), OfferStatus.DECLINED.name(), null);
        if (offer.getIntentId() != null) {
            intentMapper.updateStatus(offer.getIntentId(), IntentStatus.DECLINED.name());
        }
        if (offer.getSupplementIntentId() != null) {
            supplementMapper.updateIntentStatus(offer.getSupplementIntentId(),
                    SupplementIntentStatus.DECLINED.name(), null, reason,
                    AuthContext.userId(), LocalDateTime.now());
        }
        auditService.log(offer.getBatchId(), "OFFER_DECLINE", "OFFER", offer.getId(),
                "学生主动放弃拟录取意向", reason);
    }

    @Transactional
    public int declineAll(Long studentId, String reason) {
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.CONFIRM, BatchStatus.SUPPLEMENT);
        stageGuard.assertNotLocked(batch.getId());
        List<Offer> offers = offerMapper.findByStudent(batch.getId(), studentId).stream()
                .filter(o -> o.getStatus() == OfferStatus.OFFERED)
                .toList();
        for (Offer offer : offers) {
            declineOffer(offer.getId(), studentId, reason);
        }
        return offers.size();
    }

    @Transactional
    public int autoConfirmSingleOffers(Long batchId) {
        SelectionBatch batch = stageGuard.require(batchId);
        stageGuard.assertStage(batch, BatchStatus.CONFIRM);
        stageGuard.assertNotLocked(batchId);
        List<Offer> offered = offerMapper.findByBatch(batchId, OfferStatus.OFFERED.name());
        Map<Long, List<Offer>> byStudent = offered.stream()
                .collect(Collectors.groupingBy(Offer::getStudentId, LinkedHashMap::new, Collectors.toList()));
        int count = 0;
        for (Map.Entry<Long, List<Offer>> entry : byStudent.entrySet()) {
            List<Offer> list = entry.getValue();
            if (list.size() == 1) {
                Offer only = list.get(0);
                if (assignmentMapper.findByStudentAndBatch(batchId, entry.getKey()) == null) {
                    // 内部调用，直接执行确认；自动确认不受“学生主动确认”时间窗限制
                    Offer locked = offerMapper.lockById(only.getId());
                    if (locked != null && locked.getStatus() == OfferStatus.OFFERED) {
                        insertAutoAssignment(locked);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void insertAutoAssignment(Offer offer) {
        FinalAssignment assignment = new FinalAssignment();
        assignment.setBatchId(offer.getBatchId());
        assignment.setStudentId(offer.getStudentId());
        assignment.setApplicationId(offer.getApplicationId());
        assignment.setMentorId(offer.getMentorId());
        assignment.setLabId(offer.getLabId());
        assignment.setSource(AssignmentSource.CONFIRM);
        assignment.setStatus(AssignmentStatus.CONFIRMED);
        assignment.setConfirmedAt(LocalDateTime.now());
        assignment.setRemark("唯一拟录取意向，系统自动确认");
        try {
            assignmentMapper.insert(assignment);
        } catch (DuplicateKeyException ex) {
            return;
        }
        offerMapper.updateStatus(offer.getId(), OfferStatus.CONFIRMED.name(), LocalDateTime.now());
        releaseOtherOffers(offer, offer.getStudentId());
        auditService.log(offer.getBatchId(), "OFFER_AUTO_CONFIRM", "ASSIGNMENT", assignment.getId(),
                "唯一拟录取意向自动确认", "规则：仅收到 1 个拟录取意向自动确认");
    }

    @Transactional
    public int expirePendingOffers(Long batchId, String reason) {
        stageGuard.assertNotLocked(batchId);
        List<Offer> offered = offerMapper.findByBatch(batchId, OfferStatus.OFFERED.name());
        Map<Long, List<Offer>> byStudent = offered.stream()
                .collect(Collectors.groupingBy(Offer::getStudentId, LinkedHashMap::new, Collectors.toList()));
        int count = 0;
        for (Map.Entry<Long, List<Offer>> entry : byStudent.entrySet()) {
            for (Offer offer : entry.getValue()) {
                offerMapper.updateStatus(offer.getId(), OfferStatus.EXPIRED.name(), null);
                if (offer.getIntentId() != null) {
                    intentMapper.updateStatus(offer.getIntentId(), IntentStatus.DECLINED.name());
                }
                if (offer.getSupplementIntentId() != null) {
                    supplementMapper.updateIntentStatus(offer.getSupplementIntentId(),
                            SupplementIntentStatus.DECLINED.name(), null, reason,
                            AuthContext.userId(), LocalDateTime.now());
                }
                count++;
            }
            auditService.log(batchId, "OFFER_EXPIRE", "STUDENT", entry.getKey(),
                    "确认期限届满，系统按放弃处理并发空缺名额", reason);
        }
        return count;
    }

    @Transactional
    public void releaseOfferForAdjustment(Offer offer, String reason) {
        if (offer.getStatus() == OfferStatus.OFFERED) {
            offerMapper.updateStatus(offer.getId(), OfferStatus.RELEASED.name(), null);
            if (offer.getIntentId() != null) {
                intentMapper.updateStatus(offer.getIntentId(), IntentStatus.RELEASED.name());
            }
            auditService.log(offer.getBatchId(), "OFFER_RELEASE", "OFFER", offer.getId(),
                    "因统筹调剂释放原拟录取名额", reason);
        }
    }

    public long activeOfferCount(Long mentorId, Long batchId) {
        return offerMapper.countActiveByMentor(mentorId, batchId);
    }

    /** 供调剂等直接落库的场景复用名额校验（会锁定导师与实训室行）。 */
    public void assertCapacity(Long mentorId, Long labId, Long batchId) {
        lockAndCheckQuota(mentorId, labId, batchId);
    }

    private void lockAndCheckQuota(Long mentorId, Long labId, Long batchId) {
        Mentor mentor = mentorMapper.lockById(mentorId);
        if (mentor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "导师不存在");
        }
        Lab lab = labMapper.lockById(labId);
        if (lab == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "实训室不存在");
        }
        long activeMentor = offerMapper.countActiveByMentor(mentorId, batchId);
        long finalMentor = assignmentMapper.countByMentor(mentorId, batchId);
        long usedMentor = Math.max(activeMentor, finalMentor);
        if (usedMentor >= mentor.getQuota()) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED,
                    "导师「" + mentor.getName() + "」招生名额已满（" + mentor.getQuota() + " 人）");
        }
        long activeLab = offerMapper.countActiveByLab(labId, batchId);
        long finalLab = assignmentMapper.countByLab(labId, batchId);
        long usedLab = Math.max(activeLab, finalLab);
        if (usedLab >= lab.getTotalQuota()) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED,
                    "实训室「" + lab.getName() + "」总名额已满（" + lab.getTotalQuota() + " 人）");
        }
    }

    public Map<String, Object> summary(Long batchId, Long studentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("offers", offerMapper.findByStudent(batchId, studentId));
        result.put("assignment", assignmentMapper.findByStudentAndBatch(batchId, studentId));
        return result;
    }

    public List<Offer> studentOffers(Long batchId, Long studentId) {
        return offerMapper.findByStudent(batchId, studentId);
    }

    public List<FinalAssignment> assignmentsOfBatch(Long batchId) {
        return assignmentMapper.findByBatch(batchId);
    }
}
