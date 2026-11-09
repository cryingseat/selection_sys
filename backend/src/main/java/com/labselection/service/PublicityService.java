package com.labselection.service;

import com.labselection.auth.AuthContext;
import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.domain.entity.FinalAssignment;
import com.labselection.domain.entity.Publicity;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.enums.AssignmentStatus;
import com.labselection.domain.enums.BatchStatus;
import com.labselection.domain.enums.PublicityStatus;
import com.labselection.mapper.AssignmentMapper;
import com.labselection.mapper.BatchMapper;
import com.labselection.mapper.PublicityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PublicityService {

    private final StageGuard stageGuard;
    private final PublicityMapper publicityMapper;
    private final AssignmentMapper assignmentMapper;
    private final BatchMapper batchMapper;
    private final AuditService auditService;

    public List<Publicity> list(Long batchId) {
        SelectionBatch batch = batchId == null ? stageGuard.current() : stageGuard.require(batchId);
        return publicityMapper.findByBatch(batch.getId());
    }

    public Map<String, Object> detail(Long id) {
        Publicity publicity = publicityMapper.findDetailById(id);
        if (publicity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公示记录不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("publicity", publicity);
        result.put("assignments", assignmentMapper.findPublicByBatch(publicity.getBatchId()));
        return result;
    }

    @Transactional
    public Publicity saveDraft(Publicity request) {
        SelectionBatch batch = request.getBatchId() == null
                ? stageGuard.current() : stageGuard.require(request.getBatchId());
        stageGuard.assertStage(batch, BatchStatus.PUBLICITY);
        stageGuard.assertNotLocked(batch.getId());
        long admitted = assignmentMapper.countByBatch(batch.getId());
        if (admitted == 0) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "暂无最终录取结果，不能生成公示");
        }
        String title = request.getTitle() == null || request.getTitle().isBlank()
                ? batch.getName() + "拟录取名单公示" : request.getTitle();
        Publicity current = publicityMapper.findCurrentByBatch(batch.getId());
        if (current != null && current.getStatus() == PublicityStatus.DRAFT) {
            publicityMapper.updateDraft(current.getId(), title, request.getContent(),
                    request.getStartAt() == null ? batch.getPublicityStart() : request.getStartAt(),
                    request.getEndAt() == null ? batch.getPublicityEnd() : request.getEndAt());
            auditService.log(batch.getId(), "PUBLICITY_SAVE", "PUBLICITY", current.getId(),
                    "更新公示草稿", null);
            return publicityMapper.findDetailById(current.getId());
        }
        Publicity publicity = new Publicity();
        publicity.setBatchId(batch.getId());
        publicity.setTitle(title);
        publicity.setContent(request.getContent());
        publicity.setStartAt(request.getStartAt() == null ? batch.getPublicityStart() : request.getStartAt());
        publicity.setEndAt(request.getEndAt() == null ? batch.getPublicityEnd() : request.getEndAt());
        publicity.setStatus(PublicityStatus.DRAFT);
        publicityMapper.insert(publicity);
        auditService.log(batch.getId(), "PUBLICITY_CREATE", "PUBLICITY", publicity.getId(),
                "生成公示草稿：" + title, null);
        return publicityMapper.findDetailById(publicity.getId());
    }

    @Transactional
    public Publicity publish(Long id) {
        Publicity publicity = publicityMapper.lockById(id);
        if (publicity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公示记录不存在");
        }
        SelectionBatch batch = stageGuard.require(publicity.getBatchId());
        stageGuard.assertStage(batch, BatchStatus.PUBLICITY);
        if (publicity.getStatus() != PublicityStatus.DRAFT) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "只有草稿状态的公示可以发布");
        }
        if (assignmentMapper.countByBatch(batch.getId()) == 0) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "暂无最终录取结果");
        }
        publicityMapper.updateStatus(publicity.getId(), PublicityStatus.PUBLISHED.name(),
                AuthContext.userId(), LocalDateTime.now(), null);
        List<FinalAssignment> assignments = assignmentMapper.findByBatch(batch.getId());
        for (FinalAssignment assignment : assignments) {
            if (assignment.getStatus() == AssignmentStatus.CONFIRMED) {
                assignmentMapper.updateStatus(assignment.getId(), AssignmentStatus.PUBLICIZED.name());
            }
        }
        auditService.log(batch.getId(), "PUBLICITY_PUBLISH", "PUBLICITY", publicity.getId(),
                "发布公示：" + publicity.getTitle(), null);
        return publicityMapper.findDetailById(publicity.getId());
    }

    @Transactional
    public Publicity close(Long id) {
        Publicity publicity = publicityMapper.lockById(id);
        if (publicity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公示记录不存在");
        }
        if (publicity.getStatus() != PublicityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "只有公示中的记录可以结束");
        }
        publicityMapper.updateStatus(publicity.getId(), PublicityStatus.CLOSED.name(),
                null, null, LocalDateTime.now());
        assignmentMapper.updateStatusByBatch(publicity.getBatchId(), AssignmentStatus.LOCKED.name());
        batchMapper.updateStatus(publicity.getBatchId(), BatchStatus.FINISHED.name());
        auditService.log(publicity.getBatchId(), "PUBLICITY_CLOSE", "PUBLICITY", publicity.getId(),
                "公示结束，最终录取结果锁定", null);
        return publicityMapper.findDetailById(publicity.getId());
    }

    @Transactional
    public Publicity revoke(Long id, String reason) {
        Publicity publicity = publicityMapper.lockById(id);
        if (publicity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公示记录不存在");
        }
        if (publicity.getStatus() != PublicityStatus.PUBLISHED
                && publicity.getStatus() != PublicityStatus.CLOSED) {
            throw new BusinessException(ErrorCode.STATE_INVALID, "当前公示状态不支持撤回");
        }
        publicityMapper.updateStatus(publicity.getId(), PublicityStatus.REVOKED.name(),
                null, null, LocalDateTime.now());
        assignmentMapper.updateStatusByBatch(publicity.getBatchId(), AssignmentStatus.CONFIRMED.name());
        batchMapper.updateStatus(publicity.getBatchId(), BatchStatus.ADJUSTMENT.name());
        auditService.log(publicity.getBatchId(), "PUBLICITY_REVOKE", "PUBLICITY", publicity.getId(),
                "撤回公示，解锁结果以便修改后重新公示", reason);
        return publicityMapper.findDetailById(publicity.getId());
    }

    public Publicity currentDraftOrPublished(Long batchId) {
        return publicityMapper.findCurrentByBatch(batchId);
    }

    public Publicity publicResult(Long batchId) {
        Publicity publicity = publicityMapper.findPublishedOrClosed(batchId);
        if (publicity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该批次尚未发布公示");
        }
        return publicity;
    }

    public void assertNotCurrentLocked(Long batchId) {
        Publicity publicity = publicityMapper.findCurrentByBatch(batchId);
        if (publicity != null
                && (publicity.getStatus() == PublicityStatus.PUBLISHED
                || publicity.getStatus() == PublicityStatus.CLOSED)) {
            throw new BusinessException(ErrorCode.BATCH_LOCKED);
        }
    }
}
