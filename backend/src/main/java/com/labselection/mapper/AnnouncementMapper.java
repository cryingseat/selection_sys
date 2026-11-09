package com.labselection.mapper;

import com.labselection.domain.entity.Announcement;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AnnouncementMapper {
    int insert(Announcement announcement);

    int update(Announcement announcement);

    Announcement findById(@Param("id") Long id);

    List<Announcement> findPublic(@Param("limit") int limit);

    List<Announcement> findByBatch(@Param("batchId") Long batchId);

    List<Announcement> findAll();

    int deleteById(@Param("id") Long id);
}
