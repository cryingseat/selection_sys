package com.labselection.domain.enums;

public enum SupplementStatus {

    SCHEDULED("未开始"),
    OPEN("进行中"),
    CLOSED("已结束");

    private final String label;

    SupplementStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
