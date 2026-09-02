package com.example.jejugilmoa.global.init;

import com.example.jejugilmoa.domain.badge.entity.Badge;
import com.example.jejugilmoa.domain.badge.entity.BadgeCondition;
import com.example.jejugilmoa.domain.badge.entity.BadgeConditionCourseStop;
import com.example.jejugilmoa.domain.badge.enums.BadgeConditionType;
import com.example.jejugilmoa.domain.badge.enums.BadgeGroup;
import com.example.jejugilmoa.domain.badge.enums.BadgeType;
import com.example.jejugilmoa.domain.badge.repository.BadgeConditionRepository;
import com.example.jejugilmoa.domain.badge.repository.BadgeRepository;
import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 제주도 탐험 뱃지 20종 시드.
 *
 * <p>place는 TourAPI 동기화로 채워져 환경마다 ID가 달라, 마이그레이션 대신 부팅 시
 * 장소명으로 매칭해 멱등하게 시드한다. 아직 동기화되지 않아 매칭에 실패한 장소 조건은
 * 경고 로그만 남기고, 다음 부팅 때 다시 시도된다.</p>
 */
@Slf4j
@Component
@Order(2) // DataInitializer(카테고리 시드) 이후 실행 — 카페 카테고리 조건이 필요하다
@RequiredArgsConstructor
public class BadgeDataInitializer implements ApplicationRunner {

    private final BadgeRepository badgeRepository;
    private final BadgeConditionRepository badgeConditionRepository;
    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;

