package com.example.jejugilmoa.global.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serial;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DetailCommonItem(
        String contentid,
        String overview
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
