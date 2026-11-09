package com.labselection.mapper;

import com.labselection.domain.entity.Publicity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PublicityMapper {
    int insert(Publicity publicity);

    Publicity findById(@Param("id") Long id);

    Publicity findDetailById(@Param("id") Long id);

    Publicity findCurrentByBatch(@Param("batchId") Long batchId);

    Publicity findPublishedOrClosed(@Param("batchId") Long batchId);

    List<Publicity> findByBatch(@Param("batchId") Long batchId);

    int updateDraft(@Param("id") Long id,
                    @Param("title") String title,
                    @Param("content") String content,
                    @Param("startAt") LocalDateTime startAt,
                    @Param("endAt") LocalDateTime endAt);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("publishedBy") Long publishedBy,
                     @Param("publishedAt") LocalDateTime publishedAt,
                     @Param("closedAt") LocalDateTime closedAt);

    Publicity lockById(@Param("id") Long id);
}
