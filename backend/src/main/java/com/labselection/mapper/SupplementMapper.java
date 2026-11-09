package com.labselection.mapper;

import com.labselection.domain.entity.SupplementIntent;
import com.labselection.domain.entity.SupplementRound;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SupplementMapper {
    int insertRound(SupplementRound round);

    SupplementRound findRoundById(@Param("id") Long id);

    SupplementRound findRoundByBatchAndNo(@Param("batchId") Long batchId, @Param("roundNo") int roundNo);

    List<SupplementRound> findRoundsByBatch(@Param("batchId") Long batchId);

    SupplementRound findOpenRound(@Param("batchId") Long batchId);

    int updateRoundStatus(@Param("id") Long id,
                          @Param("status") String status,
                          @Param("startAt") LocalDateTime startAt,
                          @Param("endAt") LocalDateTime endAt);

    int insertIntent(SupplementIntent intent);

    SupplementIntent findIntentById(@Param("id") Long id);

    SupplementIntent findIntentByRoundStudentMentor(@Param("roundId") Long roundId,
                                                    @Param("studentId") Long studentId,
                                                    @Param("mentorId") Long mentorId);

    List<SupplementIntent> findIntentsByRound(@Param("roundId") Long roundId);

    List<SupplementIntent> findIntentsByStudent(@Param("roundId") Long roundId,
                                                @Param("studentId") Long studentId);

    List<SupplementIntent> findIntentsByMentor(@Param("mentorId") Long mentorId,
                                               @Param("batchId") Long batchId,
                                               @Param("status") String status);

    int updateIntentStatus(@Param("id") Long id,
                           @Param("status") String status,
                           @Param("waitlistRank") Integer waitlistRank,
                           @Param("comment") String comment,
                           @Param("decidedBy") Long decidedBy,
                           @Param("decidedAt") LocalDateTime decidedAt);

    int deleteIntentsByRoundAndStudent(@Param("roundId") Long roundId, @Param("studentId") Long studentId);

    long countIntentsByRound(@Param("roundId") Long roundId);

    long countRoundsByBatch(@Param("batchId") Long batchId);
}
