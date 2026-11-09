package com.labselection.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static List<Long> parseIds(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        List<Long> ids = new ArrayList<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    try {
                        ids.add(Long.parseLong(s));
                    } catch (NumberFormatException ignored) {
                        // 忽略非法片段，避免脏数据影响页面
                    }
                });
        return ids;
    }

    public static String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream().distinct().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(null);
    }
}
