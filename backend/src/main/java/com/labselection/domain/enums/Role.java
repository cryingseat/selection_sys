package com.labselection.domain.enums;

public enum Role {

    STUDENT("学生"),
    MENTOR("导师"),
    ADMIN("学院管理员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
