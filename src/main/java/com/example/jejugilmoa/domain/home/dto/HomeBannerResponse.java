package com.example.jejugilmoa.domain.home.dto;

public record HomeBannerResponse(
        String imageUrl,
        String title,
        String photographer,
        String location,
        int totalCount
) {}
