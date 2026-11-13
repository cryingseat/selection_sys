package com.labselection.mapper;

import com.labselection.domain.entity.College;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CollegeMapper {
    List<College> findAll();

    College findById(@Param("id") Long id);

    College findByCode(@Param("code") String code);
}
