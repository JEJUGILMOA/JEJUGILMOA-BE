package com.example.jejugilmoa.domain.badge.service;

import com.example.jejugilmoa.domain.badge.entity.Badge;
import com.example.jejugilmoa.domain.badge.entity.BadgeCondition;
import com.example.jejugilmoa.domain.badge.entity.BadgeConditionCourseStop;
import com.example.jejugilmoa.domain.badge.enums.BadgeConditionType;
import com.example.jejugilmoa.domain.badge.enums.BadgeGroup;
import com.example.jejugilmoa.domain.badge.enums.BadgeType;
import com.example.jejugilmoa.domain.badge.repository.BadgeConditionRepository;
import com.example.jejugilmoa.domain.badge.repository.BadgeRepository;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.entity.UserBadge;
import com.example.jejugilmoa.domain.user.repository.UserBadgeRepository;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    private static final Long USER_ID = 11L;
    private static final Long PLACE_ID = 42L;

    @Mock BadgeRepository badgeRepository;
    @Mock BadgeConditionRepository badgeConditionRepository;
    @Mock UserBadgeRepository userBadgeRepository;
    @Mock UserRepository userRepository;
    @Mock TravelCourseRepository travelCourseRepository;
    @Mock TravelPlanRepository travelPlanRepository;
    @InjectMocks BadgeService badgeService;

    private Badge badge(Long id, String name) {
        return Badge.builder().id(id).name(name).badgeType(BadgeType.PLACE).displayGroup(BadgeGroup.EXPLORATION).build();
    }

    private BadgeCondition placeCondition(Badge badge, Long placeId, int visitCount) {
        Place place = Place.builder().id(placeId).build();
        return BadgeCondition.builder().id(1L).badge(badge)
                .conditionType(BadgeConditionType.PLACE).place(place).visitCount(visitCount).build();
    }

    private BadgeCondition timedPlaceCondition(Badge badge, Long placeId, LocalTime start, LocalTime end) {
        Place place = Place.builder().id(placeId).build();
        return BadgeCondition.builder().id(1L).badge(badge)
                .conditionType(BadgeConditionType.PLACE).place(place).visitCount(1)
                .timeStart(start).timeEnd(end).build();
    }

    private TravelCourseRepository.PlaceVisitCount placeVisitCount(Long placeId, long cnt) {
        return new TravelCourseRepository.PlaceVisitCount() {
            public Long getPlaceId() { return placeId; }
            public Long getCnt() { return cnt; }
        };
    }

    private TravelCourseRepository.PlaceVisitTime placeVisitTime(Long placeId, LocalDateTime visitedAt) {
        return new TravelCourseRepository.PlaceVisitTime() {
            public Long getPlaceId() { return placeId; }
            public LocalDateTime getVisitedAt() { return visitedAt; }
        };
    }

    @Test
    void grantEarnedBadges_grantsBadge_whenPlaceConditionSatisfied() {
        Badge badge = badge(1L, "애월 단골");
        BadgeCondition condition = placeCondition(badge, PLACE_ID, 1);
        User userRef = User.builder().id(USER_ID).build();

        given(badgeRepository.findAll()).willReturn(List.of(badge));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L))).willReturn(List.of(condition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelCourseRepository.countVisitedByUserGroupedByPlace(USER_ID, Set.of(PLACE_ID)))
                .willReturn(List.of(placeVisitCount(PLACE_ID, 1L)));
        given(userRepository.getReferenceById(USER_ID)).willReturn(userRef);

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).containsExactly(badge);
        ArgumentCaptor<List<UserBadge>> captor = ArgumentCaptor.forClass(List.class);
        verify(userBadgeRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getBadge()).isEqualTo(badge);
        assertThat(captor.getValue().get(0).getUser()).isEqualTo(userRef);
    }

    @Test
    void grantEarnedBadges_doesNotGrant_whenConditionNotMet() {
        Badge badge = badge(1L, "애월 단골");
        BadgeCondition condition = placeCondition(badge, PLACE_ID, 3);

        given(badgeRepository.findAll()).willReturn(List.of(badge));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L))).willReturn(List.of(condition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelCourseRepository.countVisitedByUserGroupedByPlace(USER_ID, Set.of(PLACE_ID)))
                .willReturn(List.of(placeVisitCount(PLACE_ID, 1L)));

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).isEmpty();
        verify(userBadgeRepository, never()).saveAll(anyCollection());
    }

    @Test
    void grantEarnedBadges_skipsAlreadyAcquiredBadge() {
        Badge badge = badge(1L, "애월 단골");
        BadgeCondition condition = placeCondition(badge, PLACE_ID, 1);
        UserBadge alreadyAcquired = UserBadge.builder().id(9L)
                .user(User.builder().id(USER_ID).build()).badge(badge).build();

        given(badgeRepository.findAll()).willReturn(List.of(badge));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L))).willReturn(List.of(condition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of(alreadyAcquired));

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).isEmpty();
        verify(userBadgeRepository, never()).saveAll(anyCollection());
    }

    @Test
    void grantEarnedBadges_grantsMultipleBadges_returnsAllAsList() {
        Badge badgeA = badge(1L, "애월 단골");
        Badge badgeB = badge(2L, "성산 탐험가");
        BadgeCondition conditionA = placeCondition(badgeA, PLACE_ID, 1);
        BadgeCondition conditionB = placeCondition(badgeB, 43L, 1);
        User userRef = User.builder().id(USER_ID).build();

        given(badgeRepository.findAll()).willReturn(List.of(badgeA, badgeB));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L, 2L)))
                .willReturn(List.of(conditionA, conditionB));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelCourseRepository.countVisitedByUserGroupedByPlace(USER_ID, Set.of(PLACE_ID, 43L)))
                .willReturn(List.of(placeVisitCount(PLACE_ID, 1L), placeVisitCount(43L, 1L)));
        given(userRepository.getReferenceById(USER_ID)).willReturn(userRef);

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).containsExactlyInAnyOrder(badgeA, badgeB);
    }

    @Test
    void grantEarnedBadges_grantsTimedBadge_whenVisitInsideTimeWindow() {
        Badge badge = badge(1L, "일출 감상자");
        BadgeCondition condition = timedPlaceCondition(badge, PLACE_ID, LocalTime.of(5, 0), LocalTime.of(9, 0));
        User userRef = User.builder().id(USER_ID).build();

        given(badgeRepository.findAll()).willReturn(List.of(badge));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L))).willReturn(List.of(condition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelCourseRepository.findVisitTimesByUserAndPlaceIds(USER_ID, Set.of(PLACE_ID)))
                .willReturn(List.of(placeVisitTime(PLACE_ID, LocalDateTime.of(2026, 9, 1, 6, 30))));
        given(userRepository.getReferenceById(USER_ID)).willReturn(userRef);

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).containsExactly(badge);
    }

    @Test
    void grantEarnedBadges_doesNotGrantTimedBadge_whenVisitOutsideTimeWindow() {
        Badge badge = badge(1L, "일출 감상자");
        BadgeCondition condition = timedPlaceCondition(badge, PLACE_ID, LocalTime.of(5, 0), LocalTime.of(9, 0));

        given(badgeRepository.findAll()).willReturn(List.of(badge));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L))).willReturn(List.of(condition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelCourseRepository.findVisitTimesByUserAndPlaceIds(USER_ID, Set.of(PLACE_ID)))
                .willReturn(List.of(placeVisitTime(PLACE_ID, LocalDateTime.of(2026, 9, 1, 14, 0))));

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).isEmpty();
        verify(userBadgeRepository, never()).saveAll(anyCollection());
    }

    @Test
    void grantEarnedBadges_grantsNightBadge_whenWindowWrapsMidnight() {
        Badge badge = badge(1L, "별빛 헌터");
        BadgeCondition condition = timedPlaceCondition(badge, PLACE_ID, LocalTime.of(20, 0), LocalTime.of(5, 0));
        User userRef = User.builder().id(USER_ID).build();

        given(badgeRepository.findAll()).willReturn(List.of(badge));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L))).willReturn(List.of(condition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelCourseRepository.findVisitTimesByUserAndPlaceIds(USER_ID, Set.of(PLACE_ID)))
                .willReturn(List.of(placeVisitTime(PLACE_ID, LocalDateTime.of(2026, 9, 2, 1, 30))));
        given(userRepository.getReferenceById(USER_ID)).willReturn(userRef);

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).containsExactly(badge);
    }

    @Test
    void grantEarnedBadges_grantsTripCountBadge_whenCompletedTripsReachTarget() {
        Badge badge = badge(1L, "제주 사랑꾼");
        BadgeCondition condition = BadgeCondition.builder().id(1L).badge(badge)
                .conditionType(BadgeConditionType.TRIP_COUNT).visitCount(2).build();
        User userRef = User.builder().id(USER_ID).build();

        given(badgeRepository.findAll()).willReturn(List.of(badge));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L))).willReturn(List.of(condition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelPlanRepository.countByUserIdAndStatus(USER_ID, TravelPlanStatus.COMPLETED)).willReturn(2L);
        given(userRepository.getReferenceById(USER_ID)).willReturn(userRef);

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).containsExactly(badge);
    }

    @Test
    void grantEarnedBadges_grantsDiversityBadge_whenDistinctCategoriesReachTarget() {
        Badge badge = badge(1L, "여행 마니아");
        BadgeCondition condition = BadgeCondition.builder().id(1L).badge(badge)
                .conditionType(BadgeConditionType.CATEGORY_DIVERSITY).visitCount(3).build();
        User userRef = User.builder().id(USER_ID).build();

        given(badgeRepository.findAll()).willReturn(List.of(badge));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L))).willReturn(List.of(condition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelCourseRepository.countDistinctVisitedCategoriesByUser(USER_ID)).willReturn(3L);
        given(userRepository.getReferenceById(USER_ID)).willReturn(userRef);

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).containsExactly(badge);
    }

    @Test
    void grantEarnedBadges_grantsCourseBadge_whenAllStopsVisitedInOrder() {
        Badge badge = badge(1L, "트레킹 마스터");
        BadgeCondition condition = courseCondition(badge, 101L, 102L, 103L);
        User userRef = User.builder().id(USER_ID).build();

        given(badgeRepository.findAll()).willReturn(List.of(badge));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L))).willReturn(List.of(condition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelCourseRepository.findVisitTimesByUserAndPlaceIds(USER_ID, Set.of(101L, 102L, 103L)))
                .willReturn(List.of(
                        placeVisitTime(101L, LocalDateTime.of(2026, 9, 1, 8, 0)),
                        placeVisitTime(102L, LocalDateTime.of(2026, 9, 1, 10, 0)),
                        placeVisitTime(103L, LocalDateTime.of(2026, 9, 1, 12, 0))));
        given(userRepository.getReferenceById(USER_ID)).willReturn(userRef);

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).containsExactly(badge);
    }

    @Test
    void grantEarnedBadges_doesNotGrantCourseBadge_whenStopsVisitedOutOfOrder() {
        Badge badge = badge(1L, "트레킹 마스터");
        BadgeCondition condition = courseCondition(badge, 101L, 102L, 103L);

        given(badgeRepository.findAll()).willReturn(List.of(badge));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L))).willReturn(List.of(condition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelCourseRepository.findVisitTimesByUserAndPlaceIds(USER_ID, Set.of(101L, 102L, 103L)))
                .willReturn(List.of(
                        placeVisitTime(101L, LocalDateTime.of(2026, 9, 1, 12, 0)),
                        placeVisitTime(102L, LocalDateTime.of(2026, 9, 1, 10, 0)),
                        placeVisitTime(103L, LocalDateTime.of(2026, 9, 1, 8, 0))));

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).isEmpty();
        verify(userBadgeRepository, never()).saveAll(anyCollection());
    }

    @Test
    void grantEarnedBadges_grantsMasterBadge_togetherWithLastRegularBadge() {
        Badge owned = badge(1L, "애월 단골");
        Badge lastRegular = badge(2L, "성산 탐험가");
        Badge master = badge(3L, "제주 마스터");
        BadgeCondition ownedCondition = placeCondition(owned, PLACE_ID, 1);
        BadgeCondition lastCondition = placeCondition(lastRegular, 43L, 1);
        BadgeCondition masterCondition = BadgeCondition.builder().id(3L).badge(master)
                .conditionType(BadgeConditionType.ALL_BADGES).build();
        UserBadge alreadyAcquired = UserBadge.builder().id(9L)
                .user(User.builder().id(USER_ID).build()).badge(owned).build();
        User userRef = User.builder().id(USER_ID).build();

        given(badgeRepository.findAll()).willReturn(List.of(owned, lastRegular, master));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L, 2L, 3L)))
                .willReturn(List.of(ownedCondition, lastCondition, masterCondition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of(alreadyAcquired));
        given(travelCourseRepository.countVisitedByUserGroupedByPlace(USER_ID, Set.of(PLACE_ID, 43L)))
                .willReturn(List.of(placeVisitCount(PLACE_ID, 1L), placeVisitCount(43L, 1L)));
        given(userRepository.getReferenceById(USER_ID)).willReturn(userRef);

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).containsExactlyInAnyOrder(lastRegular, master);
    }

    @Test
    void grantEarnedBadges_doesNotGrantMasterBadge_whenOtherBadgesRemain() {
        Badge notOwned = badge(1L, "애월 단골");
        Badge master = badge(2L, "제주 마스터");
        BadgeCondition notOwnedCondition = placeCondition(notOwned, PLACE_ID, 3);
        BadgeCondition masterCondition = BadgeCondition.builder().id(2L).badge(master)
                .conditionType(BadgeConditionType.ALL_BADGES).build();

        given(badgeRepository.findAll()).willReturn(List.of(notOwned, master));
        given(badgeConditionRepository.findAllByBadgeIdIn(List.of(1L, 2L)))
                .willReturn(List.of(notOwnedCondition, masterCondition));
        given(userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(USER_ID)).willReturn(List.of());
        given(travelCourseRepository.countVisitedByUserGroupedByPlace(USER_ID, Set.of(PLACE_ID)))
                .willReturn(List.of(placeVisitCount(PLACE_ID, 1L)));

        List<Badge> result = badgeService.grantEarnedBadges(USER_ID);

        assertThat(result).isEmpty();
        verify(userBadgeRepository, never()).saveAll(anyCollection());
    }

    @Test
    void getBadgesEarnedSince_delegatesToRepository() {
        LocalDateTime since = LocalDateTime.of(2026, 7, 20, 9, 0);
        UserBadge earned = UserBadge.builder().id(1L)
                .user(User.builder().id(USER_ID).build()).badge(badge(1L, "애월 단골")).build();
        given(userBadgeRepository
                .findByUserIdAndUserDeletedAtIsNullAndAcquiredAtGreaterThanEqualOrderByAcquiredAtAsc(USER_ID, since))
                .willReturn(List.of(earned));

        List<UserBadge> result = badgeService.getBadgesEarnedSince(USER_ID, since);

        assertThat(result).containsExactly(earned);
    }

    private BadgeCondition courseCondition(Badge badge, Long... orderedPlaceIds) {
        BadgeCondition condition = BadgeCondition.builder().id(1L).badge(badge)
                .conditionType(BadgeConditionType.COURSE).build();
        for (int i = 0; i < orderedPlaceIds.length; i++) {
            condition.getCourseStops().add(BadgeConditionCourseStop.builder()
                    .badgeCondition(condition)
                    .place(Place.builder().id(orderedPlaceIds[i]).build())
                    .stepOrder(i + 1)
                    .build());
        }
        return condition;
    }
}
