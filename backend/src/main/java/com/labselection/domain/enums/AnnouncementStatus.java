package com.labselection.domain.enums;

public enum AnnouncementStatus {

    DRAFT("草稿"),
    PUBLISHED("已发布"),
    CLOSED("已关闭");

    private final String label;

    AnnouncementStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
