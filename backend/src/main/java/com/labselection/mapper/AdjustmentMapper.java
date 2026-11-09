package com.labselection.mapper;

import com.labselection.domain.entity.AdjustmentRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdjustmentMapper {
    int insert(AdjustmentRecord record);

    AdjustmentRecord findById(@Param("id") Long id);

    List<AdjustmentRecord> findByBatch(@Param("batchId") Long batchId);

    List<AdjustmentRecord> findByStudent(@Param("batchId") Long batchId, @Param("studentId") Long studentId);

    long countByBatch(@Param("batchId") Long batchId);
}
