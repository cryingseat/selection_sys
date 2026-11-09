package com.labselection.mapper;

import com.labselection.domain.entity.AuthToken;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface TokenMapper {
    int insert(AuthToken token);

    AuthToken findValid(@Param("hash") String hash, @Param("now") LocalDateTime now);

    int revoke(@Param("hash") String hash);

    int touch(@Param("id") Long id);

    int deleteExpired(@Param("now") LocalDateTime now);
}
