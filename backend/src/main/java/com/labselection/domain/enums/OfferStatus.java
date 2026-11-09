package com.labselection.domain.enums;

public enum OfferStatus {

    OFFERED("待确认"),
    CONFIRMED("已确认"),
    DECLINED("已放弃"),
    RELEASED("已释放"),
    EXPIRED("已逾期");

    private final String label;

    OfferStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
