package com.labselection.domain.enums;

public enum InterviewStatus {

    SCHEDULED("待面试"),
    COMPLETED("已完成"),
    CANCELED("已取消");

    private final String label;

    InterviewStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
