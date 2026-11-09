package com.labselection.domain.enums;

public enum AnnouncementType {

    GENERAL("综合通知"),
    LECTURE("集中宣讲"),
    OPEN_DAY("实训室开放日"),
    INTERVIEW("面试安排"),
    SUPPLEMENT("补录通知"),
    PUBLICITY("公示通知");

    private final String label;

    AnnouncementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
