package com.labselection.domain.enums;

public enum SupplementIntentStatus {

    PENDING("待处理"),
    OFFERED("已拟录取"),
    WAITLISTED("候补"),
    REJECTED("未录取"),
    CONFIRMED("已确认"),
    DECLINED("已放弃");

    private final String label;

    SupplementIntentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
