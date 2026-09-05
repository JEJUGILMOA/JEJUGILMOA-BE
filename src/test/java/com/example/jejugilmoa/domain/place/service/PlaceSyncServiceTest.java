package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.global.external.config.ExternalApiProperties;
import com.example.jejugilmoa.global.external.tourapi.TourApiClient;
import com.example.jejugilmoa.global.external.tourapi.TourApiException;
import com.example.jejugilmoa.global.external.tourapi.dto.TourApiResponse;
import com.example.jejugilmoa.global.external.tourapi.dto.TourListItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class PlaceSyncServiceTest {

    @Mock RestClient mockRestClient;
    @Mock RestClient.RequestHeadersUriSpec uriSpec;
    @Mock RestClient.ResponseSpec responseSpec;

    TourApiClient tourApiClient;

    @BeforeEach
    void setUp() {
        var props = new ExternalApiProperties(
            new ExternalApiProperties.ApiConfig("test-key", "https://api.test.com"),
            new ExternalApiProperties.ApiConfig("", ""),
            new ExternalApiProperties.NaverMapConfig("", "", "")
        );
        tourApiClient = new TourApiClient(props);
        ReflectionTestUtils.setField(tourApiClient, "restClient", mockRestClient);
    }

    private void givenApiReturns(TourApiResponse<TourListItem> response) {
        given(mockRestClient.get()).willReturn(uriSpec);
        given(uriSpec.uri(anyString())).willReturn(uriSpec);
        given(uriSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(any(ParameterizedTypeReference.class))).willReturn(response);
    }

    private void givenApiThrows(RuntimeException ex) {
        given(mockRestClient.get()).willReturn(uriSpec);
        given(uriSpec.uri(anyString())).willReturn(uriSpec);
        given(uriSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(any(ParameterizedTypeReference.class))).willThrow(ex);
    }

    @Test
    void getAreaBased_throwsOnNullResponse() {
        givenApiReturns(null);

        assertThatThrownBy(() -> tourApiClient.getAreaBased(TourApiClient.SIGNGU_JEJU_SI, 1, 10))
            .isInstanceOf(TourApiException.class)
            .hasMessageContaining("응답 없음");
    }

    @Test
    void getAreaBased_throwsOnFailureResponseWithResultMsg() {
        var header = new TourApiResponse.Header("9000", "SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
        var failResponse = new TourApiResponse<TourListItem>(new TourApiResponse.Response<>(header, null));
        givenApiReturns(failResponse);

        assertThatThrownBy(() -> tourApiClient.getAreaBased(TourApiClient.SIGNGU_JEJU_SI, 1, 10))
            .isInstanceOf(TourApiException.class)
            .hasMessageContaining("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
    }

    @Test
    void getAreaBased_throwsWrappingCauseOnHttpError() {
        var cause = new RuntimeException("연결 실패");
        givenApiThrows(cause);

        assertThatThrownBy(() -> tourApiClient.getAreaBased(TourApiClient.SIGNGU_JEJU_SI, 1, 10))
            .isInstanceOf(TourApiException.class)
            .hasCause(cause);
    }

    @Test
    void getAreaBased_returnsItemsOnSuccess() {
        var item = new TourListItem(
            "tAtsCd-001", "성산일출봉", "50", "제주특별자치도", "50110", "제주시",
            "rlteCd-001", "한라산", "50", "제주특별자치도", "50110", "제주시",
            "관광지", null, null, 1
        );
        var success = successResponse(List.of(item));
        givenApiReturns(success);

        var result = tourApiClient.getAreaBased(TourApiClient.SIGNGU_JEJU_SI, 1, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rlteTatsNm()).isEqualTo("한라산");
    }

    @Test
    void getAreaBased_returnsEmptyListWhenBodyItemsNull() {
        var success = new TourApiResponse<TourListItem>(
            new TourApiResponse.Response<>(new TourApiResponse.Header("0000", "OK"), null)
        );
        givenApiReturns(success);

        var result = tourApiClient.getAreaBased(TourApiClient.SIGNGU_JEJU_SI, 1, 10);

        assertThat(result).isEmpty();
    }

    private TourApiResponse<TourListItem> successResponse(List<TourListItem> items) {
        var header = new TourApiResponse.Header("0000", "OK");
        var body = new TourApiResponse.Body<>(new TourApiResponse.Items<>(items), items.size(), 10, 1);
        return new TourApiResponse<>(new TourApiResponse.Response<>(header, body));
    }
}
