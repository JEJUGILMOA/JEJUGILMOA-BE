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
        given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(owner));
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
                List.of(new TravelRecordPlaceMemoRequest(
                        102L, "노을이 좋았다", "records/42/aewol-place.webp")),
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
                .containsExactly("records/42/first.jpg", "records/42/second.webp", "records/42/aewol-place.webp");
        assertThat(imageCaptor.getValue()).extracting(TravelRecordImage::getSequenceOrder).containsExactly(1, 2, 3);
        assertThat(imageCaptor.getValue().get(0).getTravelRecordPlace()).isNull();
        assertThat(imageCaptor.getValue().get(1).getTravelRecordPlace()).isNull();
        assertThat(imageCaptor.getValue().get(2).getTravelRecordPlace()).isSameAs(placeCaptor.getValue().get(1));
        verify(imageObjectVerifier).verify("records/42/first.jpg");
        verify(imageObjectVerifier).verify("records/42/second.webp");
        verify(imageObjectVerifier).verify("records/42/aewol-place.webp");
    }

    @Test
    void create_defaultsVisibilityToPrivate() {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of());

        travelRecordService.create(USER_ID, request(null, null, null));

        ArgumentCaptor<TravelRecord> captor = ArgumentCaptor.forClass(TravelRecord.class);
        verify(travelRecordRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getVisibility()).isEqualTo(Visibility.PRIVATE);
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
        given(travelRecordRepository.existsByTravelPlanId(TRIP_ID)).willReturn(true);
        assertCode(request(null, null, null), RecordErrorCode.RECORD_ALREADY_EXISTS);
    }

    @Test
    void create_translatesUniqueConstraintRaceToConflict() {
        TravelPlan plan = completedPlan(owner);
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findAllByTravelPlanIdWithPlaceOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(List.of());
        given(travelRecordRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .willThrow(new DataIntegrityViolationException("uk_travel_record_plan"));

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

        assertCode(request(null, List.of(new TravelRecordPlaceMemoRequest(101L, null, objectKey)), null),
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
    void create_withLocalVerifierFailsWhenImageUsesAnotherUserPrefix() {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of(course(plan, 101L, 1L, "장소", "주소", "33.1", "126.1",
                LocalDate.of(2026, 8, 10), 1)));
        assertCode(localTravelRecordService(), request(null,
                        List.of(new TravelRecordPlaceMemoRequest(101L, null, "records/999/stolen.jpg")), null),
                RecordErrorCode.RECORD_INVALID_OBJECT_KEY);
        verifyNoInteractions(imageObjectVerifier);
    }

    @Test
    void create_withLocalVerifierFailsWhenImageObjectKeyIsDuplicated() {
        TravelPlan plan = completedPlan(owner);
        stubCreate(plan, List.of(course(plan, 101L, 1L, "장소", "주소", "33.1", "126.1",
                LocalDate.of(2026, 8, 10), 1)));
        assertCode(localTravelRecordService(),
                request(null, List.of(new TravelRecordPlaceMemoRequest(101L, null, "records/42/a.jpg")),
                        List.of("records/42/a.jpg")),
                RecordErrorCode.RECORD_INVALID_OBJECT_KEY);
        verifyNoInteractions(imageObjectVerifier);
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
