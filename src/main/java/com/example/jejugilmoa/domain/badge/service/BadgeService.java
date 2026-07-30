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
import com.example.jejugilmoa.domain.user.entity.UserBadge;
import com.example.jejugilmoa.domain.user.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

        Map<Long, List<BadgeCondition>> conditionsByBadgeId = badgeConditionRepository
                .findAllByBadgeIdIn(badgeIds).stream()
                .collect(Collectors.groupingBy(condition -> condition.getBadge().getId()));

        Map<Long, LocalDateTime> acquiredAtByBadgeId = userBadgeRepository.findByUserIdAndUserDeletedAtIsNull(userId).stream()
                .collect(Collectors.toMap(userBadge -> userBadge.getBadge().getId(), UserBadge::getAcquiredAt));

        Map<BadgeGroup, List<BadgeItemResponse>> grouped = new EnumMap<>(BadgeGroup.class);
        for (BadgeGroup group : BadgeGroup.values()) {
            grouped.put(group, new ArrayList<>());
        }

        for (Badge badge : badges) {
            LocalDateTime acquiredAt = acquiredAtByBadgeId.get(badge.getId());
            boolean acquired = acquiredAt != null;
            Progress progress = calculateProgress(userId, conditionsByBadgeId.getOrDefault(badge.getId(), List.of()));

            BadgeItemResponse item = BadgeConverter.toItemResponse(
                    badge, acquired, acquiredAt, progress.current(), progress.target());
            grouped.get(badge.getDisplayGroup()).add(item);
        }

        return grouped.entrySet().stream()
                .map(entry -> new BadgeGroupResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    // 배지에 조건이 여러 개 있으면 그 중 가장 진행률이 높은 조건을 대표 진행도로 사용한다
    private Progress calculateProgress(Long userId, List<BadgeCondition> conditions) {
        Progress best = new Progress(0, 0);
        for (BadgeCondition condition : conditions) {
            int target = condition.getVisitCount();
            int current = (int) Math.min(countProgress(userId, condition), target);
            if (best.target() == 0 || current * best.target() > best.current() * target) {
                best = new Progress(current, target);
            }
        }
        return best;
    }

    private long countProgress(Long userId, BadgeCondition condition) {
        if (condition.getPlace() != null) {
            return travelRecordPlaceRepository.countVisitedByUserAndPlace(userId, condition.getPlace().getId());
        }
        if (condition.getCategory() != null) {
            return travelRecordPlaceRepository.countDistinctVisitedPlacesByUserAndCategory(
                    userId, condition.getCategory().getId());
        }
        if (condition.getRegion() != null) {
            return travelRecordPlaceRepository.countDistinctVisitedPlacesByUserAndRegion(
                    userId, condition.getRegion());
        }
        return travelRecordPlaceRepository.countDistinctVisitedPlacesByUser(userId);
    }

    private record Progress(int current, int target) {
    }
}
