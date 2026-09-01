package com.example.jejugilmoa.global.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DetailImageItem(
        String contentid,
        String originimgurl,
        String smallimageurl,
        String imgname,
        String serialnum
) {}
