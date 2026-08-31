package com.example.jejugilmoa.global.external.tourapi;

import com.example.jejugilmoa.global.external.config.ExternalApiProperties;
import com.example.jejugilmoa.global.external.tourapi.dto.AreaBasedItem;
import com.example.jejugilmoa.global.external.tourapi.dto.DetailCommonItem;
import com.example.jejugilmoa.global.external.tourapi.dto.LocationBasedItem;
import com.example.jejugilmoa.global.external.tourapi.dto.TourApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
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
     * 위치기반 관광정보 조회 (locationBasedList2)
     * mapX=경도, mapY=위도 순서 (ADR-0002 동일 규칙)
     * contentTypeId null이면 전체 유형 조회.
     */
    public List<LocationBasedItem> locationBasedList2(
            double lat, double lng, int radiusMeters, int numOfRows, Integer contentTypeId) {
        var builder = UriComponentsBuilder.fromUriString(BASE_URL + "/locationBasedList2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("mapX", lng)
                .queryParam("mapY", lat)
                .queryParam("radius", radiusMeters)
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", 1);
        if (contentTypeId != null) {
            builder.queryParam("contentTypeId", contentTypeId);
        }
        String uri = builder.build().toUriString();

        TourApiResponse<LocationBasedItem> response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            throw new TourApiException("locationBasedList2 호출 오류", e);
        }

        if (response == null || !response.isSuccess()) {
            return List.of();
        }
        return response.items();
    }

    /**
     * 지역 기반 관광정보 조회 (areaBasedList2, 추천순)
     * areaCode=39(제주도), arrange=Q(추천순)
     */
    public List<AreaBasedItem> areaBasedListByPopularity(int numOfRows) {
        String uri = UriComponentsBuilder.fromUriString(BASE_URL + "/areaBasedList2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("areaCode", 39)
                .queryParam("arrange", "Q")
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", 1)
                .build().toUriString();

        TourApiResponse<AreaBasedItem> response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            throw new TourApiException("areaBasedListByPopularity 호출 오류", e);
        }

        if (response == null || !response.isSuccess()) {
            throw new TourApiException("areaBasedListByPopularity 응답 실패");
        }
        return response.items();
    }

    /**
     * 공통 정보 조회 (detailCommon2) — overview 반환
     */
    public DetailCommonItem detailCommon2(String contentId) {
        String uri = UriComponentsBuilder.fromUriString(BASE_URL + "/detailCommon2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .queryParam("overviewYN", "Y")
                .queryParam("defaultYN", "Y")
                .build().toUriString();

        TourApiResponse<DetailCommonItem> response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.warn("detailCommon2 호출 오류: contentId={}", contentId, e);
            return null;
        }

        if (response == null || !response.isSuccess()) {
            return null;
        }
        List<DetailCommonItem> items = response.items();
        return items.isEmpty() ? null : items.get(0);
    }
}
