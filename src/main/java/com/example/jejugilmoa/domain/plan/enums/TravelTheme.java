package com.example.jejugilmoa.domain.plan.enums;

public enum TravelTheme {
    FOOD    ("음식",  39),
    NATURE  ("자연",  12),
    ACTIVITY("체험",  28),
    CAFE    ("카페",  null),
    CULTURE ("역사",  14),
    SHOPPING("쇼핑",  38),
    FESTIVAL("축제",  15);

    private final String categoryName;
    private final Integer contentTypeId;

    TravelTheme(String categoryName, Integer contentTypeId) {
        this.categoryName = categoryName;
        this.contentTypeId = contentTypeId;
    }

    public String getCategoryName() { return categoryName; }
    public Integer getContentTypeId() { return contentTypeId; }
    public boolean hasDbCategory() { return categoryName != null; }
    public boolean hasTourApiType() { return contentTypeId != null; }
}
