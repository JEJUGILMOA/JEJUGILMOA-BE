package com.example.jejugilmoa.domain.badge.service;

import com.example.jejugilmoa.domain.badge.converter.BadgeConverter;
import com.example.jejugilmoa.domain.badge.dto.BadgeGroupResponse;
import com.example.jejugilmoa.domain.badge.dto.BadgeItemResponse;
import com.example.jejugilmoa.domain.badge.entity.Badge;
import com.example.jejugilmoa.domain.badge.entity.BadgeCondition;
import com.example.jejugilmoa.domain.badge.enums.BadgeGroup;
import com.example.jejugilmoa.domain.badge.repository.BadgeConditionRepository;
import com.example.jejugilmoa.domain.badge.repository.BadgeRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository.CategoryVisitCount;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository.PlaceAddress;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository.PlaceVisitCount;
import com.example.jejugilmoa.domain.user.entity.UserBadge;
import com.example.jejugilmoa.domain.user.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
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
    private final TravelRecordPlaceRepository travelRecordPlaceRepository;

    public List<BadgeGroupResponse> getMyBadges(Long userId) {
        List<Badge> badges = badgeRepository.findAll();
        List<Long> badgeIds = badges.stream().map(Badge::getId).toList();

        List<BadgeCondition> conditions = badgeConditionRepository.findAllByBadgeIdIn(badgeIds);
        Map<Long, List<BadgeCondition>> conditionsByBadgeId = conditions.stream()
                .collect(Collectors.groupingBy(condition -> condition.getBadge().getId()));

        Map<Long, LocalDateTime> acquiredAtByBadgeId = userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(userId).stream()
                .collect(Collectors.toMap(userBadge -> userBadge.getBadge().getId(), UserBadge::getAcquiredAt));

        ProgressLookup lookup = buildProgressLookup(userId, conditions);

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

    // 조건을 종류별(장소/카테고리/지역/전체)로 모아 일괄 집계 쿼리를 실행한 뒤 메모리에서 조회하도록 준비한다 (N+1 방지)
    private ProgressLookup buildProgressLookup(Long userId, List<BadgeCondition> conditions) {
        Set<Long> placeIds = conditions.stream()
                .map(BadgeCondition::getPlace).filter(Objects::nonNull)
                .map(place -> place.getId())
                .collect(Collectors.toSet());
        Set<Long> categoryIds = conditions.stream()
                .map(BadgeCondition::getCategory).filter(Objects::nonNull)
                .map(category -> category.getId())
                .collect(Collectors.toSet());
        Set<String> regions = conditions.stream()
                .map(BadgeCondition::getRegion).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean needsTotal = conditions.stream()
                .anyMatch(condition -> condition.getPlace() == null
                        && condition.getCategory() == null
                        && condition.getRegion() == null);

        Map<Long, Long> visitCountByPlaceId = placeIds.isEmpty() ? Map.of()
                : travelRecordPlaceRepository.countVisitedByUserGroupedByPlace(userId, placeIds).stream()
                        .collect(Collectors.toMap(PlaceVisitCount::getPlaceId, PlaceVisitCount::getCnt));

        Map<Long, Long> distinctPlaceCountByCategoryId = categoryIds.isEmpty() ? Map.of()
                : travelRecordPlaceRepository.countDistinctVisitedPlacesByUserGroupedByCategory(userId, categoryIds).stream()
                        .collect(Collectors.toMap(CategoryVisitCount::getCategoryId, CategoryVisitCount::getCnt));

        Map<String, Long> distinctPlaceCountByRegion = Map.of();
        if (!regions.isEmpty()) {
            List<String> visitedAddresses = travelRecordPlaceRepository.findDistinctVisitedPlacesByUser(userId).stream()
                    .map(PlaceAddress::getAddress)
                    .filter(Objects::nonNull)
                    .toList();
            Map<String, Long> counts = new HashMap<>();
            for (String region : regions) {
                counts.put(region, visitedAddresses.stream().filter(address -> address.contains(region)).count());
            }
            distinctPlaceCountByRegion = counts;
        }

        long totalDistinctVisited = needsTotal ? travelRecordPlaceRepository.countDistinctVisitedPlacesByUser(userId) : 0L;

        return new ProgressLookup(visitCountByPlaceId, distinctPlaceCountByCategoryId, distinctPlaceCountByRegion, totalDistinctVisited);
    }

    // 배지에 조건이 여러 개 있으면 그 중 가장 진행률이 높은 조건을 대표 진행도로 사용한다
    private Progress calculateProgress(List<BadgeCondition> conditions, ProgressLookup lookup) {
        Progress best = new Progress(0, 0);
        for (BadgeCondition condition : conditions) {
            int target = condition.getVisitCount();
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
            Map<Long, Long> distinctPlaceCountByCategoryId,
            Map<String, Long> distinctPlaceCountByRegion,
            long totalDistinctVisited) {

        long countFor(BadgeCondition condition) {
            if (condition.getPlace() != null) {
                return visitCountByPlaceId.getOrDefault(condition.getPlace().getId(), 0L);
            }
            if (condition.getCategory() != null) {
                return distinctPlaceCountByCategoryId.getOrDefault(condition.getCategory().getId(), 0L);
            }
            if (condition.getRegion() != null) {
                return distinctPlaceCountByRegion.getOrDefault(condition.getRegion(), 0L);
            }
            return totalDistinctVisited;
        }
    }
}
