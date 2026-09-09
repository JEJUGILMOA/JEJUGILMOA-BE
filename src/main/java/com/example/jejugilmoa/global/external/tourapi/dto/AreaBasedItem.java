package com.example.jejugilmoa.global.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serial;
import java.io.Serializable;

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
        String cat2,         // 구분류체계 중분류 (A0xxx, B02xx) — 구버전 API
        String cat3,         // 구분류체계 소분류 — 구버전 API
        String lclsSystm1,   // 신분류체계 대분류 (NA, FD, SH, HS, EV, EX, LS, VE, AC)
        String lclsSystm2,   // 신분류체계 중분류 (FD05 등)
        String lclsSystm3    // 신분류체계 소분류
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
