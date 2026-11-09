package com.labselection.service;

import com.labselection.auth.TokenService;
import com.labselection.domain.entity.SelectionBatch;
import com.labselection.domain.entity.SupplementRound;
import com.labselection.domain.enums.BatchStatus;
import com.labselection.domain.enums.SupplementStatus;
import com.labselection.mapper.BatchMapper;
import com.labselection.mapper.SupplementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 仅在 app.enforce-time-window=true（生产）时自动处理逾期；
 * 演示环境保持手动，方便逐步展示各阶段。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionScheduler {

    private final BatchMapper batchMapper;
    private final SupplementMapper supplementMapper;
    private final SelectionService selectionService;
    private final SupplementService supplementService;
    private final TokenService tokenService;

    @Value("${app.enforce-time-window:false}")
    private boolean enforceTimeWindow;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void expireDeadlines() {
        tokenService.cleanExpired();
        if (!enforceTimeWindow) {
            return;
        }
        SelectionBatch batch = batchMapper.findCurrent();
        if (batch == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (batch.getStatus() == BatchStatus.CONFIRM
                && batch.getConfirmEnd() != null && now.isAfter(batch.getConfirmEnd())) {
            int count = selectionService.expirePendingOffers(batch.getId(),
                    "确认截止时间已到，系统按放弃处理");
            if (count > 0) {
                log.info("批次 {} 自动处理逾期拟录取 {} 条", batch.getId(), count);
            }
        }
        if (batch.getStatus() == BatchStatus.SUPPLEMENT) {
            SupplementRound round = supplementMapper.findOpenRound(batch.getId());
            if (round != null && round.getStatus() == SupplementStatus.OPEN
                    && round.getEndAt() != null && now.isAfter(round.getEndAt())) {
                try {
                    supplementService.closeRound(round.getId());
                    log.info("补录轮次 {} 到期自动结束", round.getId());
                } catch (Exception ex) {
                    log.warn("补录轮次自动结束失败: {}", ex.getMessage());
                }
            }
        }
    }
}
