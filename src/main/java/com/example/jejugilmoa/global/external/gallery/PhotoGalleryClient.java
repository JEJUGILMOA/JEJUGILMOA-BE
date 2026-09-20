package com.example.jejugilmoa.global.external.gallery;

import com.example.jejugilmoa.global.external.config.ExternalApiProperties;
import com.example.jejugilmoa.global.external.tourapi.dto.TourApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Slf4j
@Component
public class PhotoGalleryClient {

    public record GallerySearchResult(List<PhotoGalleryItem> items, int totalCount) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/PhokoAwrdService";
    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "JejuGilmoa";

    private final RestClient restClient;
    private final String serviceKey;

    @Autowired
    public PhotoGalleryClient(ExternalApiProperties props) {
        this.serviceKey = props.tourApi().serviceKey();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * 관광사진공모전 수상작 조회 (phokoAwrdList).
     * lDongRegnCd=50, numOfRows=1 고정. pageNo는 호출자가 지정.
     * 결과는 pageNo별로 1일간 Redis 캐시. null 반환(API 실패) 시 미캐시.
     */
    @Nullable
    public GallerySearchResult searchJejuPhoto(int pageNo) {
        String uri = UriComponentsBuilder.fromUriString(BASE_URL + "/phokoAwrdList")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("numOfRows", 1)
                .queryParam("pageNo", pageNo)
                .queryParam("lDongRegnCd", 50)
                .build()
                .toUriString();

        try {
            TourApiResponse<PhotoGalleryItem> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.isSuccess()) {
                log.warn("PhotoGallery API 응답 실패: pageNo={}", pageNo);
                return null;
            }

            int totalCount = response.response() != null && response.response().body() != null
                    ? response.response().body().totalCount() : 0;

            return new GallerySearchResult(response.items(), totalCount);
        } catch (Exception e) {
            log.error("PhotoGallery API 호출 오류: pageNo={}", pageNo, e);
            return null;
        }
    }
}
