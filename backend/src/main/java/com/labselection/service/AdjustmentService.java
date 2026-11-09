package com.labselection.service;

import com.labselection.auth.AuthContext;
import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.domain.entity.AdjustmentRecord;
import com.labselection.domain.entity.Application;
import com.labselection.domain.entity.ApplicationIntent;
import com.labselection.domain.entity.FinalAssignment;
import com.labselection.domain.entity.Lab;
import com.labselection.domain.entity.Mentor;
import com.labselection.domain.entity.Offer;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.enums.AdjustmentPreference;
import com.labselection.domain.enums.AssignmentSource;
import com.labselection.domain.enums.AssignmentStatus;
import com.labselection.domain.enums.BatchStatus;
import com.labselection.domain.enums.IntentStatus;
import com.labselection.domain.enums.LabStatus;
import com.labselection.domain.enums.OfferStatus;
import com.labselection.mapper.AdjustmentMapper;
import com.labselection.mapper.ApplicationMapper;
import com.labselection.mapper.AssignmentMapper;
import com.labselection.mapper.IntentMapper;
import com.labselection.mapper.LabMapper;
import com.labselection.mapper.MentorMapper;
import com.labselection.mapper.OfferMapper;
import com.labselection.util.CsvUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdjustmentService {

    private final StageGuard stageGuard;
    private final ApplicationMapper applicationMapper;
    private final IntentMapper intentMapper;
    private final AssignmentMapper assignmentMapper;
    private final OfferMapper offerMapper;
    private final AdjustmentMapper adjustmentMapper;
    private final MentorMapper mentorMapper;
    private final LabMapper labMapper;
    private final SelectionService selectionService;
    private final AuditService auditService;

    public List<AdjustmentRecord> records(Long batchId) {
        SelectionBatch batch = batchId == null ? stageGuard.current() : stageGuard.require(batchId);
        return adjustmentMapper.findByBatch(batch.getId());
    }

    public Map<String, Object> workspace() {
        SelectionBatch batch = stageGuard.current();
        List<Application> unadmitted = applicationMapper.listUnadmitted(batch.getId());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Application application : unadmitted) {
            List<ApplicationIntent> intents = intentMapper.findByStudentAndBatch(
                    application.getStudentId(), batch.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("application", application);
            row.put("intents", intents);
            row.put("excludedLabIds", CsvUtils.parseIds(application.getExcludedLabIds()));
            row.put("finalAssignment", assignmentMapper.findByStudentAndBatch(
                    batch.getId(), application.getStudentId()));
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("unadmitted", rows);
        result.put("records", adjustmentMapper.findByBatch(batch.getId()));
        return result;
    }

    @Transactional
    public Map<String, Object> autoAdjust() {
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.ADJUSTMENT);
        stageGuard.assertNotLocked(batch.getId());

        List<Application> applications = applicationMapper.listUnadmitted(batch.getId());
        List<Map<String, Object>> assigned = new ArrayList<>();
        List<Map<String, Object>> talked = new ArrayList<>();

        for (Application application : applications) {
            if (assignmentMapper.findByStudentAndBatch(batch.getId(), application.getStudentId()) != null) {
                continue;
            }
            if (application.getAdjustmentPreference() == AdjustmentPreference.NONE) {
                createTalk(application, batch, "学生不接受任何调剂，进入谈话安置");
                talked.add(talkRow(application, "学生不接受任何调剂"));
                continue;
            }

            List<ApplicationIntent> intents = intentMapper.findByStudentAndBatch(
                    application.getStudentId(), batch.getId());
            Set<Long> appliedLabs = new LinkedHashSet<>();
            intents.forEach(i -> appliedLabs.add(i.getLabId()));
            Set<Long> excluded = new LinkedHashSet<>(CsvUtils.parseIds(application.getExcludedLabIds()));

            boolean done = false;
            // 第一优先级：学生填报过的实训室内部调剂
            for (Long labId : appliedLabs) {
                if (excluded.contains(labId)) {
                    continue;
                }
                Mentor candidate = bestMentor(batch.getId(), labId);
                if (candidate != null) {
                    done = tryAssign(application, batch, candidate, AssignmentSource.INTERNAL_ADJUST,
                            "第一优先级：实训室内部调剂");
                    if (done) {
                        assigned.add(assignRow(application, candidate, "实训室内调剂"));
                        break;
                    }
                }
            }
            if (done) {
                continue;
            }

            // 第二优先级：跨实训室调剂（须学生明确授权）
            if (application.getAdjustmentPreference() == AdjustmentPreference.CROSS) {
                List<Lab> labs = labMapper.findActive();
                for (Lab lab : labs) {
                    if (excluded.contains(lab.getId())) {
                        continue;
                    }
                    Mentor candidate = bestMentor(batch.getId(), lab.getId());
                    if (candidate != null) {
                        done = tryAssign(application, batch, candidate, AssignmentSource.CROSS_ADJUST,
                                "第二优先级：跨实训室调剂");
                        if (done) {
                            assigned.add(assignRow(application, candidate, "跨实训室调剂"));
                            break;
                        }
                    }
                }
            }
            if (!done) {
                createTalk(application, batch, "暂无剩余名额，进入谈话安置");
                talked.add(talkRow(application, "暂无剩余名额"));
            }
        }

        auditService.log(batch.getId(), "ADJUSTMENT_AUTO", "BATCH", batch.getId(),
                "统筹调剂完成：成功 " + assigned.size() + " 人，谈话安置 " + talked.size() + " 人", null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assigned", assigned);
        result.put("talked", talked);
        result.put("assignedCount", assigned.size());
        result.put("talkedCount", talked.size());
        result.put("records", adjustmentMapper.findByBatch(batch.getId()));
        return result;
    }

    @Transactional
    public Map<String, Object> manualAdjust(Long studentId, Long toMentorId, String reason) {
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.ADJUSTMENT);
        stageGuard.assertNotLocked(batch.getId());

        Application application = applicationMapper.findByBatchAndStudent(batch.getId(), studentId);
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该学生没有本批次报名记录");
        }
        if (assignmentMapper.findByStudentAndBatch(batch.getId(), studentId) != null) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "该学生已完成录取");
        }
        Mentor mentor = mentorMapper.findById(toMentorId);
        if (mentor == null || !Boolean.TRUE.equals(mentor.getEnabled())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标导师不存在或已停用");
        }
        Lab lab = labMapper.findById(mentor.getLabId());
        if (lab == null || lab.getStatus() != LabStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标实训室不存在或已停用");
        }
        Set<Long> excluded = new LinkedHashSet<>(CsvUtils.parseIds(application.getExcludedLabIds()));
        if (excluded.contains(lab.getId())) {
            throw new BusinessException(ErrorCode.ADJUST_NOT_ALLOWED, "该学生已明确拒绝此实训室");
        }

        List<ApplicationIntent> intents = intentMapper.findByStudentAndBatch(studentId, batch.getId());
        boolean internal = intents.stream().anyMatch(i -> Objects.equals(i.getLabId(), lab.getId()));
        AssignmentSource source;
        if (internal) {
            if (application.getAdjustmentPreference() == AdjustmentPreference.NONE) {
                throw new BusinessException(ErrorCode.ADJUST_NOT_ALLOWED, "学生不接受调剂");
            }
            source = AssignmentSource.INTERNAL_ADJUST;
        } else {
            if (application.getAdjustmentPreference() != AdjustmentPreference.CROSS) {
                throw new BusinessException(ErrorCode.ADJUST_NOT_ALLOWED,
                        "跨实训室调剂需要学生选择“接受跨实训室调剂”");
            }
            source = AssignmentSource.CROSS_ADJUST;
        }
        boolean success = tryAssign(application, batch, mentor, source,
                reason == null ? "管理员手动调剂" : reason);
        if (!success) {
            throw new BusinessException(ErrorCode.CONFLICT, "调剂写入失败，请刷新后重试");
        }
        return detail(batch, studentId);
    }

    @Transactional
    public void talk(Long studentId, String reason) {
        SelectionBatch batch = stageGuard.current();
        stageGuard.assertStage(batch, BatchStatus.ADJUSTMENT);
        if (assignmentMapper.findByStudentAndBatch(batch.getId(), studentId) != null) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "该学生已完成录取");
        }
        Application application = applicationMapper.findByBatchAndStudent(batch.getId(), studentId);
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报名记录不存在");
        }
        createTalk(application, batch, reason == null ? "管理员标记谈话安置" : reason);
        auditService.log(batch.getId(), "ADJUSTMENT_TALK", "STUDENT", studentId,
                "标记谈话安置", reason);
    }

    public Map<String, Object> detail(SelectionBatch batch, Long studentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assignment", assignmentMapper.findByStudentAndBatch(batch.getId(), studentId));
        result.put("records", adjustmentMapper.findByStudent(batch.getId(), studentId));
        return result;
    }

    private boolean tryAssign(Application application, SelectionBatch batch, Mentor mentor,
                              AssignmentSource source, String reason) {
        try {
            selectionService.assertCapacity(mentor.getId(), mentor.getLabId(), batch.getId());
            FinalAssignment assignment = new FinalAssignment();
            assignment.setBatchId(batch.getId());
            assignment.setStudentId(application.getStudentId());
            assignment.setApplicationId(application.getId());
            assignment.setMentorId(mentor.getId());
            assignment.setLabId(mentor.getLabId());
            assignment.setSource(source);
            assignment.setStatus(AssignmentStatus.CONFIRMED);
            assignment.setConfirmedAt(LocalDateTime.now());
            assignment.setRemark(reason);
            try {
                assignmentMapper.insert(assignment);
            } catch (DuplicateKeyException ex) {
                return false;
            }

            List<ApplicationIntent> intents = intentMapper.findByStudentAndBatch(
                    application.getStudentId(), batch.getId());
            Long fromMentorId = intents.isEmpty() ? null : intents.get(0).getMentorId();
            Long fromLabId = intents.isEmpty() ? null : intents.get(0).getLabId();
            AdjustmentRecord record = new AdjustmentRecord();
            record.setBatchId(batch.getId());
            record.setStudentId(application.getStudentId());
            record.setFromMentorId(fromMentorId);
            record.setFromLabId(fromLabId);
            record.setToMentorId(mentor.getId());
            record.setToLabId(mentor.getLabId());
            record.setType(source);
            record.setStatus(AssignmentStatus.CONFIRMED);
            record.setReason(reason);
            record.setOperatorId(AuthContext.userId());
            adjustmentMapper.insert(record);

            for (ApplicationIntent intent : intents) {
                intentMapper.updateStatus(intent.getId(), IntentStatus.RELEASED.name());
            }
            List<Offer> offers = offerMapper.findByStudent(batch.getId(), application.getStudentId());
            for (Offer offer : offers) {
                if (offer.getStatus() == OfferStatus.OFFERED) {
                    offerMapper.updateStatus(offer.getId(), OfferStatus.RELEASED.name(), null);
                }
            }
            auditService.log(batch.getId(), "ADJUSTMENT_APPLY", "STUDENT", application.getStudentId(),
                    "调剂至 " + mentor.getName() + "（" + source.getLabel() + "）", reason);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private void createTalk(Application application, SelectionBatch batch, String reason) {
        AdjustmentRecord record = new AdjustmentRecord();
        record.setBatchId(batch.getId());
        record.setStudentId(application.getStudentId());
        record.setType(AssignmentSource.TALK);
        record.setStatus(AssignmentStatus.CONFIRMED);
        record.setReason(reason);
        record.setOperatorId(AuthContext.userId());
        adjustmentMapper.insert(record);
    }

    private Mentor bestMentor(Long batchId, Long labId) {
        List<Mentor> mentors = mentorMapper.listWithStats(batchId, labId);
        Lab lab = labMapper.findById(labId);
        if (lab == null || lab.getStatus() != LabStatus.ACTIVE) {
            return null;
        }
        long labUsed = Math.max(offerMapper.countActiveByLab(labId, batchId),
                assignmentMapper.countByLab(labId, batchId));
        if (labUsed >= lab.getTotalQuota()) {
            return null;
        }
        return mentors.stream()
                .filter(m -> Boolean.TRUE.equals(m.getEnabled()))
                .filter(m -> {
                    long used = Math.max(m.getOfferedCount() == null ? 0 : m.getOfferedCount(),
                            m.getConfirmedCount() == null ? 0 : m.getConfirmedCount());
                    return used < m.getQuota();
                })
                .max(Comparator.comparingInt(m -> m.getQuota()
                        - Math.max(m.getOfferedCount() == null ? 0 : m.getOfferedCount(),
                        m.getConfirmedCount() == null ? 0 : m.getConfirmedCount())))
                .orElse(null);
    }

    private Map<String, Object> assignRow(Application application, Mentor mentor, String type) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("studentNo", application.getStudentNo());
        row.put("studentName", application.getStudentName());
        row.put("mentorName", mentor.getName());
        row.put("labName", mentor.getLabName());
        row.put("type", type);
        return row;
    }

    private Map<String, Object> talkRow(Application application, String reason) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("studentNo", application.getStudentNo());
        row.put("studentName", application.getStudentName());
        row.put("reason", reason);
        return row;
    }
}
