package com.labselection.mapper;

import com.labselection.domain.entity.ApplicationIntent;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IntentMapper {
    ApplicationIntent findById(@Param("id") Long id);

    ApplicationIntent findDetailById(@Param("id") Long id);

    List<ApplicationIntent> findByApplicationId(@Param("applicationId") Long applicationId);

    List<ApplicationIntent> findByBatch(@Param("batchId") Long batchId);

    List<ApplicationIntent> findByStudentAndBatch(@Param("studentId") Long studentId, @Param("batchId") Long batchId);

    List<ApplicationIntent> findByMentorAndBatch(@Param("mentorId") Long mentorId,
                                                 @Param("batchId") Long batchId,
                                                 @Param("status") String status,
                                                 @Param("keyword") String keyword,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    long countByMentorAndBatch(@Param("mentorId") Long mentorId,
                               @Param("batchId") Long batchId,
                               @Param("status") String status,
                               @Param("keyword") String keyword);

    List<ApplicationIntent> findWaitlistByMentor(@Param("mentorId") Long mentorId, @Param("batchId") Long batchId);

    int insert(ApplicationIntent intent);

    int deleteByApplicationId(@Param("applicationId") Long applicationId);

    int updateDecision(@Param("id") Long id,
                       @Param("status") String status,
                       @Param("waitlistRank") Integer waitlistRank,
                       @Param("comment") String comment,
                       @Param("decidedBy") Long decidedBy,
                       @Param("decidedAt") LocalDateTime decidedAt);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateWaitlistRank(@Param("id") Long id, @Param("waitlistRank") Integer waitlistRank);

    int updateStatusByApplication(@Param("applicationId") Long applicationId, @Param("status") String status);

    long countByMentorAndStatus(@Param("mentorId") Long mentorId,
                                @Param("batchId") Long batchId,
                                @Param("status") String status);

    long countPendingByMentor(@Param("mentorId") Long mentorId, @Param("batchId") Long batchId);

    long countByBatch(@Param("batchId") Long batchId);
}
