package com.example.jejugilmoa.domain.map.dto;

import com.example.jejugilmoa.domain.map.enums.CongestionLevel;

import java.math.BigDecimal;

public record HeatmapCellDto(
    BigDecimal latitude,
    BigDecimal longitude,
    CongestionLevel level,
    double intensity
) {}
