package com.labselection.mapper;

import com.labselection.domain.entity.Lab;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LabMapper {
    Lab findById(@Param("id") Long id);

    Lab findByCode(@Param("code") String code);

    List<Lab> findAll();

    List<Lab> findActive();

    List<Lab> search(@Param("keyword") String keyword,
                     @Param("status") String status,
                     @Param("offset") int offset,
                     @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword, @Param("status") String status);

    List<Lab> listWithStats(@Param("batchId") Long batchId);

    int insert(Lab lab);

    int update(Lab lab);

    Lab lockById(@Param("id") Long id);

    long count();
}
