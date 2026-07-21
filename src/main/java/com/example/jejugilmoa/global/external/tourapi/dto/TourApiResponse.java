package com.example.jejugilmoa.global.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiResponse<T>(Response<T> response) {

    public boolean isSuccess() {
        return response != null
            && response.header() != null
            && "0000".equals(response.header().resultCode());
    }

    public List<T> items() {
        if (response == null || response.body() == null || response.body().items() == null) {
            return List.of();
        }
        List<T> item = response.body().items().item();
        return item != null ? item : List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response<T>(Header header, Body<T> body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body<T>(Items<T> items, int totalCount, int numOfRows, int pageNo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items<T>(List<T> item) {}
}
