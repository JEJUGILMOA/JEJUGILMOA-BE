package com.example.jejugilmoa.global.external.navermap;

import com.example.jejugilmoa.global.external.config.ExternalApiProperties;
import com.example.jejugilmoa.global.external.navermap.dto.NaverDirectionsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NaverDirectionsClient {

    private static final String DRIVING_PATH = "/map-direction/v1/driving";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String baseUrl;

    @Autowired
    public NaverDirectionsClient(ExternalApiProperties props) {
        this.clientId = props.naverMap().clientId();
        this.clientSecret = props.naverMap().clientSecret();
        this.baseUrl = props.naverMap().baseUrl();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        this.restClient = RestClient.builder()
            .requestFactory(factory)
            .build();
    }

    NaverDirectionsClient(ExternalApiProperties props, RestClient.Builder builder) {
        this.clientId = props.naverMap().clientId();
        this.clientSecret = props.naverMap().clientSecret();
        this.baseUrl = props.naverMap().baseUrl();
        this.restClient = builder.build();
    }

    /**
     * 자동차 길찾기 (Directions 5 driving).
     * start/goal/waypoints 좌표는 네이버 규격인 "경도,위도" 순서 문자열.
     * waypoints는 '|' 구분 (최대 5개), 없으면 null.
     */
    public NaverDirectionsResponse getDriving(String start, String goal, String waypoints, String option) {
        var builder = UriComponentsBuilder.fromUriString(baseUrl + DRIVING_PATH)
            .queryParam("start", start)
            .queryParam("goal", goal)
            .queryParam("option", option);
        if (StringUtils.hasText(waypoints)) {
            builder.queryParam("waypoints", waypoints);
        }
        String uri = builder.build().toUriString();

        NaverDirectionsResponse response;
        try {
            response = restClient.get()
                .uri(uri)
                .header("x-ncp-apigw-api-key-id", clientId)
                .header("x-ncp-apigw-api-key", clientSecret)
                .retrieve()
                .body(NaverDirectionsResponse.class);
        } catch (Exception e) {
            throw new NaverMapException("네이버 지도 Directions API 호출 오류", e);
        }

        if (response == null) {
            throw new NaverMapException("네이버 지도 Directions API 응답 없음");
        }
        return response;
    }
}
