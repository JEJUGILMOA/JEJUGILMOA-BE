package com.example.jejugilmoa.global.external.tourapi;

import com.example.jejugilmoa.global.external.config.ExternalApiProperties;
import com.example.jejugilmoa.global.external.tourapi.dto.AreaBasedItem;
import com.example.jejugilmoa.global.external.tourapi.dto.DetailCommonItem;
import com.example.jejugilmoa.global.external.tourapi.dto.DetailImageItem;
import com.example.jejugilmoa.global.external.tourapi.dto.LocationBasedItem;
import com.example.jejugilmoa.global.external.tourapi.dto.TourApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class KorServiceClient {

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2";
    private static final String MOBILE_OS = "AND";
    private static final String MOBILE_APP = "JejuGilmoa";

    private final RestClient restClient;
    private final String serviceKey;
    private final ObjectMapper objectMapper;

    @Autowired
    public KorServiceClient(ExternalApiProperties props, ObjectMapper objectMapper) {
        this.serviceKey = props.tourApi().serviceKey();
        this.objectMapper = objectMapper;
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
    public List<AreaBasedItem> areaBasedListByPopularity(int numOfRows, int pageNo) {
        String uri = UriComponentsBuilder.fromUriString(BASE_URL + "/areaBasedList2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("areaCode", 39)
                .queryParam("arrange", "Q")
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
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
     * 공통 정보 조회 (detailCommon2) — overview 반환.
     * 429 응답 시 최대 2회 재시도 (1초 간격).
     */
    public DetailCommonItem detailCommon2(String contentId) {
        String uri = UriComponentsBuilder.fromUriString(BASE_URL + "/detailCommon2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .build().toUriString();

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String rawBody = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(String.class);
                if (rawBody == null || rawBody.isBlank()) {
                    return null;
                }

                TourApiResponse<DetailCommonItem> response;
                try {
                    response = objectMapper.readValue(rawBody,
                            objectMapper.getTypeFactory().constructParametricType(TourApiResponse.class, DetailCommonItem.class));
                } catch (Exception parseEx) {
                    return null;
                }

                if (response == null || !response.isSuccess()) {
                    return null;
                }
                List<DetailCommonItem> items = response.items();
                return items.isEmpty() ? null : items.get(0);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt < 2) {
                    log.warn("detailCommon2 rate limit 초과: contentId={}, 1초 후 재시도 ({}/2)", contentId, attempt + 1);
                    try { Thread.sleep(1_000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
                } else {
                    log.warn("detailCommon2 rate limit 초과 (재시도 소진): contentId={}", contentId);
                }
            } catch (Exception e) {
                log.warn("detailCommon2 호출 오류: contentId={}", contentId, e);
                return null;
            }
        }
        return null;
    }

    /**
     * 이미지 목록 조회 (detailImage2) — originimgurl 최대 3건 반환.
     * Optional.empty() = API 호출/파싱 실패 (일시적 오류 — imageEnriched 설정 금지)
     * Optional.of(empty) = API 성공이나 이미지 없음 (imageEnriched 설정 가능)
     */
    public Optional<List<String>> detailImage2(String contentId) {
        String uri = UriComponentsBuilder.fromUriString(BASE_URL + "/detailImage2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .queryParam("imageYN", "Y")
                .build().toUriString();

        try {
            String rawBody = restClient.get().uri(uri).retrieve().body(String.class);
            if (rawBody == null || rawBody.isBlank()) {
                log.warn("detailImage2 응답 빈 바디: contentId={}", contentId);
                return Optional.empty();
            }

            TourApiResponse<DetailImageItem> response = objectMapper.readValue(rawBody,
                    objectMapper.getTypeFactory().constructParametricType(TourApiResponse.class, DetailImageItem.class));

            if (response == null || !response.isSuccess()) {
                log.warn("detailImage2 응답 실패: contentId={}, body={}", contentId, rawBody.length() > 200 ? rawBody.substring(0, 200) : rawBody);
                return Optional.empty();
            }
            List<String> urls = response.items().stream()
                    .map(DetailImageItem::originimgurl)
                    .filter(url -> url != null && !url.isBlank())
                    .limit(3)
                    .toList();
            log.info("detailImage2 결과: contentId={}, 이미지 {}건", contentId, urls.size());
            return Optional.of(urls);
        } catch (Exception e) {
            log.warn("detailImage2 호출 오류: contentId={}", contentId, e);
            return Optional.empty();
        }
    }
}
