package com.labselection.domain.enums;

public enum ApplicationStatus {

    DRAFT("草稿"),
    SUBMITTED("已提交"),
    WITHDRAWN("已撤回"),
    LOCKED("已锁定");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
