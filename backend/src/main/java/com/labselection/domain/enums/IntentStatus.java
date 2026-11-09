package com.labselection.domain.enums;

public enum IntentStatus {

    PENDING("待处理"),
    OFFERED("已拟录取"),
    WAITLISTED("候补"),
    REJECTED("未录取"),
    CONFIRMED("已确认"),
    RELEASED("已释放"),
    DECLINED("已放弃");

    private final String label;

    IntentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
