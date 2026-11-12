package com.labselection.mapper;

import com.labselection.domain.entity.Application;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApplicationMapper {
    Application findById(@Param("id") Long id);

    Application findDetailById(@Param("id") Long id);

    Application findByBatchAndStudent(@Param("batchId") Long batchId, @Param("studentId") Long studentId);

    List<Application> findByStudent(@Param("studentId") Long studentId);

    List<Application> findByBatchWithStudent(@Param("batchId") Long batchId,
                                             @Param("collegeId") Long collegeId);

    List<Application> search(@Param("batchId") Long batchId,
                             @Param("status") String status,
                             @Param("keyword") String keyword,
                             @Param("collegeId") Long collegeId,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    long countSearch(@Param("batchId") Long batchId,
                     @Param("status") String status,
                     @Param("keyword") String keyword,
                     @Param("collegeId") Long collegeId);

    List<Application> listUnadmitted(@Param("batchId") Long batchId,
                                     @Param("collegeId") Long collegeId);

    int insert(Application application);

    int update(Application application);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("submittedAt") LocalDateTime submittedAt);

    Application lockById(@Param("id") Long id);

    long countByBatch(@Param("batchId") Long batchId);

    long countSubmittedByBatch(@Param("batchId") Long batchId);
}
