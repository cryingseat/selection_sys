package com.labselection.mapper;

import com.labselection.domain.entity.Interview;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InterviewMapper {
    Interview findById(@Param("id") Long id);

    Interview findDetailById(@Param("id") Long id);

    Interview findByApplicationAndLab(@Param("applicationId") Long applicationId, @Param("labId") Long labId);

    List<Interview> findByApplication(@Param("applicationId") Long applicationId);

    List<Interview> findByMentor(@Param("mentorId") Long mentorId, @Param("batchId") Long batchId);

    List<Interview> findByBatch(@Param("batchId") Long batchId);

    int insert(Interview interview);

    int update(Interview interview);

    int countCompletedByMentor(@Param("mentorId") Long mentorId, @Param("batchId") Long batchId);

    int existsCompleted(@Param("applicationId") Long applicationId, @Param("labId") Long labId);
}
