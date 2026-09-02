package com.example.jejugilmoa.domain.record.service;

import com.example.jejugilmoa.domain.imageupload.service.ImageUrlResolver;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.entity.TravelRecordImage;
import com.example.jejugilmoa.domain.record.entity.TravelRecordPlace;
import com.example.jejugilmoa.domain.record.enums.ReactionType;
import com.example.jejugilmoa.domain.record.enums.RecordView;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.repository.TravelRecordImageRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordReactionRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TravelRecordQueryServiceTest {

    @Mock TravelRecordRepository travelRecordRepository;
    @Mock TravelRecordPlaceRepository travelRecordPlaceRepository;
    @Mock TravelRecordImageRepository travelRecordImageRepository;
    @Mock TravelRecordReactionRepository travelRecordReactionRepository;
    @Mock ImageUrlResolver imageUrlResolver;
    @InjectMocks TravelRecordQueryService service;

    @Test
    void mineCardReturnsPublicAndPrivateWithBulkCountsAndFirstThumbnail() {
        User owner = user(1L, "나");
        TravelRecord publicRecord = record(10L, owner, Visibility.PUBLIC);
        TravelRecord privateRecord = record(11L, owner, Visibility.PRIVATE);
        var pageable = PageRequest.of(0, 20);
        given(travelRecordRepository.findActiveByUserId(1L, pageable))
                .willReturn(new PageImpl<>(List.of(publicRecord, privateRecord), pageable, 2));

        TravelRecordImage first = TravelRecordImage.builder().id(101L).travelRecord(publicRecord)
                .objectKey("records/1/first.jpg").sequenceOrder(1).build();
        given(travelRecordImageRepository.findFirstImagesByRecordIds(List.of(10L, 11L)))
                .willReturn(List.of(first));
        given(imageUrlResolver.resolve("records/1/first.jpg")).willReturn("https://signed/first.jpg");

        var visited = mock(TravelRecordPlaceRepository.VisitedPlaceCount.class);
        given(visited.getRecordId()).willReturn(10L);
        given(visited.getCount()).willReturn(2L);
        given(travelRecordPlaceRepository.countVisitedByRecordIds(List.of(10L, 11L)))
                .willReturn(List.of(visited));

        var photos = mock(TravelRecordImageRepository.RecordImageCount.class);
        given(photos.getRecordId()).willReturn(10L);
        given(photos.getCount()).willReturn(3L);
        given(travelRecordImageRepository.countByRecordIds(List.of(10L, 11L)))
                .willReturn(List.of(photos));

        var likes = reactionCount(10L, ReactionType.LIKE, 4L);
        var dislikes = reactionCount(10L, ReactionType.DISLIKE, 1L);
        given(travelRecordReactionRepository.countByRecordIdsAndType(List.of(10L, 11L)))
                .willReturn(List.of(likes, dislikes));
        given(travelRecordReactionRepository.findMineByRecordIds(List.of(10L, 11L), 1L))
                .willReturn(List.of());

        var result = service.getRecords(1L, RecordView.CARD, true, pageable);

        assertThat(result.content()).hasSize(2);
        var cards = result.content().stream()
                .map(item -> (com.example.jejugilmoa.domain.record.dto.TravelRecordCardResponse) item)
                .toList();
        assertThat(cards).extracting(card -> card.visibility())
                .containsExactly(Visibility.PUBLIC, Visibility.PRIVATE);
        assertThat(cards.get(0).thumbnailUrl()).isEqualTo("https://signed/first.jpg");
        assertThat(cards.get(0).visitedPlaceCount()).isEqualTo(2);
        assertThat(cards.get(0).photoCount()).isEqualTo(3);
        assertThat(cards.get(0).likeCount()).isEqualTo(4);
        assertThat(cards.get(0).dislikeCount()).isEqualTo(1);
        assertThat(cards.get(1).thumbnailUrl()).isNull();
        verify(imageUrlResolver).resolve("records/1/first.jpg");
    }

    @Test
    void publicCardUsesPublicRepositoryAndIncludesOwnersPublicRecord() {
        User owner = user(1L, "나");
        TravelRecord ownPublic = record(10L, owner, Visibility.PUBLIC);
        var pageable = PageRequest.of(0, 20);
        given(travelRecordRepository.findActivePublic(pageable))
                .willReturn(new PageImpl<>(List.of(ownPublic), pageable, 1));
        given(travelRecordImageRepository.findFirstImagesByRecordIds(anyList())).willReturn(List.of());
        given(travelRecordPlaceRepository.countVisitedByRecordIds(anyList())).willReturn(List.of());
        given(travelRecordImageRepository.countByRecordIds(anyList())).willReturn(List.of());
        given(travelRecordReactionRepository.countByRecordIdsAndType(anyList())).willReturn(List.of());
        given(travelRecordReactionRepository.findMineByRecordIds(anyList(), org.mockito.ArgumentMatchers.eq(1L)))
                .willReturn(List.of());

        var result = service.getRecords(1L, RecordView.CARD, false, pageable);

        assertThat(result.content()).hasSize(1);
        verify(travelRecordRepository).findActivePublic(pageable);
        verify(travelRecordRepository, never()).findActiveByUserId(1L, pageable);
    }

    @Test
    void mapUsesSnapshotCoordinatesAndOneBulkPlaceQuery() {
        User author = user(2L, "작성자");
        TravelRecord record = record(20L, author, Visibility.PUBLIC);
        var pageable = PageRequest.of(0, 20);
        given(travelRecordRepository.findActivePublic(pageable))
                .willReturn(new PageImpl<>(List.of(record), pageable, 1));
        Place changedOriginal = Place.builder().id(7L).name("변경된 이름")
                .latitude(new BigDecimal("1.0")).longitude(new BigDecimal("2.0")).build();
        TravelRecordPlace snapshot = TravelRecordPlace.builder()
                .id(201L).travelRecord(record).place(changedOriginal).placeName("기록 당시 이름")
                .address("기록 당시 주소").latitude(new BigDecimal("33.12345678"))
                .longitude(new BigDecimal("126.12345678")).visitDate(LocalDate.of(2026, 8, 1))
                .sequenceOrder(1).visited(true).build();
        given(travelRecordPlaceRepository.findAllByRecordIdsInSnapshotOrder(List.of(20L)))
                .willReturn(List.of(snapshot));

        var result = service.getRecords(1L, RecordView.MAP, false, pageable);
        var map = (com.example.jejugilmoa.domain.record.dto.TravelRecordMapResponse) result.content().getFirst();

        assertThat(map.places().getFirst().placeName()).isEqualTo("기록 당시 이름");
        assertThat(map.places().getFirst().latitude()).isEqualByComparingTo("33.12345678");
        assertThat(map.places().getFirst().longitude()).isEqualByComparingTo("126.12345678");
        verify(travelRecordPlaceRepository).findAllByRecordIdsInSnapshotOrder(List.of(20L));
        verify(imageUrlResolver, never()).resolve(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void detailAllowsOwnerPrivateAndReturnsNullPlanWithSeparatedImages() {
        User owner = user(1L, "나");
        TravelRecord record = record(30L, owner, Visibility.PRIVATE);
        given(travelRecordRepository.findActiveByIdWithUserAndPlan(30L)).willReturn(Optional.of(record));
        Place original = Place.builder().id(9L).name("최신 이름").build();
        TravelRecordPlace place = TravelRecordPlace.builder().id(301L).travelRecord(record).place(original)
                .placeName("snapshot").address("snapshot address")
                .latitude(new BigDecimal("33.3")).longitude(new BigDecimal("126.3"))
                .visitDate(LocalDate.of(2026, 8, 2)).sequenceOrder(1).visited(true).memo("메모").build();
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(30L)).willReturn(List.of(place));
        TravelRecordImage recordImage = TravelRecordImage.builder().id(401L).travelRecord(record)
                .objectKey("records/1/general.jpg").sequenceOrder(3).build();
        TravelRecordImage placeImage = TravelRecordImage.builder().id(402L).travelRecord(record)
                .travelRecordPlace(place).objectKey("records/1/place-1.jpg").sequenceOrder(1).build();
        TravelRecordImage secondPlaceImage = TravelRecordImage.builder().id(403L).travelRecord(record)
                .travelRecordPlace(place).objectKey("records/1/place-2.jpg").sequenceOrder(2).build();
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(30L))
                .willReturn(List.of(placeImage, recordImage, secondPlaceImage));
        given(imageUrlResolver.resolve("records/1/general.jpg")).willReturn("https://signed/general");
        given(imageUrlResolver.resolve("records/1/place-1.jpg")).willReturn("https://signed/place-1");
        given(imageUrlResolver.resolve("records/1/place-2.jpg")).willReturn("https://signed/place-2");
        given(travelRecordReactionRepository.countByRecordIdsAndType(List.of(30L))).willReturn(List.of());
        given(travelRecordReactionRepository.findMineByRecordIds(List.of(30L), 1L)).willReturn(List.of());

        var result = service.getDetail(30L, 1L);

        assertThat(result.plan()).isNull();
        assertThat(result.images()).singleElement().satisfies(image ->
                assertThat(image.imageUrl()).isEqualTo("https://signed/general"));
        assertThat(result.imageCount()).isEqualTo(3);
        assertThat(result.allImages())
                .extracting(image -> image.imageId(), image -> image.sequenceOrder())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(402L, 1),
                        org.assertj.core.groups.Tuple.tuple(403L, 2),
                        org.assertj.core.groups.Tuple.tuple(401L, 3));
        assertThat(result.places()).singleElement().satisfies(response -> {
            assertThat(response.placeName()).isEqualTo("snapshot");
            assertThat(response.images()).extracting(image -> image.imageUrl())
                    .containsExactly("https://signed/place-1", "https://signed/place-2");
        });
    }

    @Test
    void detailReturnsPlanWhenItExistsAndAllowsAnotherUsersPublicRecord() {
        User author = user(2L, "타인");
        TravelPlan plan = TravelPlan.builder().id(50L).title("원본 계획").build();
        TravelRecord record = TravelRecord.builder().id(31L).user(author).travelPlan(plan)
                .title("공개").visibility(Visibility.PUBLIC).build();
        given(travelRecordRepository.findActiveByIdWithUserAndPlan(31L)).willReturn(Optional.of(record));
        given(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(31L)).willReturn(List.of());
        given(travelRecordImageRepository.findAllByRecordIdOrderBySequence(31L)).willReturn(List.of());
        given(travelRecordReactionRepository.countByRecordIdsAndType(List.of(31L))).willReturn(List.of());
        given(travelRecordReactionRepository.findMineByRecordIds(List.of(31L), 1L)).willReturn(List.of());

        var result = service.getDetail(31L, 1L);

        assertThat(result.plan().planId()).isEqualTo(50L);
        assertThat(result.plan().title()).isEqualTo("원본 계획");
        assertThat(result.imageCount()).isZero();
        assertThat(result.allImages()).isEmpty();
    }

    @Test
    void detailHidesAnotherUsersPrivateRecordAndMissingRecordAsNotFound() {
        TravelRecord privateRecord = record(40L, user(2L, "타인"), Visibility.PRIVATE);
        given(travelRecordRepository.findActiveByIdWithUserAndPlan(40L))
                .willReturn(Optional.of(privateRecord));
        given(travelRecordRepository.findActiveByIdWithUserAndPlan(404L)).willReturn(Optional.empty());

        assertNotFound(40L);
        assertNotFound(404L);
    }

    private void assertNotFound(Long recordId) {
        assertThatThrownBy(() -> service.getDetail(recordId, 1L))
                .isInstanceOf(GeneralException.class)
                .satisfies(error -> assertThat(((GeneralException) error).getCode())
                        .isEqualTo(RecordErrorCode.RECORD_NOT_FOUND));
    }

    private User user(Long id, String nickname) {
        return User.builder().id(id).nickname(nickname).build();
    }

    private TravelRecord record(Long id, User user, Visibility visibility) {
        return TravelRecord.builder().id(id).user(user).title("기록 " + id).visibility(visibility).build();
    }

    private TravelRecordReactionRepository.ReactionCount reactionCount(
            Long recordId, ReactionType type, Long count) {
        var result = mock(TravelRecordReactionRepository.ReactionCount.class);
        given(result.getRecordId()).willReturn(recordId);
        given(result.getReactionType()).willReturn(type);
        given(result.getCount()).willReturn(count);
        return result;
    }
}
