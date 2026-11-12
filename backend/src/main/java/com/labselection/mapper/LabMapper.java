package com.labselection.mapper;

import com.labselection.domain.entity.Lab;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LabMapper {
    Lab findById(@Param("id") Long id);

    Lab findByCode(@Param("code") String code);

    List<Lab> findAll();

    List<Lab> findActive(@Param("collegeId") Long collegeId);

    List<Lab> search(@Param("keyword") String keyword,
                     @Param("status") String status,
                     @Param("collegeId") Long collegeId,
                     @Param("offset") int offset,
                     @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword,
                     @Param("status") String status,
                     @Param("collegeId") Long collegeId);

    List<Lab> listWithStats(@Param("batchId") Long batchId, @Param("collegeId") Long collegeId);

    int insert(Lab lab);

    int update(Lab lab);

    Lab lockById(@Param("id") Long id);

    long count();
}
