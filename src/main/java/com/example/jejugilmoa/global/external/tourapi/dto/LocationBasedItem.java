package com.example.jejugilmoa.global.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LocationBasedItem(
        String contentid,
        String contenttypeid,
        String title,
        String addr1,
        String firstimage,
        String mapx,
        String mapy,
        String dist
) {}
