package com.labselection.domain.enums;

public enum OfferSource {

    INITIAL("首轮拟录取"),
    SUPPLEMENT("补录"),
    PROMOTION("候补递补"),
    ADJUSTMENT("调剂");

    private final String label;

    OfferSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
