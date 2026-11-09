package com.labselection.domain.enums;

public enum InterviewConclusion {

    PENDING("待考核"),
    PASS("通过"),
    FAIL("不通过");

    private final String label;

    InterviewConclusion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
