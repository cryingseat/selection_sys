package com.labselection.domain.enums;

public enum LabStatus {

    ACTIVE("正常"),
    DISABLED("停用");

    private final String label;

    LabStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
