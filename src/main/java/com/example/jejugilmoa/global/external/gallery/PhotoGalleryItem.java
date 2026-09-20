package com.example.jejugilmoa.global.external.gallery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serial;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PhotoGalleryItem(
        String orgImage,
        String koTitle,
        String koCmanNm,
        String koFilmst
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
