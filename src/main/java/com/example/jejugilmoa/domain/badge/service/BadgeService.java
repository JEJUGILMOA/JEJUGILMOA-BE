package com.example.jejugilmoa.domain.badge.service;

import com.example.jejugilmoa.domain.badge.converter.BadgeConverter;
import com.example.jejugilmoa.domain.badge.dto.BadgeGroupResponse;
import com.example.jejugilmoa.domain.badge.dto.BadgeItemResponse;
import com.example.jejugilmoa.domain.badge.entity.Badge;
import com.example.jejugilmoa.domain.badge.entity.BadgeCondition;
import com.example.jejugilmoa.domain.badge.entity.BadgeConditionCourseStop;
import com.example.jejugilmoa.domain.badge.enums.BadgeConditionType;
import com.example.jejugilmoa.domain.badge.enums.BadgeGroup;
import com.example.jejugilmoa.domain.badge.repository.BadgeConditionRepository;
import com.example.jejugilmoa.domain.badge.repository.BadgeRepository;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository.CategoryVisitCount;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository.PlaceAddress;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository.PlaceVisitCount;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository.PlaceVisitTime;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.user.entity.UserBadge;
import com.example.jejugilmoa.domain.user.repository.UserBadgeRepository;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final BadgeConditionRepository badgeConditionRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final TravelCourseRepository travelCourseRepository;
    private final TravelPlanRepository travelPlanRepository;

    public List<BadgeGroupResponse> getMyBadges(Long userId) {
        List<Badge> badges = badgeRepository.findAll();
        List<Long> badgeIds = badges.stream().map(Badge::getId).toList();

        List<BadgeCondition> conditions = badgeConditionRepository.findAllByBadgeIdIn(badgeIds);
        Map<Long, List<BadgeCondition>> conditionsByBadgeId = conditions.stream()
                .collect(Collectors.groupingBy(condition -> condition.getBadge().getId()));

        Map<Long, LocalDateTime> acquiredAtByBadgeId = userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(userId).stream()
                .collect(Collectors.toMap(userBadge -> userBadge.getBadge().getId(), UserBadge::getAcquiredAt));

        ProgressLookup lookup = buildProgressLookup(userId, conditions, acquiredAtByBadgeId.keySet(), badges.size());

        Map<BadgeGroup, List<BadgeItemResponse>> grouped = new EnumMap<>(BadgeGroup.class);
        for (BadgeGroup group : BadgeGroup.values()) {
            grouped.put(group, new ArrayList<>());
        }

        for (Badge badge : badges) {
            LocalDateTime acquiredAt = acquiredAtByBadgeId.get(badge.getId());
            boolean acquired = acquiredAt != null;
            Progress progress = calculateProgress(conditionsByBadgeId.getOrDefault(badge.getId(), List.of()), lookup);

            BadgeItemResponse item = BadgeConverter.toItemResponse(
                    badge, acquired, acquiredAt, progress.current(), progress.target());
            grouped.get(badge.getDisplayGroup()).add(item);
        }

        return grouped.entrySet().stream()
                .map(entry -> new BadgeGroupResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 방문 인증/여행 완료 시점에 호출해, 조건을 새로 충족한 뱃지를 지급한다.
     *
     * <p>건너뛴 경유지(skip)는 {@link TravelCourseRepository}의 집계 쿼리에서 이미 제외되므로
     * 여기서는 별도 처리가 필요 없다. 이미 보유한 뱃지는 건너뛰고, 새로 충족한 뱃지만
     * {@code UserBadge}로 저장한다.</p>
     *
     * <p>마스터형(ALL_BADGES) 뱃지는 일반 뱃지 판정이 끝난 뒤, 이번 호출로 새로 지급된
     * 뱃지까지 포함해 "자신을 제외한 전부 보유"를 판정한다 — 마지막 뱃지와 마스터 뱃지를
     * 같은 인증에서 동시에 받을 수 있다.</p>
     *
     * @return 이번 호출로 새로 지급된 뱃지 목록
     */
    @Transactional
    public List<Badge> grantEarnedBadges(Long userId) {
        List<Badge> badges = badgeRepository.findAll();
        List<Long> badgeIds = badges.stream().map(Badge::getId).toList();

        List<BadgeCondition> conditions = badgeConditionRepository.findAllByBadgeIdIn(badgeIds);
        Map<Long, List<BadgeCondition>> conditionsByBadgeId = conditions.stream()
                .collect(Collectors.groupingBy(condition -> condition.getBadge().getId()));

        Set<Long> acquiredBadgeIds = userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(userId).stream()
                .map(userBadge -> userBadge.getBadge().getId())
                .collect(Collectors.toSet());

        ProgressLookup lookup = buildProgressLookup(userId, conditions, acquiredBadgeIds, badges.size());

        List<Badge> newlyEarned = new ArrayList<>();
        List<Badge> masterBadges = new ArrayList<>();
        for (Badge badge : badges) {
            if (acquiredBadgeIds.contains(badge.getId())) {
                continue;
            }
            List<BadgeCondition> badgeConditions = conditionsByBadgeId.getOrDefault(badge.getId(), List.of());
            if (badgeConditions.stream().anyMatch(c -> c.getConditionType() == BadgeConditionType.ALL_BADGES)) {
                masterBadges.add(badge);
                continue;
            }
            boolean satisfied = badgeConditions.stream().anyMatch(condition -> {
                long target = lookup.targetFor(condition);
                return target > 0 && lookup.countFor(condition) >= target;
            });
            if (satisfied) {
                newlyEarned.add(badge);
            }
        }

        for (Badge master : masterBadges) {
            Set<Long> ownedAfter = new HashSet<>(acquiredBadgeIds);
            newlyEarned.forEach(badge -> ownedAfter.add(badge.getId()));
            boolean ownsAllOthers = badges.size() > 1 && badges.stream()
                    .filter(badge -> !badge.getId().equals(master.getId()))
                    .allMatch(badge -> ownedAfter.contains(badge.getId()));
            if (ownsAllOthers) {
                newlyEarned.add(master);
            }
        }

        if (!newlyEarned.isEmpty()) {
            var userRef = userRepository.getReferenceById(userId);
            List<UserBadge> toGrant = newlyEarned.stream()
                    .map(badge -> UserBadge.builder().user(userRef).badge(badge).build())
                    .toList();
            userBadgeRepository.saveAll(toGrant);
        }

        return newlyEarned;
    }

    // 여행 완료 응답에 "이번 여행에서 획득한 뱃지"를 보여주기 위한 조회 — actualStartedAt 이후 획득분만
    public List<UserBadge> getBadgesEarnedSince(Long userId, LocalDateTime since) {
        return userBadgeRepository
                .findByUserIdAndUserDeletedAtIsNullAndAcquiredAtGreaterThanEqualOrderByAcquiredAtAsc(userId, since);
    }

    // 조건을 종류별로 모아 일괄 집계 쿼리를 실행한 뒤 메모리에서 조회하도록 준비한다 (N+1 방지)
    private ProgressLookup buildProgressLookup(
            Long userId, List<BadgeCondition> conditions, Set<Long> acquiredBadgeIds, int totalBadgeCount) {
        // 시간대 조건이 없는 PLACE 조건은 횟수 집계로, 시간대 조건이 있는 PLACE 조건과
        // COURSE 경유지는 인증 시각까지 필요하므로 개별 방문 기록으로 나눠 조회한다.
        Set<Long> countedPlaceIds = conditions.stream()
                .filter(c -> c.getConditionType() == BadgeConditionType.PLACE && c.getTimeStart() == null)
                .map(c -> c.getPlace().getId())
                .collect(Collectors.toSet());
        Set<Long> timedPlaceIds = conditions.stream()
                .filter(c -> c.getConditionType() == BadgeConditionType.PLACE && c.getTimeStart() != null)
                .map(c -> c.getPlace().getId())
                .collect(Collectors.toSet());
        conditions.stream()
                .filter(c -> c.getConditionType() == BadgeConditionType.COURSE)
                .flatMap(c -> c.getCourseStops().stream())
                .forEach(stop -> timedPlaceIds.add(stop.getPlace().getId()));

        Set<Long> categoryIds = conditions.stream()
                .filter(c -> c.getConditionType() == BadgeConditionType.CATEGORY)
                .map(c -> c.getCategory().getId())
                .collect(Collectors.toSet());
        Set<String> regions = conditions.stream()
                .filter(c -> c.getConditionType() == BadgeConditionType.REGION)
                .map(BadgeCondition::getRegion)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean needsTotal = conditions.stream()
                .anyMatch(c -> c.getConditionType() == BadgeConditionType.TOTAL_PLACES);
        boolean needsDiversity = conditions.stream()
                .anyMatch(c -> c.getConditionType() == BadgeConditionType.CATEGORY_DIVERSITY);
        boolean needsTripCount = conditions.stream()
                .anyMatch(c -> c.getConditionType() == BadgeConditionType.TRIP_COUNT);

        Map<Long, Long> visitCountByPlaceId = countedPlaceIds.isEmpty() ? Map.of()
                : travelCourseRepository.countVisitedByUserGroupedByPlace(userId, countedPlaceIds).stream()
                        .collect(Collectors.toMap(PlaceVisitCount::getPlaceId, PlaceVisitCount::getCnt));

        Map<Long, List<LocalDateTime>> visitTimesByPlaceId = timedPlaceIds.isEmpty() ? Map.of()
                : travelCourseRepository.findVisitTimesByUserAndPlaceIds(userId, timedPlaceIds).stream()
                        .collect(Collectors.groupingBy(PlaceVisitTime::getPlaceId,
                                Collectors.mapping(PlaceVisitTime::getVisitedAt, Collectors.toList())));

        Map<Long, Long> distinctPlaceCountByCategoryId = categoryIds.isEmpty() ? Map.of()
                : travelCourseRepository.countDistinctVisitedPlacesByUserGroupedByCategory(userId, categoryIds).stream()
                        .collect(Collectors.toMap(CategoryVisitCount::getCategoryId, CategoryVisitCount::getCnt));

        Map<String, Long> distinctPlaceCountByRegion = Map.of();
        if (!regions.isEmpty()) {
            List<String> visitedAddresses = travelCourseRepository.findDistinctVisitedPlacesByUser(userId).stream()
                    .map(PlaceAddress::getAddress)
                    .filter(Objects::nonNull)
                    .toList();
            Map<String, Long> counts = new HashMap<>();
            for (String region : regions) {
                counts.put(region, visitedAddresses.stream().filter(address -> address.contains(region)).count());
            }
            distinctPlaceCountByRegion = counts;
        }

        long totalDistinctVisited = needsTotal ? travelCourseRepository.countDistinctVisitedPlacesByUser(userId) : 0L;
        long distinctCategoryCount = needsDiversity ? travelCourseRepository.countDistinctVisitedCategoriesByUser(userId) : 0L;
        long completedTripCount = needsTripCount
                ? travelPlanRepository.countByUserIdAndStatus(userId, TravelPlanStatus.COMPLETED) : 0L;

        return new ProgressLookup(visitCountByPlaceId, visitTimesByPlaceId, distinctPlaceCountByCategoryId,
                distinctPlaceCountByRegion, totalDistinctVisited, distinctCategoryCount, completedTripCount,
                acquiredBadgeIds, totalBadgeCount);
    }

    // 배지에 조건이 여러 개 있으면 그 중 가장 진행률이 높은 조건을 대표 진행도로 사용한다
    private Progress calculateProgress(List<BadgeCondition> conditions, ProgressLookup lookup) {
        Progress best = new Progress(0, 0);
        for (BadgeCondition condition : conditions) {
            int target = (int) lookup.targetFor(condition);
            int current = (int) Math.min(lookup.countFor(condition), target);
            if (best.target() == 0 || current * best.target() > best.current() * target) {
                best = new Progress(current, target);
            }
        }
        return best;
    }

    private record Progress(int current, int target) {
    }

    private record ProgressLookup(
            Map<Long, Long> visitCountByPlaceId,
            Map<Long, List<LocalDateTime>> visitTimesByPlaceId,
            Map<Long, Long> distinctPlaceCountByCategoryId,
            Map<String, Long> distinctPlaceCountByRegion,
            long totalDistinctVisited,
            long distinctCategoryCount,
            long completedTripCount,
            Set<Long> acquiredBadgeIds,
            int totalBadgeCount) {

        long countFor(BadgeCondition condition) {
            return switch (condition.getConditionType()) {
                case PLACE -> condition.getTimeStart() != null
                        ? countVisitsInTimeWindow(condition)
                        : visitCountByPlaceId.getOrDefault(condition.getPlace().getId(), 0L);
                case CATEGORY -> distinctPlaceCountByCategoryId.getOrDefault(condition.getCategory().getId(), 0L);
                case REGION -> distinctPlaceCountByRegion.getOrDefault(condition.getRegion(), 0L);
                case TOTAL_PLACES -> totalDistinctVisited;
                case CATEGORY_DIVERSITY -> distinctCategoryCount;
                case TRIP_COUNT -> completedTripCount;
                case COURSE -> countCourseStepsInOrder(condition);
                case ALL_BADGES -> acquiredBadgeIds.stream()
                        .filter(badgeId -> !badgeId.equals(condition.getBadge().getId()))
                        .count();
            };
        }

        long targetFor(BadgeCondition condition) {
            return switch (condition.getConditionType()) {
                case COURSE -> condition.getCourseStops().size();
                case ALL_BADGES -> totalBadgeCount - 1L;
                default -> condition.getVisitCount();
            };
        }

        private long countVisitsInTimeWindow(BadgeCondition condition) {
            return visitTimesByPlaceId.getOrDefault(condition.getPlace().getId(), List.of()).stream()
                    .map(LocalDateTime::toLocalTime)
                    .filter(time -> isWithinWindow(time, condition.getTimeStart(), condition.getTimeEnd()))
                    .count();
        }

        // timeStart > timeEnd이면 자정을 넘는 구간(예: 별빛 20:00~05:00)으로 해석한다
        private static boolean isWithinWindow(LocalTime time, LocalTime start, LocalTime end) {
            if (!start.isAfter(end)) {
                return !time.isBefore(start) && !time.isAfter(end);
            }
            return !time.isBefore(start) || !time.isAfter(end);
        }

        // 코스 경유지를 순서대로 인증한 연속 구간 길이 — 각 경유지의 최초 인증 시각이 앞 경유지 이후여야 한다
        private long countCourseStepsInOrder(BadgeCondition condition) {
            LocalDateTime previous = LocalDateTime.MIN;
            long completed = 0;
            for (BadgeConditionCourseStop stop : condition.getCourseStops()) {
                LocalDateTime firstVisit = visitTimesByPlaceId
                        .getOrDefault(stop.getPlace().getId(), List.of()).stream()
                        .min(Comparator.naturalOrder())
                        .orElse(null);
                if (firstVisit == null || firstVisit.isBefore(previous)) {
                    break;
                }
                completed++;
                previous = firstVisit;
            }
            return completed;
        }
    }
}