    // 시간조건형 뱃지 인증 시간대 (KST 기준, 계절별 일출/일몰 시각을 넉넉히 포함)
    private static final LocalTime SUNRISE_START = LocalTime.of(5, 0);
    private static final LocalTime SUNRISE_END = LocalTime.of(9, 0);
    private static final LocalTime SUNSET_START = LocalTime.of(17, 0);
    private static final LocalTime SUNSET_END = LocalTime.of(20, 30);
    private static final LocalTime NIGHT_START = LocalTime.of(20, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(5, 0); // 자정을 넘는 구간

    private static final List<BadgeSpec> BADGE_SPECS = List.of(
            new BadgeSpec("바다 사랑꾼", "제주 바다 명소를 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(placeKeyword("해수욕장"), placeKeyword("해변"))),
            new BadgeSpec("한라산 정복자", "한라산 등반을 완료했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(places("한라산 백록담"))),
            new BadgeSpec("폭포 탐험가", "제주 대표 폭포를 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(placeKeyword("폭포"))),
            new BadgeSpec("일출 감상자", "제주 일출 명소에서 아침을 맞이했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(placesAtTime(SUNRISE_START, SUNRISE_END,
                            "일출랜드", "해맞이해안로", "소천지", "지미봉", "종달리해변", "우도"))),
            new BadgeSpec("일몰 감상자", "제주 일몰 명소에서 노을을 감상했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(placesAtTime(SUNSET_START, SUNSET_END,
                            "노을해안로", "수월봉과 차귀해안", "수월봉", "신창풍차해안도로", "형제해안도로",
                            "용담해안도로", "곽지해수욕장", "협재해수욕장", "금능포구", "한담해변",
                            "도두봉", "알작지", "썬셋클리프", "제주 서귀포 산방산", "용머리해안"))),
            new BadgeSpec("돌하르방 친구", "돌하르방 명소를 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(places("돌하르방미술관"))),
            new BadgeSpec("귤밭 탐방자", "귤밭 명소를 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(places("아날로그감귤밭", "귤향기 감귤체험농장"))),
            new BadgeSpec("유채꽃 애호가", "유채꽃 명소를 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(placeKeyword("유채"))),
            new BadgeSpec("별빛 헌터", "제주 밤하늘 명소에서 별을 감상했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(placesAtTime(NIGHT_START, NIGHT_END,
                            "제주별빛누리공원", "서귀포천문과학문화관", "새별오름"))),
            // TODO: '바람의 언덕' 전용 스팟이 place 데이터에 없어 풍차/바람 테마 장소로 임시 매핑 — 실제 스팟 확정 시 교체
            new BadgeSpec("바람의 언덕", "바람 명소를 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(places("신창풍차해안도로", "상가리야자숲"))),
            new BadgeSpec("말과의 추억", "승마 체험 장소를 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(places("도치돌목장", "성이시돌목장"))),
            new BadgeSpec("서퍼 라이더", "서핑 명소를 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(places("중문색달해수욕장", "이호테우해변", "협재해수욕장", "곽지해수욕장"))),
            // TODO: 트레킹 코스 데이터가 확정되지 않아 영실 코스(영실→윗세오름→백록담)를 예시로 시드 — 확정 시 교체
            new BadgeSpec("트레킹 마스터", "트레킹 코스를 완주했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(course("한라산 영실", "윗세오름", "한라산 백록담"))),
            new BadgeSpec("오름 탐험가", "오름(기생화산)을 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(placeKeyword("오름"))),
            new BadgeSpec("용암 탐험가", "용암 동굴을 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(places("만장굴", "미천굴"))),
            new BadgeSpec("카페 투어러", "제주 유명 카페 4곳을 방문했어요.", BadgeType.CATEGORY, BadgeGroup.GOURMET,
                    List.of(category("카페", 4))),
            // TODO: 포토스팟 카테고리가 없어 대표 포토 스팟 장소로 임시 매핑 — 스팟 목록 확정 시 교체
            new BadgeSpec("포토 그래퍼", "포토 스팟을 방문했어요.", BadgeType.PLACE, BadgeGroup.EXPLORATION,
                    List.of(places("도두봉", "새별오름", "용머리해안", "한라산둘레길/사려니숲길"))),
            new BadgeSpec("여행 마니아", "서로 다른 카테고리 3곳을 탐방했어요.", BadgeType.CATEGORY, BadgeGroup.SOCIAL,
                    List.of(categoryDiversity(3))),
            new BadgeSpec("제주 사랑꾼", "제주를 다시 찾아 여행을 2회 완료했어요.", BadgeType.REGION, BadgeGroup.SOCIAL,
                    List.of(tripCount(2))),
            new BadgeSpec("제주 마스터", "모든 탐험 뱃지를 획득했어요.", BadgeType.REGION, BadgeGroup.SOCIAL,
                    List.of(allBadges()))
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (BadgeSpec spec : BADGE_SPECS) {
            Badge badge = badgeRepository.findByName(spec.name())
                    .orElseGet(() -> {
                        log.info("뱃지 초기화: {}", spec.name());
                        return badgeRepository.save(Badge.builder()
                                .name(spec.name())
                                .description(spec.description())
                                .badgeType(spec.badgeType())
                                .displayGroup(spec.group())
                                .build());
                    });

            List<BadgeCondition> existing = badgeConditionRepository.findAllByBadgeId(badge.getId());
            for (ConditionSpec conditionSpec : spec.conditions()) {
                seedCondition(badge, conditionSpec, existing);
            }
        }
    }

    private void seedCondition(Badge badge, ConditionSpec spec, List<BadgeCondition> existing) {
        switch (spec.type()) {
            case PLACE -> seedPlaceConditions(badge, spec, existing);
            case CATEGORY -> seedCategoryCondition(badge, spec, existing);
            case COURSE -> seedCourseCondition(badge, spec, existing);
            // 단순 카운트형 — 같은 유형의 조건이 없으면 하나 추가
            default -> {
                if (existing.stream().noneMatch(c -> c.getConditionType() == spec.type())) {
                    badgeConditionRepository.save(BadgeCondition.builder()
                            .badge(badge)
                            .conditionType(spec.type())
                            .visitCount(spec.visitCount())
                            .build());
                }
            }
        }
    }

    // 장소명(정확 일치 목록 또는 부분 일치 키워드)으로 place를 해석해 장소당 조건 1건씩 추가한다 (OR 판정)
    private void seedPlaceConditions(Badge badge, ConditionSpec spec, List<BadgeCondition> existing) {
        List<Place> places;
        if (spec.placeNameKeyword() != null) {
            places = placeRepository.findAllByNameContaining(spec.placeNameKeyword());
        } else {
            places = placeRepository.findAllByNameIn(spec.placeNames());
            Set<String> foundNames = places.stream().map(Place::getName).collect(Collectors.toSet());
            List<String> missing = spec.placeNames().stream()
                    .filter(name -> !foundNames.contains(name))
                    .toList();
            if (!missing.isEmpty()) {
                log.warn("뱃지 '{}' 조건 장소 미존재 (place 동기화 후 재시도됨): {}", badge.getName(), missing);
            }
        }

        Set<Long> existingPlaceIds = existing.stream()
                .filter(c -> c.getConditionType() == BadgeConditionType.PLACE)
                .map(c -> c.getPlace().getId())
                .collect(Collectors.toSet());
        for (Place place : places) {
            if (existingPlaceIds.contains(place.getId())) {
                continue;
            }
            badgeConditionRepository.save(BadgeCondition.builder()
                    .badge(badge)
                    .conditionType(BadgeConditionType.PLACE)
                    .place(place)
                    .visitCount(spec.visitCount())
                    .timeStart(spec.timeStart())
                    .timeEnd(spec.timeEnd())
                    .build());
        }
    }

    private void seedCategoryCondition(Badge badge, ConditionSpec spec, List<BadgeCondition> existing) {
        Category category = categoryRepository.findByName(spec.categoryName()).orElse(null);
        if (category == null) {
            log.warn("뱃지 '{}' 조건 카테고리 미존재: {}", badge.getName(), spec.categoryName());
            return;
        }
        boolean alreadySeeded = existing.stream()
                .anyMatch(c -> c.getConditionType() == BadgeConditionType.CATEGORY
                        && c.getCategory().getId().equals(category.getId()));
        if (!alreadySeeded) {
            badgeConditionRepository.save(BadgeCondition.builder()
                    .badge(badge)
                    .conditionType(BadgeConditionType.CATEGORY)
                    .category(category)
                    .visitCount(spec.visitCount())
                    .build());
        }
    }

    // 코스 경유지가 하나라도 미존재하면 부분 코스가 되지 않도록 전체를 보류한다
    private void seedCourseCondition(Badge badge, ConditionSpec spec, List<BadgeCondition> existing) {
        if (existing.stream().anyMatch(c -> c.getConditionType() == BadgeConditionType.COURSE)) {
            return;
        }
        List<Place> resolved = spec.courseStopNames().stream()
                .map(name -> placeRepository.findAllByNameIn(List.of(name)).stream().findFirst().orElse(null))
                .toList();
        if (resolved.stream().anyMatch(Objects::isNull)) {
            log.warn("뱃지 '{}' 코스 경유지 미존재 (place 동기화 후 재시도됨): {}", badge.getName(), spec.courseStopNames());
            return;
        }
        BadgeCondition condition = BadgeCondition.builder()
                .badge(badge)
                .conditionType(BadgeConditionType.COURSE)
                .build();
        List<BadgeConditionCourseStop> stops = new ArrayList<>();
        for (int i = 0; i < resolved.size(); i++) {
            stops.add(BadgeConditionCourseStop.builder()
                    .badgeCondition(condition)
                    .place(resolved.get(i))
                    .stepOrder(i + 1)
                    .build());
        }
        condition.getCourseStops().addAll(stops);
        badgeConditionRepository.save(condition); // cascade로 courseStops까지 저장
    }

    private record BadgeSpec(
            String name, String description, BadgeType badgeType, BadgeGroup group, List<ConditionSpec> conditions) {
    }

    private record ConditionSpec(
            BadgeConditionType type,
            List<String> placeNames,
            String placeNameKeyword,
            String categoryName,
            int visitCount,
            LocalTime timeStart,
            LocalTime timeEnd,
            List<String> courseStopNames) {
    }

    private static ConditionSpec places(String... names) {
        return new ConditionSpec(BadgeConditionType.PLACE, List.of(names), null, null, 1, null, null, null);
    }

    private static ConditionSpec placesAtTime(LocalTime start, LocalTime end, String... names) {
        return new ConditionSpec(BadgeConditionType.PLACE, List.of(names), null, null, 1, start, end, null);
    }

    private static ConditionSpec placeKeyword(String keyword) {
        return new ConditionSpec(BadgeConditionType.PLACE, null, keyword, null, 1, null, null, null);
    }

    private static ConditionSpec category(String categoryName, int visitCount) {
        return new ConditionSpec(BadgeConditionType.CATEGORY, null, null, categoryName, visitCount, null, null, null);
    }

    private static ConditionSpec categoryDiversity(int visitCount) {
        return new ConditionSpec(BadgeConditionType.CATEGORY_DIVERSITY, null, null, null, visitCount, null, null, null);
    }

    private static ConditionSpec tripCount(int visitCount) {
        return new ConditionSpec(BadgeConditionType.TRIP_COUNT, null, null, null, visitCount, null, null, null);
    }

    private static ConditionSpec course(String... orderedNames) {
        return new ConditionSpec(BadgeConditionType.COURSE, null, null, null, 1, null, null, List.of(orderedNames));
    }

    private static ConditionSpec allBadges() {
        return new ConditionSpec(BadgeConditionType.ALL_BADGES, null, null, null, 1, null, null, null);
    }
}
