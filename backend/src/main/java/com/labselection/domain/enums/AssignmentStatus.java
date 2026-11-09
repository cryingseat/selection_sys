package com.labselection.domain.enums;

public enum AssignmentStatus {

    CONFIRMED("已确认"),
    PUBLICIZED("已公示"),
    LOCKED("已锁定"),
    REVOKED("已撤回");

    private final String label;

    AssignmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
