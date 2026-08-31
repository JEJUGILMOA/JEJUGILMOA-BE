package com.example.jejugilmoa.global.external.tats;

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
public class TatsCnctrRateClient {

    private static final String MOBILE_OS = "AND";
    private static final String MOBILE_APP = "JejuGilmoa";

    private final RestClient restClient;
    private final TatsCnctrRateProperties props;

    @Autowired
    public TatsCnctrRateClient(TatsCnctrRateProperties props) {
        this.props = props;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * 관광지 집중률 방문자 추이 예측 목록 조회 (tatsCnctrRatedList)
     * areaCd + signguCd 단위로 조회. 결과는 향후 30일간 일별 예측값 목록.
     */
    public List<TatsCnctrRateItem> tatsCnctrRatedList(String signguCd, int numOfRows) {
        if (props.serviceKey() == null || props.serviceKey().isBlank()) {
            log.warn("TatsCnctrRateService API 키 미설정 — TODAY_PICK 동기화 건너뜀");
            return List.of();
        }

        String uri = UriComponentsBuilder.fromUriString(props.baseUrl() + "/tatsCnctrRatedList")
                .queryParam("serviceKey", props.serviceKey())
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("areaCd", props.areaCd())
                .queryParam("signguCd", signguCd)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", numOfRows)
                .build().toUriString();

        try {
            TourApiResponse<TatsCnctrRateItem> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !response.isSuccess()) {
                log.warn("TatsCnctrRateService 응답 실패: signguCd={}", signguCd);
                return List.of();
            }
            return response.items();
        } catch (Exception e) {
            log.error("TatsCnctrRateService 호출 오류: signguCd={}", signguCd, e);
            return List.of();
        }
    }
}
