package com.example.jejugilmoa.global.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourListItem(
    String tAtsCd,           // 관광지코드
    String tAtsNm,           // 관광지명
    String areaCd,           // 관광지 지역코드
    String areaNm,           // 관광지 지역명
    String signguCd,         // 관광지 시군구코드
    String signguNm,         // 관광지 시군구명
    String rlteTatsCd,       // 연관관광지코드
    String rlteTatsNm,       // 연관관광지명
    String rlteRegnCd,       // 연관관광지 지역코드
    String rlteRegnNm,       // 연관관광지 지역명
    String rlteSignguCd,     // 연관관광지 시군구코드
    String rlteSignguNm,     // 연관관광지 시군구명
    String rlteCtgryLclsNm, // 연관 카테고리 대분류 (관광지, 음식, 숙박)
    String rlteCtgryMclsNm, // 연관 카테고리 중분류
    String rlteCtgrySclsNm, // 연관 카테고리 소분류
    int rlteRank             // 연관순위 (1이 가장 높음)
) {}
