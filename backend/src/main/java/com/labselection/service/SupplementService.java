package com.labselection.service;

import com.labselection.auth.AuthContext;
import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.domain.entity.ApplicationIntent;
import com.labselection.domain.entity.Lab;
import com.labselection.domain.entity.Mentor;
import com.labselection.domain.entity.Offer;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.entity.SupplementIntent;
import com.labselection.domain.entity.SupplementRound;
import com.labselection.domain.enums.BatchStatus;
import com.labselection.domain.enums.IntentStatus;
import com.labselection.domain.enums.OfferSource;
import com.labselection.domain.enums.OfferStatus;
import com.labselection.domain.enums.SupplementIntentStatus;
import com.labselection.domain.enums.SupplementStatus;
import com.labselection.dto.SupplementRoundRequest;
import com.labselection.mapper.AssignmentMapper;
import com.labselection.mapper.IntentMapper;
import com.labselection.mapper.LabMapper;
import com.labselection.mapper.MentorMapper;
import com.labselection.mapper.OfferMapper;
import com.labselection.mapper.SupplementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplementService {

    private final StageGuard stageGuard;
    private final SupplementMapper supplementMapper;
    private final IntentMapper intentMapper;
    private final OfferMapper offerMapper;
    private final AssignmentMapper assignmentMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final SelectionService selectionService;
    private final AuditService auditService;

    public List<SupplementRound> rounds(Long batchId) {
        SelectionBatch batch = batchId == null ? stageGuard.current() : stageGuard.require(batchId);
        return supplementMapper.findRoundsByBatch(batch.getId());
    }

    @Transactional
    public SupplementRound createRound(SupplementRoundRequest request) {
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.SUPPLEMENT);
        stageGuard.assertNotLocked(batch.getId());
        if (supplementMapper.findRoundByBatchAndNo(batch.getId(), request.roundNo()) != null) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "该轮次已存在");
        }
        SupplementRound round = new SupplementRound();
        round.setBatchId(batch.getId());
        round.setRoundNo(request.roundNo());
        round.setTitle(request.title() == null || request.title().isBlank()
                ? "第" + request.roundNo() + "轮补录" : request.title());
        round.setStatus(SupplementStatus.SCHEDULED);
        round.setStartAt(request.startAt() == null ? batch.getSupplementStart() : request.startAt());
        round.setEndAt(request.endAt() == null ? batch.getSupplementEnd() : request.endAt());
        round.setCreatedBy(AuthContext.userId());
        supplementMapper.insertRound(round);
        auditService.log(batch.getId(), "SUPPLEMENT_ROUND_CREATE", "SUPPLEMENT_ROUND", round.getId(),
                "创建补录轮次：" + round.getTitle(), null);
        return supplementMapper.findRoundById(round.getId());
    }

    @Transactional
    public SupplementRound ensureFirstRound(Long batchId) {
        if (supplementMapper.countRoundsByBatch(batchId) > 0) {
            return supplementMapper.findRoundsByBatch(batchId).get(0);
        }
        SelectionBatch batch = stageGuard.require(batchId);
        SupplementRound round = new SupplementRound();
        round.setBatchId(batchId);
        round.setRoundNo(1);
        round.setTitle("第一轮补录");
        round.setStatus(SupplementStatus.SCHEDULED);
        round.setStartAt(batch.getSupplementStart());
        round.setEndAt(batch.getSupplementEnd());
        round.setCreatedBy(AuthContext.userId());
        supplementMapper.insertRound(round);
        auditService.log(batchId, "SUPPLEMENT_ROUND_CREATE", "SUPPLEMENT_ROUND", round.getId(),
                "进入补录阶段，系统自动创建第一轮补录", "流程规则");
        return supplementMapper.findRoundById(round.getId());
    }

    @Transactional
    public SupplementRound openRound(Long roundId) {
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.SUPPLEMENT);
        stageGuard.assertNotLocked(batch.getId());
        SupplementRound round = requireRound(roundId, batch.getId());
        if (round.getStatus() == SupplementStatus.CLOSED) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "已结束的轮次不能重新开放");
        }
        SupplementRound current = supplementMapper.findOpenRound(batch.getId());
        if (current != null && !Objects.equals(current.getId(), round.getId())) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "已有进行中的补录轮次，请先结束");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = round.getEndAt() == null ? batch.getSupplementEnd() : round.getEndAt();
        supplementMapper.updateRoundStatus(round.getId(), SupplementStatus.OPEN.name(), now, end);
        auditService.log(batch.getId(), "SUPPLEMENT_ROUND_OPEN", "SUPPLEMENT_ROUND", round.getId(),
                "开放补录轮次：" + round.getTitle(), null);
        return supplementMapper.findRoundById(round.getId());
    }

    @Transactional
    public Map<String, Object> closeRound(Long roundId) {
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.SUPPLEMENT);
        stageGuard.assertNotLocked(batch.getId());
        SupplementRound round = requireRound(roundId, batch.getId());
        if (round.getStatus() != SupplementStatus.OPEN) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "只有进行中的轮次可以结束");
        }
        supplementMapper.updateRoundStatus(round.getId(), SupplementStatus.CLOSED.name(), null, LocalDateTime.now());
        int expired = expireRoundOffers(batch.getId(), round.getRoundNo());
        auditService.log(batch.getId(), "SUPPLEMENT_ROUND_CLOSE", "SUPPLEMENT_ROUND", round.getId(),
                "结束补录轮次：" + round.getTitle() + "，逾期未确认拟录取 " + expired + " 条",
                null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("round", supplementMapper.findRoundById(round.getId()));
        result.put("expiredOffers", expired);
        return result;
    }

    private int expireRoundOffers(Long batchId, int roundNo) {
        List<Offer> offers = offerMapper.findByBatch(batchId, OfferStatus.OFFERED.name());
        int expired = 0;
        for (Offer offer : offers) {
            if (!Objects.equals(offer.getRoundNo(), roundNo)) {
                continue;
            }
            offerMapper.updateStatus(offer.getId(), OfferStatus.EXPIRED.name(), null);
            if (offer.getIntentId() != null) {
                intentMapper.updateStatus(offer.getIntentId(), IntentStatus.DECLINED.name());
            }
            if (offer.getSupplementIntentId() != null) {
                supplementMapper.updateIntentStatus(offer.getSupplementIntentId(),
                        SupplementIntentStatus.DECLINED.name(), null, "补录轮次结束仍未确认",
                        AuthContext.userId(), LocalDateTime.now());
            }
            expired++;
        }
        return expired;
    }

    /**
     * 依据原批次候补顺位自动递补：导师有余额时，按顺位把候补学生转为拟录取。
     */
    @Transactional
    public Map<String, Object> promoteWaitlist(Long roundId) {
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.SUPPLEMENT);
        stageGuard.assertNotLocked(batch.getId());
        SupplementRound round = requireRound(roundId, batch.getId());
        if (round.getStatus() != SupplementStatus.OPEN) {
            throw new BusinessException(ErrorCode.SUPPLEMENT_CLOSED, "补录轮次未开放");
        }

        java.util.Set<Long> collegeLabIds = collegeLabIds(batch.getId());
        List<ApplicationIntent> waitlists = intentMapper.findByBatch(batch.getId()).stream()
                .filter(i -> i.getStatus() == IntentStatus.WAITLISTED)
                .filter(i -> collegeLabIds.contains(i.getLabId()))
                .sorted(Comparator.comparing((ApplicationIntent i) -> i.getMentorId())
                        .thenComparing(i -> i.getWaitlistRank() == null ? 9999 : i.getWaitlistRank()))
                .toList();

        List<Map<String, Object>> promoted = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        for (ApplicationIntent intent : waitlists) {
            if (assignmentMapper.findByStudentAndBatch(batch.getId(), intent.getStudentId()) != null) {
                skipped.add(skip(intent, "学生已完成录取"));
                continue;
            }
            boolean alreadyOffered = offerMapper.findByStudent(batch.getId(), intent.getStudentId()).stream()
                    .anyMatch(o -> o.getStatus() == OfferStatus.OFFERED
                            && Objects.equals(o.getIntentId(), intent.getId()));
            if (alreadyOffered) {
                skipped.add(skip(intent, "该意向已递补"));
                continue;
            }
            try {
                Offer offer = selectionService.createOfferForIntent(intent,
                        OfferSource.PROMOTION, round.getRoundNo(), "依据候补顺位自动递补");
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("offerId", offer.getId());
                row.put("studentName", intent.getStudentName());
                row.put("mentorName", intent.getMentorName());
                row.put("waitlistRank", intent.getWaitlistRank());
                promoted.add(row);
            } catch (BusinessException ex) {
                skipped.add(skip(intent, ex.getMessage()));
            }
        }
        auditService.log(batch.getId(), "SUPPLEMENT_PROMOTE", "SUPPLEMENT_ROUND", round.getId(),
                "候补顺位递补：" + promoted.size() + " 人转为拟录取", null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("promoted", promoted);
        result.put("skipped", skipped);
        result.put("promotedCount", promoted.size());
        result.put("round", supplementMapper.findRoundById(round.getId()));
        return result;
    }

    private Map<String, Object> skip(ApplicationIntent intent, String reason) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("studentName", intent.getStudentName());
        map.put("mentorName", intent.getMentorName());
        map.put("waitlistRank", intent.getWaitlistRank());
        map.put("reason", reason);
        return map;
    }

    public Map<String, Object> waitlistOverview(Long batchId) {
        SelectionBatch batch = batchId == null ? stageGuard.current() : stageGuard.require(batchId);
        java.util.Set<Long> collegeLabIds = collegeLabIds(batch.getId());
        List<ApplicationIntent> waitlists = intentMapper.findByBatch(batch.getId()).stream()
                .filter(i -> i.getStatus() == IntentStatus.WAITLISTED)
                .filter(i -> collegeLabIds.contains(i.getLabId()))
                .sorted(Comparator.comparing((ApplicationIntent i) -> i.getMentorId())
                        .thenComparing(i -> i.getWaitlistRank() == null ? 9999 : i.getWaitlistRank()))
                .toList();
        Map<Long, List<ApplicationIntent>> byMentor = waitlists.stream()
                .collect(Collectors.groupingBy(ApplicationIntent::getMentorId, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<Long, List<ApplicationIntent>> entry : byMentor.entrySet()) {
            Mentor mentor = mentorMapper.findById(entry.getKey());
            if (mentor == null) {
                continue;
            }
            long used = Math.max(offerMapper.countActiveByMentor(mentor.getId(), batch.getId()),
                    assignmentMapper.countByMentor(mentor.getId(), batch.getId()));
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("mentor", mentor);
            group.put("remainingQuota", Math.max(0, mentor.getQuota() - used));
            group.put("waitlist", entry.getValue());
            groups.add(group);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("groups", groups);
        result.put("totalWaitlist", waitlists.size());
        return result;
    }

    private SupplementRound requireRound(Long roundId, Long batchId) {
        SupplementRound round = supplementMapper.findRoundById(roundId);
        if (round == null || !Objects.equals(round.getBatchId(), batchId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "补录轮次不存在");
        }
        return round;
    }

    private java.util.Set<Long> collegeLabIds(Long batchId) {
        Long collegeId = AuthContext.get().collegeId();
        if (collegeId == null) {
            return java.util.Set.of();
        }
        return labMapper.listWithStats(batchId, collegeId).stream()
                .map(Lab::getId)
                .collect(Collectors.toSet());
    }
}
