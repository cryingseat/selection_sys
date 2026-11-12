package com.labselection.mapper;

import com.labselection.domain.entity.Mentor;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MentorMapper {
    Mentor findById(@Param("id") Long id);

    Mentor findByUserId(@Param("userId") Long userId);

    List<Mentor> findAll();

    List<Mentor> findByLabId(@Param("labId") Long labId);

    List<Mentor> search(@Param("keyword") String keyword,
                        @Param("labId") Long labId,
                        @Param("enabled") Boolean enabled,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword,
                     @Param("labId") Long labId,
                     @Param("enabled") Boolean enabled);

    List<Mentor> listWithStats(@Param("batchId") Long batchId,
                               @Param("collegeId") Long collegeId,
                               @Param("labId") Long labId);

    int insert(Mentor mentor);

    int update(Mentor mentor);

    int updateQuota(@Param("id") Long id, @Param("quota") int quota);

    Mentor lockById(@Param("id") Long id);

    long count();
}
