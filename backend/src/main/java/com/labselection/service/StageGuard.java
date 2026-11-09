package com.labselection.service;

import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.domain.entity.Publicity;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.enums.BatchStatus;
import com.labselection.domain.enums.PublicityStatus;
import com.labselection.mapper.BatchMapper;
import com.labselection.mapper.PublicityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 所有“阶段 + 时间窗口 + 公示锁”的统一校验入口。
 * 业务规则集中在 Service 层，本组件被各 Service 复用。
 */
@Component
@RequiredArgsConstructor
public class StageGuard {

    private final BatchMapper batchMapper;
    private final PublicityMapper publicityMapper;

    @Value("${app.enforce-time-window:false}")
    private boolean enforceTimeWindow;

    public SelectionBatch current() {
        SelectionBatch batch = batchMapper.findCurrent();
        if (batch != null) {
            return batch;
        }
        List<SelectionBatch> all = batchMapper.findAll();
        if (all.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "尚未创建选拔批次");
        }
        return all.get(0);
    }

    public SelectionBatch require(Long batchId) {
        SelectionBatch batch = batchMapper.findById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "选拔批次不存在");
        }
        return batch;
    }

    public void assertStage(SelectionBatch batch, BatchStatus... allowed) {
        boolean matched = Arrays.stream(allowed).anyMatch(s -> s == batch.getStatus());
        if (!matched) {
            String allowText = Arrays.stream(allowed).map(BatchStatus::getLabel).collect(Collectors.joining("、"));
            throw new BusinessException(ErrorCode.STAGE_NOT_ALLOWED,
                    "当前阶段为「" + batch.getStatus().getLabel() + "」，仅允许在「" + allowText + "」阶段操作");
        }
    }

    public void assertIntentWindow(SelectionBatch batch) {
        assertStage(batch, BatchStatus.INTENT_OPEN);
        assertWindow(batch.getIntentStart(), batch.getIntentEnd(), "意向填报");
    }

    public void assertWindow(LocalDateTime start, LocalDateTime end, String label) {
        if (!enforceTimeWindow) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (start != null && now.isBefore(start)) {
            throw new BusinessException(ErrorCode.TIME_WINDOW_CLOSED,
                    label + "尚未开始，开放时间为 " + start);
        }
        if (end != null && now.isAfter(end)) {
            throw new BusinessException(ErrorCode.TIME_WINDOW_CLOSED,
                    label + "已于 " + end + " 截止");
        }
    }

    /** 公示发布后至撤回前，禁止改动录取结果。 */
    public void assertNotLocked(Long batchId) {
        Publicity publicity = publicityMapper.findCurrentByBatch(batchId);
        if (publicity != null
                && (publicity.getStatus() == PublicityStatus.PUBLISHED
                || publicity.getStatus() == PublicityStatus.CLOSED)) {
            throw new BusinessException(ErrorCode.BATCH_LOCKED);
        }
    }

    public boolean timeCheckEnabled() {
        return enforceTimeWindow;
    }
}
