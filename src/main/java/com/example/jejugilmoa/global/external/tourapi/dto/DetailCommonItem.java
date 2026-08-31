package com.example.jejugilmoa.global.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DetailCommonItem(
        String contentid,
        String overview
) {}
