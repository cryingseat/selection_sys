package com.labselection.mapper;

import com.labselection.domain.entity.FinalAssignment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AssignmentMapper {
    int insert(FinalAssignment assignment);

    FinalAssignment findById(@Param("id") Long id);

    FinalAssignment findDetailById(@Param("id") Long id);

    FinalAssignment findByStudentAndBatch(@Param("batchId") Long batchId, @Param("studentId") Long studentId);

    List<FinalAssignment> findByBatch(@Param("batchId") Long batchId);

    List<FinalAssignment> findPublicByBatch(@Param("batchId") Long batchId);

    List<FinalAssignment> findByMentor(@Param("mentorId") Long mentorId, @Param("batchId") Long batchId);

    List<FinalAssignment> findByLab(@Param("labId") Long labId, @Param("batchId") Long batchId);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateStatusByBatch(@Param("batchId") Long batchId, @Param("status") String status);

    long countByMentor(@Param("mentorId") Long mentorId, @Param("batchId") Long batchId);

    long countByLab(@Param("labId") Long labId, @Param("batchId") Long batchId);

    long countByBatch(@Param("batchId") Long batchId);
}
