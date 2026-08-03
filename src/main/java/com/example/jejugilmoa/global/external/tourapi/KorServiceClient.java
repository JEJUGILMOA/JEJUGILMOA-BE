package com.example.jejugilmoa.global.external.tourapi;

import com.example.jejugilmoa.global.external.config.ExternalApiProperties;
import com.example.jejugilmoa.global.external.tourapi.dto.LocationBasedItem;
import com.example.jejugilmoa.global.external.tourapi.dto.TourApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class KorServiceClient {

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2";
    private static final String MOBILE_OS = "AND";
    private static final String MOBILE_APP = "JejuGilmoa";

    private final RestClient restClient;
    private final String serviceKey;

    @Autowired
    public KorServiceClient(ExternalApiProperties props) {
        this.serviceKey = props.tourApi().serviceKey();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * 위치기반 관광정보 조회 (locationBasedList1)
     * mapX=경도, mapY=위도 순서 (ADR-0002 동일 규칙)
     */
    public List<LocationBasedItem> locationBasedList1(double lat, double lng, int radiusMeters, int numOfRows) {
        String uri = UriComponentsBuilder.fromUriString(BASE_URL + "/locationBasedList2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("mapX", lng)
                .queryParam("mapY", lat)
                .queryParam("radius", radiusMeters)
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", 1)
                .build().toUriString();

        TourApiResponse<LocationBasedItem> response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            throw new TourApiException("KorService2 locationBasedList2 호출 오류", e);
        }

        if (response == null || !response.isSuccess()) {
            return List.of();
        }
        return response.items();
    }
}
