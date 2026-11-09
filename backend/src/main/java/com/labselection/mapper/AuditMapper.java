package com.labselection.mapper;

import com.labselection.domain.entity.AuditLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AuditMapper {
    int insert(AuditLog log);

    List<AuditLog> search(@Param("batchId") Long batchId,
                          @Param("action") String action,
                          @Param("offset") int offset,
                          @Param("limit") int limit);

    long countSearch(@Param("batchId") Long batchId, @Param("action") String action);
}
