package com.labselection.domain.enums;

public enum BatchStatus {

    DRAFT("草稿"),
    ANNOUNCED("宣讲与开放日"),
    INTENT_OPEN("意向填报"),
    INTENT_CLOSED("填报截止"),
    INTERVIEW("面试与初选"),
    CONFIRM("拟录取确认"),
    SUPPLEMENT("分轮补录"),
    ADJUSTMENT("统筹调剂"),
    PUBLICITY("结果公示"),
    FINISHED("已完成");

    private final String label;

    BatchStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
