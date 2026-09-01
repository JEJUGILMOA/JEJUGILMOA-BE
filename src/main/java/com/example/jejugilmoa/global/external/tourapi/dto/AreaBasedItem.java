package com.example.jejugilmoa.global.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AreaBasedItem(
        String contentid,
        String contenttypeid,
        String title,
        String addr1,
        String mapx,
        String mapy,
        String firstimage,
        String areacode,
        String sigungucode,
        String cat2,  // 중분류코드 (clsSystem2)
        String cat3   // 소분류코드 (clsSystem3)
) {}
