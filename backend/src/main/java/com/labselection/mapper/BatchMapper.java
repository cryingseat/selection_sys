package com.labselection.mapper;

import com.labselection.domain.entity.SelectionBatch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BatchMapper {
    SelectionBatch findById(@Param("id") Long id);

    SelectionBatch findCurrent();

    List<SelectionBatch> findAll();

    List<SelectionBatch> search(@Param("status") String status,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    long countSearch(@Param("status") String status);

    int insert(SelectionBatch batch);

    int update(SelectionBatch batch);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    long count();
}
