package com.labselection.mapper;

import com.labselection.domain.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {
    SysUser findById(@Param("id") Long id);

    SysUser findByAccount(@Param("account") String account);

    List<SysUser> findByIds(@Param("ids") List<Long> ids);

    int insert(SysUser user);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);

    int updateDisplayName(@Param("id") Long id, @Param("displayName") String displayName);

    long count();
}
