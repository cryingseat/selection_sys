package com.labselection.domain.enums;

public enum AdjustmentPreference {

    INTERNAL("仅接受实训室内调剂"),
    CROSS("接受跨实训室调剂"),
    NONE("不接受调剂");

    private final String label;

    AdjustmentPreference(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
