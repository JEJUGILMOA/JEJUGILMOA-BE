package com.example.jejugilmoa.domain.record.controller;

import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateResponse;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.service.TravelRecordService;
import com.example.jejugilmoa.domain.record.service.TravelRecordQueryService;
import com.example.jejugilmoa.domain.record.service.TravelRecordReactionService;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentCaptor.forClass;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TravelRecordController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity
class TravelRecordControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TravelRecordService travelRecordService;
    @MockitoBean TravelRecordQueryService travelRecordQueryService;
    @MockitoBean TravelRecordReactionService travelRecordReactionService;
    @MockitoBean JwtProvider jwtProvider;

    @Test
    void getRecordsUsesCardDefaultsAndStableSort() throws Exception {
        given(travelRecordQueryService.getRecords(eq(42L), any(), eq(false), any()))
                .willReturn(new com.example.jejugilmoa.global.apiPayload.dto.PageResponse<>(
                        List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/records").with(authentication(authenticationFor(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(20));

        var pageableCaptor = forClass(Pageable.class);
        verify(travelRecordQueryService).getRecords(
                eq(42L), eq(com.example.jejugilmoa.domain.record.enums.RecordView.CARD),
                eq(false), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    void getRecordsRejectsInvalidPageAndSize() throws Exception {
        mockMvc.perform(get("/api/records?page=-1").with(authentication(authenticationFor(42L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RECORD400_4"));

        mockMvc.perform(get("/api/records?size=101").with(authentication(authenticationFor(42L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RECORD400_5"));
    }

    @Test
    void getDetailReturnsNotFoundEnvelope() throws Exception {
        given(travelRecordQueryService.getDetail(999L, 42L))
                .willThrow(new GeneralException(com.example.jejugilmoa.domain.record.exception.RecordErrorCode.RECORD_NOT_FOUND));

        mockMvc.perform(get("/api/records/999").with(authentication(authenticationFor(42L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECORD404_1"));
    }

    @Test
    void setsLikeReaction() throws Exception {
        mockMvc.perform(post("/api/records/77/reactions")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"));

        verify(travelRecordReactionService).setReaction(eq(42L), eq(77L), any());
    }

    @Test
    void setsDislikeReaction() throws Exception {
        mockMvc.perform(post("/api/records/77/reactions")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"reactionType\":\"DISLIKE\"}"))
                .andExpect(status().isOk());

        var captor = forClass(com.example.jejugilmoa.domain.record.dto.TravelRecordReactionRequest.class);
        verify(travelRecordReactionService).setReaction(eq(42L), eq(77L), captor.capture());
        assertThat(captor.getValue().reactionType()).isEqualTo(
                com.example.jejugilmoa.domain.record.enums.ReactionType.DISLIKE);
    }

    @Test
    void deletesReaction() throws Exception {
        mockMvc.perform(delete("/api/records/77/reactions")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"));

        verify(travelRecordReactionService).deleteReaction(42L, 77L);
    }

    @Test
    void reactionEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/records/77/reactions")
                        .contentType("application/json")
                        .content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
        mockMvc.perform(delete("/api/records/77/reactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }

    @Test
    void reactionRejectsUnknownNullAndMissingType() throws Exception {
        for (String body : List.of(
                "{\"reactionType\":\"LOVE\"}",
                "{\"reactionType\":null}",
                "{}")) {
            mockMvc.perform(post("/api/records/77/reactions")
                            .with(authentication(authenticationFor(42L)))
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON400_1"));
        }
    }

    @Test
    void selfReactionErrorUsesForbiddenDomainCode() throws Exception {
        org.mockito.Mockito.doThrow(new GeneralException(RecordErrorCode.RECORD_SELF_REACTION_NOT_ALLOWED))
                .when(travelRecordReactionService).setReaction(eq(42L), eq(77L), any());

        mockMvc.perform(post("/api/records/77/reactions")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECORD403_3"));
    }

    @Test
    void authenticatedUserCreatesRecord() throws Exception {
        given(travelRecordService.create(eq(42L), any())).willReturn(new TravelRecordCreateResponse(
                77L, 10L, "여행 기록", Visibility.PRIVATE, Instant.parse("2026-08-12T03:00:00Z")));

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {"tripId":10,"title":"여행 기록","imageObjectKeys":[]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.recordId").value(77))
                .andExpect(jsonPath("$.result.tripId").value(10))
                .andExpect(jsonPath("$.result.visibility").value("PRIVATE"));
    }

    @Test
    void createAcceptsMultipleImagesForEachPlace() throws Exception {
        stubSuccessfulCreate();

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {"tripId":10,"title":"여행 기록","placeMemos":[
                                  {"travelCourseId":101,"imageObjectKeys":[
                                    "records/42/place-1.jpg","records/42/place-2.jpg"
                                  ]}
                                ]}
                                """))
                .andExpect(status().isCreated());

        var captor = forClass(com.example.jejugilmoa.domain.record.dto.TravelRecordCreateRequest.class);
        verify(travelRecordService).create(eq(42L), captor.capture());
        assertThat(captor.getValue().placeMemos().getFirst().imageObjectKeys())
                .containsExactly("records/42/place-1.jpg", "records/42/place-2.jpg");
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/records")
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"여행 기록\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }

    @Test
    void duplicateRecordReturnsConflictEnvelope() throws Exception {
        given(travelRecordService.create(eq(42L), any()))
                .willThrow(new GeneralException(RecordErrorCode.RECORD_ALREADY_EXISTS));

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"여행 기록\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("RECORD409_1"));
    }

    @Test
    void invalidRequestReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    void blankPlaceImageObjectKeyElementReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {"tripId":10,"title":"여행 기록","placeMemos":[
                                  {"travelCourseId":101,"memo":"메모","imageObjectKeys":["   "]}
                                ]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    void titleWith50CharactersIsAccepted() throws Exception {
        stubSuccessfulCreate();

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"%s\"}".formatted("가".repeat(50))))
                .andExpect(status().isCreated());
    }

    @Test
    void titleWith51CharactersIsRejected() throws Exception {
        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"%s\"}".formatted("가".repeat(51))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeMemoWith1000CharactersIsAccepted() throws Exception {
        stubSuccessfulCreate();

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {"tripId":10,"title":"기록","placeMemos":[
                                  {"travelCourseId":101,"memo":"%s"}
                                ]}
                                """.formatted("가".repeat(1000))))
                .andExpect(status().isCreated());
    }

    @Test
    void placeMemoWith1001CharactersIsRejected() throws Exception {
        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {"tripId":10,"title":"기록","placeMemos":[
                                  {"travelCourseId":101,"memo":"%s"}
                                ]}
                                """.formatted("가".repeat(1001))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void imageObjectKeyWith500CharactersIsAccepted() throws Exception {
        stubSuccessfulCreate();

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"기록\",\"imageObjectKeys\":[\"%s\"]}"
                                .formatted("k".repeat(500))))
                .andExpect(status().isCreated());
    }

    @Test
    void imageObjectKeyWith501CharactersIsRejected() throws Exception {
        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"기록\",\"imageObjectKeys\":[\"%s\"]}"
                                .formatted("k".repeat(501))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticatedOwnerUpdatesRecord() throws Exception {
        given(travelRecordService.update(eq(42L), eq(77L), any()))
                .willReturn(new TravelRecordUpdateResponse(
                        77L, "수정 제목", "", Visibility.PUBLIC,
                        Instant.parse("2026-08-26T03:00:00Z")));

        mockMvc.perform(patch("/api/records/77")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {
                                  "title":"수정 제목",
                                  "description":"",
                                  "visibility":"PUBLIC",
                                  "places":[{"recordPlaceId":501,"memo":"새 메모",
                                    "image":{"action":"REMOVE"}}],
                                  "imageObjectKeys":[]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.recordId").value(77))
                .andExpect(jsonPath("$.result.description").value(""))
                .andExpect(jsonPath("$.result.visibility").value("PUBLIC"));
    }

    @Test
    void updateAcceptsMultipleReplacementImagesForPlace() throws Exception {
        given(travelRecordService.update(eq(42L), eq(77L), any()))
                .willReturn(new TravelRecordUpdateResponse(
                        77L, "제목", null, Visibility.PRIVATE,
                        Instant.parse("2026-08-26T03:00:00Z")));

        mockMvc.perform(patch("/api/records/77")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {"places":[{"recordPlaceId":501,
                                  "image":{"action":"REPLACE","objectKeys":[
                                    "records/42/new-1.jpg","records/42/new-2.jpg"
                                  ]}}]}
                                """))
                .andExpect(status().isOk());

        var captor = forClass(com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateRequest.class);
        verify(travelRecordService).update(eq(42L), eq(77L), captor.capture());
        assertThat(captor.getValue().places().getFirst().image().objectKeys())
                .containsExactly("records/42/new-1.jpg", "records/42/new-2.jpg");
    }

    @Test
    void updateRejectsBlankTitleAndInvalidVisibility() throws Exception {
        mockMvc.perform(patch("/api/records/77")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));

        mockMvc.perform(patch("/api/records/77")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"visibility\":\"FRIENDS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    void updateRejectsNullPlaceElementWithBadRequest() throws Exception {
        mockMvc.perform(patch("/api/records/77")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"places\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));

        verify(travelRecordService, org.mockito.Mockito.never()).update(any(), any(), any());
    }

    @Test
    void ownerDeletesRecord() throws Exception {
        mockMvc.perform(delete("/api/records/77")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result").doesNotExist());

        verify(travelRecordService).delete(42L, 77L);
    }

    @Test
    void deletingAlreadyDeletedRecordReturnsGone() throws Exception {
        org.mockito.Mockito.doThrow(new GeneralException(RecordErrorCode.RECORD_ALREADY_DELETED))
                .when(travelRecordService).delete(42L, 77L);

        mockMvc.perform(delete("/api/records/77")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("RECORD410_1"));
    }

    private void stubSuccessfulCreate() {
        given(travelRecordService.create(eq(42L), any())).willReturn(new TravelRecordCreateResponse(
                77L, 10L, "여행 기록", Visibility.PRIVATE, Instant.parse("2026-08-12T03:00:00Z")));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(Long userId) {
        UserPrincipal principal = new UserPrincipal(userId, Role.USER);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }
}
