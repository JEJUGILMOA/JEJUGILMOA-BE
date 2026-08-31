package com.example.jejugilmoa.global.external.tats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TatsCnctrRateItem(
        String baseYmd,    // 기준 연월일 (YYYYMMDD)
        String areaCd,
        String areaNm,
        String signguCd,
        String signguNm,
        String tAtsNm,     // 관광지명 — Place.name과 매칭
        String cnctrRate   // 집중률 (0~100 String)
) {}
