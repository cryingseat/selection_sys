package com.labselection.domain.enums;

public enum PublicityStatus {

    DRAFT("草稿"),
    PUBLISHED("公示中"),
    CLOSED("已结束"),
    REVOKED("已撤回");

    private final String label;

    PublicityStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
