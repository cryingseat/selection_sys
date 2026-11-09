package com.labselection.mapper;

import com.labselection.domain.entity.Offer;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OfferMapper {
    int insert(Offer offer);

    Offer findById(@Param("id") Long id);

    Offer findDetailById(@Param("id") Long id);

    List<Offer> findByStudent(@Param("batchId") Long batchId, @Param("studentId") Long studentId);

    List<Offer> findByMentor(@Param("batchId") Long batchId, @Param("mentorId") Long mentorId);

    List<Offer> findByBatch(@Param("batchId") Long batchId, @Param("status") String status);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("confirmedAt") LocalDateTime confirmedAt);

    int releaseOtherOffers(@Param("studentId") Long studentId,
                           @Param("batchId") Long batchId,
                           @Param("keepOfferId") Long keepOfferId);

    int expireOffersOfStudent(@Param("studentId") Long studentId, @Param("batchId") Long batchId);

    long countActiveByMentor(@Param("mentorId") Long mentorId, @Param("batchId") Long batchId);

    long countActiveByLab(@Param("labId") Long labId, @Param("batchId") Long batchId);

    long countByBatch(@Param("batchId") Long batchId, @Param("status") String status);

    Offer lockById(@Param("id") Long id);
}
