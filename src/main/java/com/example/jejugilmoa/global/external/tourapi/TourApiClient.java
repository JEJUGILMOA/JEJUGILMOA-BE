package com.example.jejugilmoa.global.external.tourapi;

import com.example.jejugilmoa.global.external.config.ExternalApiProperties;
import com.example.jejugilmoa.global.external.tourapi.dto.TourApiResponse;
import com.example.jejugilmoa.global.external.tourapi.dto.TourListItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class TourApiClient {

    private final RestClient restClient;
    private final String serviceKey;
    private final String baseUrl;

    private static final String MOBILE_OS = "AND";
    private static final String MOBILE_APP = "JejuGilmoa";
    private static final String AREA_CODE_JEJU = "50";

    public static final String SIGNGU_JEJU_SI = "50110";   // 제주시
    public static final String SIGNGU_SEOGWIPO = "50130";  // 서귀포시

    @Autowired
    public TourApiClient(ExternalApiProperties props) {
        this.serviceKey = props.tourApi().serviceKey();
        this.baseUrl = props.tourApi().baseUrl();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        this.restClient = RestClient.builder()
            .requestFactory(factory)
            .build();
    }

    TourApiClient(ExternalApiProperties props, RestClient.Builder builder) {
        this.serviceKey = props.tourApi().serviceKey();
        this.baseUrl = props.tourApi().baseUrl();
        this.restClient = builder.build();
    }

    /**
     * 지역기반 관광지별 연관관광지 목록 조회 (areaBasedList1)
     * baseYm은 현재 연월로 자동 설정
     */
    public List<TourListItem> getAreaBased(String signguCd, int pageNo, int numOfRows) {
        return getAreaBased(currentBaseYm(), signguCd, pageNo, numOfRows);
    }

    public List<TourListItem> getAreaBased(String baseYm, String signguCd, int pageNo, int numOfRows) {
        var uri = UriComponentsBuilder.fromUriString(baseUrl + "/areaBasedList1")
            .queryParam("serviceKey", serviceKey)
            .queryParam("MobileOS", MOBILE_OS)
            .queryParam("MobileApp", MOBILE_APP)
            .queryParam("_type", "json")
            .queryParam("baseYm", baseYm)
            .queryParam("areaCd", AREA_CODE_JEJU)
            .queryParam("signguCd", signguCd)
            .queryParam("pageNo", pageNo)
            .queryParam("numOfRows", numOfRows)
            .build().toUriString();

        return fetchItems(uri, new ParameterizedTypeReference<TourApiResponse<TourListItem>>() {}, "지역기반 연관관광지");
    }

    /**
     * 키워드 검색 관광지별 연관관광지 정보 조회 (searchKeyword1)
     * baseYm은 현재 연월로 자동 설정
     */
    public List<TourListItem> searchByKeyword(String keyword, String signguCd, int pageNo, int numOfRows) {
        return searchByKeyword(keyword, currentBaseYm(), signguCd, pageNo, numOfRows);
    }

    public List<TourListItem> searchByKeyword(String keyword, String baseYm, String signguCd, int pageNo, int numOfRows) {
        var uri = UriComponentsBuilder.fromUriString(baseUrl + "/searchKeyword1")
            .queryParam("serviceKey", serviceKey)
            .queryParam("MobileOS", MOBILE_OS)
            .queryParam("MobileApp", MOBILE_APP)
            .queryParam("_type", "json")
            .queryParam("baseYm", baseYm)
            .queryParam("areaCd", AREA_CODE_JEJU)
            .queryParam("signguCd", signguCd)
            .queryParam("keyword", keyword)
            .queryParam("pageNo", pageNo)
            .queryParam("numOfRows", numOfRows)
            .build().toUriString();

        return fetchItems(uri, new ParameterizedTypeReference<TourApiResponse<TourListItem>>() {}, "키워드 연관관광지");
    }

    private static String currentBaseYm() {
        return YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private <T> List<T> fetchItems(String uri, ParameterizedTypeReference<TourApiResponse<T>> type, String apiName) {
        try {
            var response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(type);

            if (response == null || !response.isSuccess()) {
                log.warn("TourAPI 응답 실패: api={}", apiName);
                return List.of();
            }
            return response.items();
        } catch (Exception e) {
            log.error("TourAPI 호출 오류: api={}", apiName, e);
            return List.of();
        }
    }
}
