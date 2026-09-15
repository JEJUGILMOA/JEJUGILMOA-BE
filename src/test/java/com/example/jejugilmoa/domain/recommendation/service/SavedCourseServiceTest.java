package com.example.jejugilmoa.domain.recommendation.service;

import com.example.jejugilmoa.domain.imageupload.service.ImageUrlResolver;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.record.entity.*;
import com.example.jejugilmoa.domain.record.repository.*;
import com.example.jejugilmoa.domain.recommendation.dto.*;
import com.example.jejugilmoa.domain.recommendation.entity.*;
import com.example.jejugilmoa.domain.recommendation.enums.CourseSourceType;
import com.example.jejugilmoa.domain.recommendation.repository.*;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedCourseServiceTest {
    @Mock SavedCourseRepository savedCourseRepository;
    @Mock RecommendedCourseRepository recommendedCourseRepository;
    @Mock TravelRecordRepository travelRecordRepository;
    @Mock TravelRecordPlaceRepository travelRecordPlaceRepository;
    @Mock UserRepository userRepository;
    @Mock ImageUrlResolver imageUrlResolver;
    @InjectMocks SavedCourseService service;

    @Test
    void recommendedPreservesExistingFieldsAndSortsWaypoints() {
        RecommendedCourse course = course(10L, List.of(path(2), path(1)));
        when(savedCourseRepository.findAllByUserIdWithSources(1L)).thenReturn(List.of(saved(course)));
        when(recommendedCourseRepository.findAllByIdInWithPaths(List.of(10L))).thenReturn(List.of(course));

        SavedCourseListItemResponse result = service.getSavedCourses(1L).getFirst();

        assertThat(result).isEqualTo(new SavedCourseListItemResponse(110L, CourseSourceType.RECOMMENDED,
                "추천 제목", "cover", "제주", 2, 90, 10L, "NATURE", "추천 설명", 7,
                List.of(waypoint(1), waypoint(2))));
        verify(recommendedCourseRepository, never()).countPathsByCourseIds(any());
        verifyNoInteractions(travelRecordPlaceRepository);
        verifyNoMoreInteractions(recommendedCourseRepository);
    }

    @Test
    void recordUsesSnapshotsAndKeepsNullableMetadataAndThumbnail() {
        TravelRecord record = record(20L);
        when(savedCourseRepository.findAllByUserIdWithSources(1L)).thenReturn(List.of(saved(record)));
        when(travelRecordPlaceRepository.findAllByRecordIdsWithPlaceInSnapshotOrder(List.of(20L)))
                .thenReturn(List.of(snapshot(record, 2), snapshot(record, 1)));
        when(imageUrlResolver.resolve("thumbnail-key")).thenReturn("resolved-thumbnail");

        SavedCourseListItemResponse result = service.getSavedCourses(1L).getFirst();

        assertThat(result).isEqualTo(new SavedCourseListItemResponse(120L, CourseSourceType.RECORD,
                "기록 제목", "resolved-thumbnail", null, 2, null, 20L, null, "기록 설명", null,
                List.of(snapshotWaypoint(1), snapshotWaypoint(2))));
        verify(travelRecordPlaceRepository, never()).countAllByRecordIds(any());
        verifyNoInteractions(recommendedCourseRepository);
        verifyNoMoreInteractions(travelRecordPlaceRepository);
    }

    @Test
    void mixedListBatchesBySourceAndPreservesSavedOrderAndIdIsolation() {
        RecommendedCourse first = course(10L, List.of(path(1)));
        RecommendedCourse second = course(11L, List.of(path(2)));
        TravelRecord record = record(10L);
        when(savedCourseRepository.findAllByUserIdWithSources(1L))
                .thenReturn(List.of(saved(record), saved(second), saved(first)));
        when(recommendedCourseRepository.findAllByIdInWithPaths(List.of(11L, 10L)))
                .thenReturn(List.of(first, second));
        when(travelRecordPlaceRepository.findAllByRecordIdsWithPlaceInSnapshotOrder(List.of(10L)))
                .thenReturn(List.of(snapshot(record, 1)));

        List<SavedCourseListItemResponse> results = service.getSavedCourses(1L);

        assertThat(results).extracting(SavedCourseListItemResponse::sourceId).containsExactly(10L, 11L, 10L);
        assertThat(results.get(0).waypoints()).containsExactly(snapshotWaypoint(1));
        assertThat(results.get(1).waypoints()).containsExactly(waypoint(2));
        assertThat(results.get(2).waypoints()).containsExactly(waypoint(1));
        verify(recommendedCourseRepository).findAllByIdInWithPaths(List.of(11L, 10L));
        verify(travelRecordPlaceRepository).findAllByRecordIdsWithPlaceInSnapshotOrder(List.of(10L));
        verifyNoMoreInteractions(recommendedCourseRepository, travelRecordPlaceRepository);
    }

    @Test
    void emptyWaypointsProduceEmptyListsAndZeroCountsForBothTypes() {
        RecommendedCourse course = course(10L, List.of());
        TravelRecord record = TravelRecord.builder().id(20L).title("빈 기록").build();
        when(savedCourseRepository.findAllByUserIdWithSources(1L)).thenReturn(List.of(saved(course), saved(record)));
        when(recommendedCourseRepository.findAllByIdInWithPaths(List.of(10L))).thenReturn(List.of(course));
        when(travelRecordPlaceRepository.findAllByRecordIdsWithPlaceInSnapshotOrder(List.of(20L))).thenReturn(List.of());

        assertThat(service.getSavedCourses(1L)).allSatisfy(result -> {
            assertThat(result.waypoints()).isEmpty();
            assertThat(result.placeCount()).isZero();
        });
        verifyNoInteractions(imageUrlResolver);
    }

    @Test
    void emptySavedListSkipsAllSourceQueries() {
        when(savedCourseRepository.findAllByUserIdWithSources(1L)).thenReturn(List.of());
        assertThat(service.getSavedCourses(1L)).isEmpty();
        verifyNoInteractions(recommendedCourseRepository, travelRecordPlaceRepository, imageUrlResolver);
    }

    private RecommendedCourse course(Long id, List<RecommendedCoursePath> paths) {
        return RecommendedCourse.builder().id(id).title("추천 제목").imageUrl("cover").region("제주")
                .estimatedMinutes(90).theme("NATURE").description("추천 설명").copyCount(7).paths(paths).build();
    }

    private TravelRecord record(Long id) {
        return TravelRecord.builder().id(id).title("기록 제목").description("기록 설명")
                .thumbnailImage(TravelRecordImage.builder().objectKey("thumbnail-key").build()).build();
    }

    private SavedCourse saved(RecommendedCourse course) {
        return SavedCourse.builder().id(course.getId() + 100).sourceType(CourseSourceType.RECOMMENDED)
                .recommendedCourse(course).build();
    }

    private SavedCourse saved(TravelRecord record) {
        return SavedCourse.builder().id(record.getId() + 100).sourceType(CourseSourceType.RECORD)
                .travelRecord(record).build();
    }

    private Place place(int order) {
        return Place.builder().id((long) order).name("현재 장소" + order).imageUrl("place-image" + order)
                .latitude(new BigDecimal("33.4")).longitude(new BigDecimal("126.4")).build();
    }

    private RecommendedCoursePath path(int order) {
        return RecommendedCoursePath.builder().sequenceOrder(order).place(place(order)).build();
    }

    private TravelRecordPlace snapshot(TravelRecord record, int order) {
        return TravelRecordPlace.builder().travelRecord(record).place(place(order)).sequenceOrder(order)
                .visitDate(LocalDate.of(2026, 9, 1)).placeName("기록 장소" + order)
                .latitude(new BigDecimal("33.5")).longitude(new BigDecimal("126.5")).build();
    }

    private CourseWaypointItem waypoint(int order) {
        return new CourseWaypointItem(order, (long) order, "현재 장소" + order, "place-image" + order,
                new BigDecimal("33.4"), new BigDecimal("126.4"));
    }

    private CourseWaypointItem snapshotWaypoint(int order) {
        return new CourseWaypointItem(order, (long) order, "기록 장소" + order, "place-image" + order,
                new BigDecimal("33.5"), new BigDecimal("126.5"));
    }
}
