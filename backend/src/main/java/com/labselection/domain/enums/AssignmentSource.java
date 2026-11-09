package com.labselection.domain.enums;

public enum AssignmentSource {

    CONFIRM("首轮确认"),
    SUPPLEMENT("补录确认"),
    PROMOTION("候补递补"),
    INTERNAL_ADJUST("实训室内调剂"),
    CROSS_ADJUST("跨实训室调剂"),
    TALK("谈话安置");

    private final String label;

    AssignmentSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
