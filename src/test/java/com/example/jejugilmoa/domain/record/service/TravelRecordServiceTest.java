package com.example.jejugilmoa.domain.record.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.imageupload.service.ImageObjectVerifier;
import com.example.jejugilmoa.domain.imageupload.service.LocalImageObjectVerifier;
import com.example.jejugilmoa.domain.imageupload.exception.ImageUploadErrorCode;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordPlaceMemoRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordPlaceImageUpdateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordPlaceUpdateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateRequest;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.entity.TravelRecordImage;
import com.example.jejugilmoa.domain.record.entity.TravelRecordPlace;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.repository.TravelRecordImageRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TravelRecordServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long TRIP_ID = 10L;

    @Mock UserRepository userRepository;
    @Mock TravelPlanRepository travelPlanRepository;
    @Mock TravelCourseRepository travelCourseRepository;
    @Mock TravelRecordRepository travelRecordRepository;
    @Mock TravelRecordPlaceRepository travelRecordPlaceRepository;
    @Mock TravelRecordImageRepository travelRecordImageRepository;
    @Mock ImageObjectVerifier imageObjectVerifier;
    @InjectMocks TravelRecordService travelRecordService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(USER_ID).nickname("기록자").build();
        org.mockito.Mockito.lenient().when(userRepository.findByIdAndDeletedAtIsNull(USER_ID))
                .thenReturn(Optional.of(owner));
    }

    @Test
    void create_completedTripCopiesSnapshotsMemoImagesAndRequestedVisibility() {
        TravelPlan plan = completedPlan(owner);
        TravelCourse first = course(plan, 101L, 1L, "성산일출봉", "서귀포시 성산읍",
                "33.4587", "126.9426", LocalDate.of(2026, 8, 10), 1);
        TravelCourse second = course(plan, 102L, 2L, "애월 해변", "제주시 애월읍",
                "33.4625", "126.3223", LocalDate.of(2026, 8, 11), 1);
        stubCreate(plan, List.of(first, second));
        var request = request(Visibility.PUBLIC,
                List.of(
                        new TravelRecordPlaceMemoRequest(101L, null,
                                List.of("records/42/sungsan-1.jpg", "records/42/sungsan-2.jpg")),
                        new TravelRecordPlaceMemoRequest(102L, "노을이 좋았다",
                                List.of("records/42/aewol-1.webp", "records/42/aewol-2.webp"))),
                List.of("records/42/first.jpg", "records/42/second.webp"));

        var response = travelRecordService.create(USER_ID, request);

        ArgumentCaptor<TravelRecord> recordCaptor = ArgumentCaptor.forClass(TravelRecord.class);
        verify(travelRecordRepository).saveAndFlush(recordCaptor.capture());
        TravelRecord record = recordCaptor.getValue();
        assertThat(record.getTravelPlan()).isSameAs(plan);
        assertThat(record.getUser()).isSameAs(owner);
        assertThat(record.getActualStartDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(record.getActualEndDate()).isEqualTo(LocalDate.of(2026, 8, 11));
        assertThat(record.getVisibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(response.recordId()).isEqualTo(77L);
        assertThat(response.tripId()).isEqualTo(TRIP_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TravelRecordPlace>> placeCaptor = ArgumentCaptor.forClass(List.class);
        verify(travelRecordPlaceRepository).saveAll(placeCaptor.capture());
        assertThat(placeCaptor.getValue()).extracting(TravelRecordPlace::getSequenceOrder).containsExactly(1, 2);
        assertThat(placeCaptor.getValue()).extracting(TravelRecordPlace::getPlaceName)
                .containsExactly("성산일출봉", "애월 해변");
        assertThat(placeCaptor.getValue().get(1).getAddress()).isEqualTo("제주시 애월읍");
        assertThat(placeCaptor.getValue().get(1).getLatitude()).isEqualByComparingTo("33.4625");
        assertThat(placeCaptor.getValue().get(1).getLongitude()).isEqualByComparingTo("126.3223");
        assertThat(placeCaptor.getValue().get(1).getVisitDate()).isEqualTo(LocalDate.of(2026, 8, 11));
        assertThat(placeCaptor.getValue()).extracting(TravelRecordPlace::getMemo)
                .containsExactly(null, "노을이 좋았다");
        assertThat(placeCaptor.getValue()).allSatisfy(place -> {
            assertThat(place.getRating()).isNull();
            assertThat(place.getStayMinutes()).isNull();
        });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TravelRecordImage>> imageCaptor = ArgumentCaptor.forClass(List.class);
        verify(travelRecordImageRepository).saveAll(imageCaptor.capture());
        assertThat(imageCaptor.getValue()).extracting(TravelRecordImage::getObjectKey)
                .containsExactly("records/42/first.jpg", "records/42/second.webp",
                        "records/42/sungsan-1.jpg", "records/42/sungsan-2.jpg",
                        "records/42/aewol-1.webp", "records/42/aewol-2.webp");
        assertThat(imageCaptor.getValue()).extracting(TravelRecordImage::getSequenceOrder)
                .containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(imageCaptor.getValue().get(0).getTravelRecordPlace()).isNull();
        assertThat(imageCaptor.getValue().get(1).getTravelRecordPlace()).isNull();
        assertThat(imageCaptor.getValue().subList(2, 4)).allSatisfy(image ->
                assertThat(image.getTravelRecordPlace()).isSameAs(placeCaptor.getValue().get(0)));
        assertThat(imageCaptor.getValue().subList(4, 6)).allSatisfy(image ->
                assertThat(image.getTravelRecordPlace()).isSameAs(placeCaptor.getValue().get(1)));
        assertThat(imageCaptor.getValue().getFirst().getTravelRecord().getThumbnailImage())
                .isSameAs(imageCaptor.getValue().getFirst());
        verify(imageObjectVerifier).verify("records/42/first.jpg");
        verify(imageObjectVerifier).verify("records/42/second.webp");
        verify(imageObjectVerifier).verify("records/42/sungsan-1.jpg");
        verify(imageObjectVerifier).verify("records/42/sungsan-2.jpg");
        verify(imageObjectVerifier).verify("records/42/aewol-1.webp");
        verify(imageObjectVerifier).verify("records/42/aewol-2.webp");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"second.jpg", "place.jpg"})
    void create_selectsRequestedThumbnailWithoutChangingSequence(String filename) {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of(course(plan, 101L, 1L, "장소", "주소", "33.1", "126.1",
                LocalDate.of(2026, 8, 10), 1)));
        String selectedKey = "records/42/" + filename;
        travelRecordService.create(USER_ID, new TravelRecordCreateRequest(
                TRIP_ID, "여행 기록", null, null,
                List.of(new TravelRecordPlaceMemoRequest(101L, null, List.of("records/42/place.jpg"))),
                List.of("records/42/first.jpg", "records/42/second.jpg"), selectedKey));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TravelRecordImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(travelRecordImageRepository).saveAll(captor.capture());
        List<TravelRecordImage> images = captor.getValue();
        assertThat(images).extracting(TravelRecordImage::getObjectKey)
                .containsExactly("records/42/first.jpg", "records/42/second.jpg", "records/42/place.jpg");
        assertThat(images).extracting(TravelRecordImage::getSequenceOrder).containsExactly(1, 2, 3);
        assertThat(images.getFirst().getTravelRecord().getThumbnailImage())
                .isSameAs(images.get(filename.equals("place.jpg") ? 2 : 1));
        assertThat(images.getLast().getTravelRecordPlace()).isNotNull();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void create_rejectsThumbnailOutsideRequest(boolean hasImages) {
        stubCreate(completedPlan(owner), List.of());
        assertCode(new TravelRecordCreateRequest(TRIP_ID, "여행 기록", null, null, null,
                        hasImages ? List.of("records/42/first.jpg") : null, "records/42/missing.jpg"),
                RecordErrorCode.RECORD_THUMBNAIL_TARGET_MISMATCH);
    }

    @Test
    void create_defaultsVisibilityToPrivate() {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of());

        travelRecordService.create(USER_ID, request(null, null, null));

        ArgumentCaptor<TravelRecord> captor = ArgumentCaptor.forClass(TravelRecord.class);
        verify(travelRecordRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(captor.getValue().getThumbnailImage()).isNull();
    }

    @Test
    void create_failsWhenTripDoesNotExist() {
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.empty());
        assertCode(request(null, null, null), PlanErrorCode.PLAN_NOT_FOUND);
    }

    @Test
    void create_failsForAnotherUsersTrip() {
        TravelPlan plan = completedPlan(User.builder().id(999L).nickname("타인").build());
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        assertCode(request(null, null, null), RecordErrorCode.RECORD_TRIP_ACCESS_DENIED);
    }

    @ParameterizedTest
    @EnumSource(value = TravelPlanStatus.class, names = {"DRAFT", "IN_PROGRESS"})
    void create_failsForIncompleteTrip(TravelPlanStatus status) {
        TravelPlan plan = TravelPlan.builder().id(TRIP_ID).user(owner).status(status).build();
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        assertCode(request(null, null, null), RecordErrorCode.RECORD_TRIP_NOT_COMPLETED);
    }

    @Test
    void create_failsWhenRecordAlreadyExists() {
        TravelPlan plan = completedPlan(owner);
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelRecordRepository.existsByTravelPlanIdAndDeletedAtIsNull(TRIP_ID)).willReturn(true);
        assertCode(request(null, null, null), RecordErrorCode.RECORD_ALREADY_EXISTS);
    }

    @Test
    void create_succeedsWhenOnlySoftDeletedRecordExists() {
        TravelPlan plan = completedPlan(owner);
        given(travelRecordRepository.existsByTravelPlanIdAndDeletedAtIsNull(TRIP_ID)).willReturn(false);
        stubCreate(plan, List.of());

        travelRecordService.create(USER_ID, request(null, null, null));

        verify(travelRecordRepository).existsByTravelPlanIdAndDeletedAtIsNull(TRIP_ID);
        verify(travelRecordRepository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void create_translatesUniqueConstraintRaceToConflict() {
        TravelPlan plan = completedPlan(owner);
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findAllByTravelPlanIdWithPlaceOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(List.of());
        given(travelRecordRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .willThrow(new DataIntegrityViolationException("uk_travel_record_active_plan"));

        assertCode(request(null, null, null), RecordErrorCode.RECORD_ALREADY_EXISTS);
    }

    @Test
    void create_failsWhenMemoTargetsCourseFromAnotherTrip() {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of(course(plan, 101L, 1L, "장소", "주소", "33.1", "126.1",
                LocalDate.of(2026, 8, 10), 1)));
        assertCode(request(null, List.of(new TravelRecordPlaceMemoRequest(999L, "침범", null)), null),
                RecordErrorCode.RECORD_MEMO_TARGET_MISMATCH);
    }

    @Test
    void create_failsWhenPlaceImageObjectDoesNotExist() {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of(course(plan, 101L, 1L, "장소", "주소", "33.1", "126.1",
                LocalDate.of(2026, 8, 10), 1)));
        String objectKey = "records/42/missing.jpg";
        willThrow(new GeneralException(ImageUploadErrorCode.OBJECT_NOT_FOUND))
                .given(imageObjectVerifier).verify(objectKey);

        assertCode(request(null, List.of(new TravelRecordPlaceMemoRequest(101L, null, List.of(objectKey))), null),
                ImageUploadErrorCode.OBJECT_NOT_FOUND);
    }

    @Test
    void create_succeedsWhenPlacesHaveNoImages() {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of(course(plan, 101L, 1L, "장소", "주소", "33.1", "126.1",
                LocalDate.of(2026, 8, 10), 1)));

        travelRecordService.create(USER_ID,
                request(null, List.of(new TravelRecordPlaceMemoRequest(101L, "메모", null)), null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TravelRecordImage>> imageCaptor = ArgumentCaptor.forClass(List.class);
        verify(travelRecordImageRepository).saveAll(imageCaptor.capture());
        assertThat(imageCaptor.getValue()).isEmpty();
        verifyNoInteractions(imageObjectVerifier);
    }

    @Test
    void create_succeedsWhenPlaceHasOneImage() {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of(course(plan, 101L, 1L, "장소", "주소", "33.1", "126.1",
                LocalDate.of(2026, 8, 10), 1)));

        travelRecordService.create(USER_ID, request(null,
                List.of(new TravelRecordPlaceMemoRequest(
                        101L, null, List.of("records/42/place.jpg"))), null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TravelRecordImage>> imageCaptor = ArgumentCaptor.forClass(List.class);
        verify(travelRecordImageRepository).saveAll(imageCaptor.capture());
        assertThat(imageCaptor.getValue()).extracting(TravelRecordImage::getObjectKey)
                .containsExactly("records/42/place.jpg");
        assertThat(imageCaptor.getValue().getFirst().getTravelRecord().getThumbnailImage())
                .isSameAs(imageCaptor.getValue().getFirst());
    }

    @Test
    void create_withLocalVerifierFailsWhenImageUsesAnotherUserPrefix() {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of(course(plan, 101L, 1L, "장소", "주소", "33.1", "126.1",
                LocalDate.of(2026, 8, 10), 1)));
        assertCode(localTravelRecordService(), request(null,
                        List.of(new TravelRecordPlaceMemoRequest(
                                101L, null, List.of("records/999/stolen.jpg"))), null),
                RecordErrorCode.RECORD_INVALID_OBJECT_KEY);
        verifyNoInteractions(imageObjectVerifier);
    }

    @Test
    void create_withLocalVerifierFailsWhenImageObjectKeyIsDuplicated() {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of(course(plan, 101L, 1L, "장소", "주소", "33.1", "126.1",
                LocalDate.of(2026, 8, 10), 1)));
        assertCode(localTravelRecordService(),
                request(null, List.of(new TravelRecordPlaceMemoRequest(
                                101L, null, List.of("records/42/a.jpg"))),
                        List.of("records/42/a.jpg")),
                RecordErrorCode.RECORD_INVALID_OBJECT_KEY);
        verifyNoInteractions(imageObjectVerifier);
    }

    @Test
    void update_changesOnlyEditableContentAndSupportsRecordWithoutPlan() {
        TravelRecord record = record(77L, owner, null);
        TravelRecordPlace place = recordPlace(501L, record, "기록 당시 장소");
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID)).willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L)).willReturn(List.of(place));
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L)).willReturn(List.of());

        travelRecordService.update(USER_ID, 77L, new TravelRecordUpdateRequest(
                "수정 제목", "", Visibility.PUBLIC,
                List.of(new TravelRecordPlaceUpdateRequest(501L, "수정 메모", null)), null));

        assertThat(record.getTitle()).isEqualTo("수정 제목");
        assertThat(record.getDescription()).isEmpty();
        assertThat(record.getVisibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(record.getTravelPlan()).isNull();
        assertThat(place.getMemo()).isEqualTo("수정 메모");
        assertThat(place.getPlaceName()).isEqualTo("기록 당시 장소");
        assertThat(place.getAddress()).isEqualTo("snapshot address");
        assertThat(place.getLatitude()).isEqualByComparingTo("33.1");
        assertThat(place.getLongitude()).isEqualByComparingTo("126.1");
        assertThat(place.isVisited()).isTrue();
        assertThat(place.getSequenceOrder()).isEqualTo(1);
        verifyNoInteractions(imageObjectVerifier);
    }

    @Test
    void update_diffsRecordImagesAndReordersWhileVerifyingOnlyNewKey() {
        TravelRecord record = record(77L, owner, null);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID)).willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L)).willReturn(List.of());
        TravelRecordImage a = image(1L, record, null, "records/42/A.jpg", 1);
        TravelRecordImage b = image(2L, record, null, "records/42/B.jpg", 2);
        TravelRecordImage c = image(3L, record, null, "records/42/C.jpg", 3);
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L))
                .willReturn(List.of(a, b, c));

        travelRecordService.update(USER_ID, 77L, new TravelRecordUpdateRequest(
                null, null, null, null,
                List.of("records/42/B.jpg", "records/42/C.jpg", "records/42/D.jpg")));

        verify(imageObjectVerifier).verify("records/42/D.jpg");
        verify(imageObjectVerifier, never()).verify("records/42/B.jpg");
        verify(travelRecordImageRepository).deleteAll(List.of(a));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TravelRecordImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(travelRecordImageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(TravelRecordImage::getObjectKey)
                .containsExactly("records/42/B.jpg", "records/42/C.jpg", "records/42/D.jpg");
        assertThat(captor.getValue()).extracting(TravelRecordImage::getSequenceOrder)
                .containsExactly(1, 2, 3);
    }

    @Test
    void update_addsReplacesAndRemovesPlaceImages() {
        TravelRecord record = record(77L, owner, null);
        TravelRecordPlace first = recordPlace(501L, record, "첫 장소");
        TravelRecordPlace second = recordPlace(502L, record, "둘째 장소");
        TravelRecordPlace third = recordPlace(503L, record, "셋째 장소");
        TravelRecordImage firstImage = image(11L, record, first, "records/42/old.jpg", 1);
        TravelRecordImage secondImage = image(12L, record, second, "records/42/remove.jpg", 2);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID)).willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L))
                .willReturn(List.of(first, second, third));
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L))
                .willReturn(List.of(firstImage, secondImage));

        travelRecordService.update(USER_ID, 77L, new TravelRecordUpdateRequest(null, null, null,
                List.of(
                        new TravelRecordPlaceUpdateRequest(501L, null,
                                new TravelRecordPlaceImageUpdateRequest(
                                        com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction.REPLACE,
                                        List.of("records/42/new-1.jpg", "records/42/new-2.jpg"))),
                        new TravelRecordPlaceUpdateRequest(502L, null,
                                new TravelRecordPlaceImageUpdateRequest(
                                        com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction.REMOVE, null)),
                        new TravelRecordPlaceUpdateRequest(503L, null,
                                new TravelRecordPlaceImageUpdateRequest(
                                        com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction.REPLACE,
                                        List.of("records/42/third-1.jpg", "records/42/third-2.jpg")))), null));

        verify(travelRecordImageRepository).deleteAll(List.of(firstImage, secondImage));
        verify(imageObjectVerifier).verify("records/42/new-1.jpg");
        verify(imageObjectVerifier).verify("records/42/new-2.jpg");
        verify(imageObjectVerifier).verify("records/42/third-1.jpg");
        verify(imageObjectVerifier).verify("records/42/third-2.jpg");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TravelRecordImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(travelRecordImageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(TravelRecordImage::getObjectKey)
                .containsExactly("records/42/new-1.jpg", "records/42/new-2.jpg",
                        "records/42/third-1.jpg", "records/42/third-2.jpg");
    }

    @Test
    void update_changesOnlyThumbnailWithoutMutatingImageOrder() {
        TravelRecord record = record(77L, owner, null);
        TravelRecordImage a = image(1L, record, null, "records/42/A.jpg", 1);
        TravelRecordImage b = image(2L, record, null, "records/42/B.jpg", 2);
        TravelRecordImage c = image(3L, record, null, "records/42/C.jpg", 3);
        record.changeThumbnailImage(a);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID))
                .willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L)).willReturn(List.of());
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L))
                .willReturn(List.of(a, b, c));

        travelRecordService.update(USER_ID, 77L, new TravelRecordUpdateRequest(
                null, null, null, null, null, "records/42/C.jpg"));

        assertThat(record.getThumbnailImage()).isSameAs(c);
        assertThat(List.of(a, b, c)).extracting(TravelRecordImage::getSequenceOrder)
                .containsExactly(1, 2, 3);
        verify(travelRecordImageRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(travelRecordImageRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyList());
        verify(travelRecordImageRepository, never()).flush();
    }

    @Test
    void update_allowsPlaceImageAsThumbnail() {
        TravelRecord record = record(77L, owner, null);
        TravelRecordPlace place = recordPlace(501L, record, "장소");
        TravelRecordImage placeImage = image(
                3L, record, place, "records/42/place.jpg", 3);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID))
                .willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L))
                .willReturn(List.of(place));
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L))
                .willReturn(List.of(placeImage));

        travelRecordService.update(USER_ID, 77L, new TravelRecordUpdateRequest(
                null, null, null, null, null, "records/42/place.jpg"));

        assertThat(record.getThumbnailImage()).isSameAs(placeImage);
        assertThat(placeImage.getSequenceOrder()).isEqualTo(3);
    }

    @Test
    void update_rejectsThumbnailOutsideFinalImageSet() {
        TravelRecord record = record(77L, owner, null);
        TravelRecordImage image = image(1L, record, null, "records/42/A.jpg", 1);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID))
                .willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L)).willReturn(List.of());
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L))
                .willReturn(List.of(image));

        assertUpdateCode(77L, new TravelRecordUpdateRequest(
                        null, null, null, null, null, "records/42/foreign.jpg"),
                RecordErrorCode.RECORD_THUMBNAIL_TARGET_MISMATCH);
    }

    @Test
    void update_selectsNewRecordImageAsThumbnail() {
        TravelRecord record = record(77L, owner, null);
        TravelRecordImage old = image(1L, record, null, "records/42/old.jpg", 1);
        record.changeThumbnailImage(old);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID))
                .willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L)).willReturn(List.of());
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L))
                .willReturn(List.of(old));

        travelRecordService.update(USER_ID, 77L, new TravelRecordUpdateRequest(
                null, null, null, null,
                List.of("records/42/old.jpg", "records/42/new.jpg"), "records/42/new.jpg"));

        assertThat(record.getThumbnailImage().getObjectKey()).isEqualTo("records/42/new.jpg");
        assertThat(record.getThumbnailImage().getSequenceOrder()).isEqualTo(2);
    }

    @Test
    void update_selectsNewPlaceImageAsThumbnail() {
        TravelRecord record = record(77L, owner, null);
        TravelRecordPlace place = recordPlace(501L, record, "장소");
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID))
                .willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L))
                .willReturn(List.of(place));
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L)).willReturn(List.of());
        var placeUpdate = new TravelRecordPlaceUpdateRequest(501L, null,
                new TravelRecordPlaceImageUpdateRequest(
                        com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction.REPLACE,
                        List.of("records/42/new-place.jpg")));

        travelRecordService.update(USER_ID, 77L, new TravelRecordUpdateRequest(
                null, null, null, List.of(placeUpdate), null, "records/42/new-place.jpg"));

        assertThat(record.getThumbnailImage().getObjectKey()).isEqualTo("records/42/new-place.jpg");
        assertThat(record.getThumbnailImage().getTravelRecordPlace()).isSameAs(place);
    }

    @Test
    void update_fallsBackWhenCurrentThumbnailIsRemoved() {
        TravelRecord record = record(77L, owner, null);
        TravelRecordImage a = image(1L, record, null, "records/42/A.jpg", 1);
        TravelRecordImage b = image(2L, record, null, "records/42/B.jpg", 2);
        record.changeThumbnailImage(a);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID))
                .willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L)).willReturn(List.of());
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L))
                .willReturn(List.of(a, b));

        travelRecordService.update(USER_ID, 77L, new TravelRecordUpdateRequest(
                null, null, null, null, List.of("records/42/B.jpg"), null));

        assertThat(record.getThumbnailImage()).isSameAs(b);
        assertThat(b.getSequenceOrder()).isEqualTo(1);
        var inOrder = org.mockito.Mockito.inOrder(travelRecordRepository, travelRecordImageRepository);
        inOrder.verify(travelRecordRepository).flush();
        inOrder.verify(travelRecordImageRepository).deleteAll(List.of(a));
    }

    @Test
    void update_clearsThumbnailWhenLastImageIsRemoved() {
        TravelRecord record = record(77L, owner, null);
        TravelRecordImage only = image(1L, record, null, "records/42/only.jpg", 1);
        record.changeThumbnailImage(only);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID))
                .willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L)).willReturn(List.of());
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L))
                .willReturn(List.of(only));

        travelRecordService.update(USER_ID, 77L, new TravelRecordUpdateRequest(
                null, null, null, null, List.of(), null));

        assertThat(record.getThumbnailImage()).isNull();
    }

    @Test
    void update_keepsCurrentThumbnailWhenAnotherImageIsRemoved() {
        TravelRecord record = record(77L, owner, null);
        TravelRecordImage a = image(1L, record, null, "records/42/A.jpg", 1);
        TravelRecordImage b = image(2L, record, null, "records/42/B.jpg", 2);
        record.changeThumbnailImage(b);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID))
                .willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L)).willReturn(List.of());
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L))
                .willReturn(List.of(a, b));

        travelRecordService.update(USER_ID, 77L, new TravelRecordUpdateRequest(
                null, null, null, null, List.of("records/42/B.jpg"), null));

        assertThat(record.getThumbnailImage()).isSameAs(b);
    }

    @Test
    void update_rejectsForeignDeletedAndMismatchedPlace() {
        given(travelRecordRepository.existsActiveById(77L)).willReturn(true);
        assertUpdateCode(77L, new TravelRecordUpdateRequest("제목", null, null, null, null),
                RecordErrorCode.RECORD_ACCESS_DENIED);

        assertUpdateCode(78L, new TravelRecordUpdateRequest("제목", null, null, null, null),
                RecordErrorCode.RECORD_NOT_FOUND);

        TravelRecord own = record(79L, owner, null);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(79L, USER_ID)).willReturn(Optional.of(own));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(79L)).willReturn(List.of());
        assertUpdateCode(79L, new TravelRecordUpdateRequest(null, null, null,
                        List.of(new TravelRecordPlaceUpdateRequest(999L, "침범", null)), null),
                RecordErrorCode.RECORD_PLACE_TARGET_MISMATCH);
    }

    @Test
    void update_rejectsForeignActiveRecord() {
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID)).willReturn(Optional.empty());
        given(travelRecordRepository.existsActiveById(77L)).willReturn(true);

        assertUpdateCode(77L, new TravelRecordUpdateRequest("제목", null, null, null, null),
                RecordErrorCode.RECORD_ACCESS_DENIED);
    }

    @Test
    void update_propagatesNewImageVerificationFailure() {
        TravelRecord record = record(77L, owner, null);
        given(travelRecordRepository.findActiveOwnedRecordForUpdate(77L, USER_ID)).willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(77L)).willReturn(List.of());
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(77L)).willReturn(List.of());
        willThrow(new GeneralException(ImageUploadErrorCode.OBJECT_NOT_FOUND))
                .given(imageObjectVerifier).verify("records/42/missing.jpg");

        assertUpdateCode(77L, new TravelRecordUpdateRequest(null, null, null, null,
                List.of("records/42/missing.jpg")), ImageUploadErrorCode.OBJECT_NOT_FOUND);
    }

    @Test
    void delete_softDeletesOwnedRecordWithoutDeletingRowsOrPlan() {
        TravelPlan plan = completedPlan(owner);
        TravelRecord record = TravelRecord.builder().id(77L).travelPlan(plan).user(owner)
                .title("기록").build();
        given(travelRecordRepository.findActiveOwnedRecord(77L, USER_ID)).willReturn(Optional.of(record));

        travelRecordService.delete(USER_ID, 77L);

        assertThat(record.getDeletedAt()).isNotNull();
        assertThat(record.getTravelPlan()).isSameAs(plan);
        verify(travelRecordRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(travelRecordPlaceRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void delete_rejectsForeignAndDeletedRecord() {
        given(travelRecordRepository.existsActiveById(77L)).willReturn(true);
        assertThatThrownBy(() -> travelRecordService.delete(USER_ID, 77L))
                .isInstanceOf(GeneralException.class)
                .satisfies(error -> assertThat(((GeneralException) error).getCode())
                        .isEqualTo(RecordErrorCode.RECORD_ACCESS_DENIED));
        assertThatThrownBy(() -> travelRecordService.delete(USER_ID, 78L))
                .isInstanceOf(GeneralException.class)
                .satisfies(error -> assertThat(((GeneralException) error).getCode())
                        .isEqualTo(RecordErrorCode.RECORD_NOT_FOUND));
    }

    private TravelRecord record(Long id, User user, LocalDateTime deletedAt) {
        return TravelRecord.builder().id(id).user(user).title("원본 제목").description("원본 소개")
                .visibility(Visibility.PRIVATE).actualStartDate(LocalDate.of(2026, 8, 10))
                .actualEndDate(LocalDate.of(2026, 8, 11)).deletedAt(deletedAt).build();
    }

    private TravelRecordPlace recordPlace(Long id, TravelRecord record, String name) {
        return TravelRecordPlace.builder().id(id).travelRecord(record)
                .place(Place.builder().id(id).name("현재 장소").build()).placeName(name)
                .address("snapshot address").latitude(new BigDecimal("33.1"))
                .longitude(new BigDecimal("126.1")).visitDate(LocalDate.of(2026, 8, 10))
                .sequenceOrder(1).visited(true).visitedAt(LocalDateTime.of(2026, 8, 10, 12, 0)).build();
    }

    private TravelRecordImage image(Long id, TravelRecord record, TravelRecordPlace place,
                                    String key, int sequence) {
        return TravelRecordImage.builder().id(id).travelRecord(record).travelRecordPlace(place)
                .objectKey(key).sequenceOrder(sequence).build();
    }

    private void assertUpdateCode(Long recordId, TravelRecordUpdateRequest request, Object expectedCode) {
        assertThatThrownBy(() -> travelRecordService.update(USER_ID, recordId, request))
                .isInstanceOf(GeneralException.class)
                .satisfies(error -> assertThat(((GeneralException) error).getCode()).isEqualTo(expectedCode));
    }

    private void stubCreate(TravelPlan plan, List<TravelCourse> courses) {
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findAllByTravelPlanIdWithPlaceOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(courses);
        org.mockito.Mockito.lenient().when(travelRecordRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> TravelRecord.builder()
                        .id(77L).travelPlan(plan).user(owner)
                        .title(invocation.<TravelRecord>getArgument(0).getTitle())
                        .description(invocation.<TravelRecord>getArgument(0).getDescription())
                        .actualStartDate(invocation.<TravelRecord>getArgument(0).getActualStartDate())
                        .actualEndDate(invocation.<TravelRecord>getArgument(0).getActualEndDate())
                        .visibility(invocation.<TravelRecord>getArgument(0).getVisibility()).build());
    }

    private TravelPlan completedPlan(User user) {
        return TravelPlan.builder().id(TRIP_ID).user(user).status(TravelPlanStatus.COMPLETED)
                .actualStartedAt(LocalDateTime.of(2026, 8, 10, 9, 0))
                .actualCompletedAt(LocalDateTime.of(2026, 8, 11, 18, 0)).build();
    }

    private TravelCourse course(TravelPlan plan, Long id, Long placeId, String name, String address,
                                String latitude, String longitude, LocalDate visitDate, int sequence) {
        Place place = Place.builder().id(placeId).name(name).address(address)
                .latitude(new BigDecimal(latitude)).longitude(new BigDecimal(longitude)).build();
        return TravelCourse.builder().id(id).travelPlan(plan).place(place).visitDate(visitDate)
                .sequenceOrder(sequence).visited(true).visitedAt(visitDate.atTime(12, 0)).build();
    }

    private TravelRecordCreateRequest request(Visibility visibility,
                                               List<TravelRecordPlaceMemoRequest> memos,
                                               List<String> imageObjectKeys) {
        return new TravelRecordCreateRequest(TRIP_ID, "여행 기록", "설명", visibility, memos, imageObjectKeys);
    }

    private void assertCode(TravelRecordCreateRequest request, Object expectedCode) {
        assertCode(travelRecordService, request, expectedCode);
    }

    private void assertCode(TravelRecordService service, TravelRecordCreateRequest request, Object expectedCode) {
        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> assertThat(((GeneralException) exception).getCode()).isEqualTo(expectedCode));
    }

    private TravelRecordService localTravelRecordService() {
        return new TravelRecordService(userRepository, travelPlanRepository, travelCourseRepository,
                travelRecordRepository, travelRecordPlaceRepository, travelRecordImageRepository,
                new LocalImageObjectVerifier());
    }
}
