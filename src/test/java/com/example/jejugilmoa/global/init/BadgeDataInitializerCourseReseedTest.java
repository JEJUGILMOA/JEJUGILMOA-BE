package com.example.jejugilmoa.global.init;

import com.example.jejugilmoa.domain.badge.entity.Badge;
import com.example.jejugilmoa.domain.badge.entity.BadgeCondition;
import com.example.jejugilmoa.domain.badge.entity.BadgeConditionCourseStop;
import com.example.jejugilmoa.domain.badge.enums.BadgeConditionType;
import com.example.jejugilmoa.domain.badge.repository.BadgeConditionRepository;
import com.example.jejugilmoa.domain.badge.repository.BadgeRepository;
import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.entity.UserBadge;
import com.example.jejugilmoa.domain.user.repository.UserBadgeRepository;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BadgeDataInitializerCourseReseedTest {

    // BadgeDataInitializer의 트레킹 마스터 코스 스펙과 동일해야 한다
    private static final List<String> COURSE_SPEC_NAMES = List.of(
            "[제주올레 7코스] 제주올레 여행자센터-월평 올레",
            "[제주올레 8코스] 월평-대평 올레",
            "[제주올레 9코스] 대평-화순 올레");

    @Autowired BadgeDataInitializer initializer;
    @Autowired BadgeRepository badgeRepository;
    @Autowired BadgeConditionRepository badgeConditionRepository;
    @Autowired UserBadgeRepository userBadgeRepository;
    @Autowired UserRepository userRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired CategoryRepository categoryRepository;

    @Test
    void run_replacesStaleCourseCondition_keepingGrantedUserBadges() throws Exception {
        Badge badge = badgeRepository.findByName("트레킹 마스터").orElseThrow();
        Category category = categoryRepository.saveAndFlush(Category.builder().name("코스 재시드 테스트").build());
        ensureSpecPlacesExist(category);

        // 기존 배포 상태 재현: 스펙과 다른(구버전) 코스 조건으로 바꿔치기
        deleteCourseConditions(badge);
        Place oldStop1 = savePlace("코스재시드테스트-구경유지1", category);
        Place oldStop2 = savePlace("코스재시드테스트-구경유지2", category);
        BadgeCondition stale = BadgeCondition.builder()
                .badge(badge).conditionType(BadgeConditionType.COURSE).build();
        stale.getCourseStops().add(courseStop(stale, oldStop1, 1));
        stale.getCourseStops().add(courseStop(stale, oldStop2, 2));
        badgeConditionRepository.saveAndFlush(stale);

        // 이미 지급된 UserBadge는 조건 교체와 무관하게 유지되어야 한다
        User user = userRepository.saveAndFlush(User.builder().nickname("코스 뱃지 보유자").build());
        UserBadge granted = userBadgeRepository.saveAndFlush(
                UserBadge.builder().user(user).badge(badge).build());

        initializer.run(null);
        badgeConditionRepository.flush();

        List<BadgeCondition> after = courseConditions(badge);
        assertThat(after).hasSize(1);
        assertThat(after.get(0).getId()).isNotEqualTo(stale.getId());
        assertThat(after.get(0).getCourseStops())
                .extracting(stop -> stop.getPlace().getName())
                .containsExactlyElementsOf(COURSE_SPEC_NAMES);
        assertThat(userBadgeRepository.findById(granted.getId())).isPresent();
    }

    @Test
    void run_keepsCourseCondition_whenSpecUnchanged() throws Exception {
        Badge badge = badgeRepository.findByName("트레킹 마스터").orElseThrow();
        Category category = categoryRepository.saveAndFlush(Category.builder().name("코스 재시드 테스트").build());
        ensureSpecPlacesExist(category);

        initializer.run(null);
        List<Long> firstIds = courseConditions(badge).stream().map(BadgeCondition::getId).toList();
        assertThat(firstIds).hasSize(1);

        initializer.run(null);
        List<Long> secondIds = courseConditions(badge).stream().map(BadgeCondition::getId).toList();

        assertThat(secondIds).containsExactlyElementsOf(firstIds);
    }

    private List<BadgeCondition> courseConditions(Badge badge) {
        return badgeConditionRepository.findAllByBadgeId(badge.getId()).stream()
                .filter(c -> c.getConditionType() == BadgeConditionType.COURSE)
                .toList();
    }

    private void deleteCourseConditions(Badge badge) {
        badgeConditionRepository.deleteAll(courseConditions(badge));
        badgeConditionRepository.flush();
    }

    // 로컬/CI 어느 환경에서든 스펙 코스가 시드될 수 있도록 경유지 장소를 보장한다
    private void ensureSpecPlacesExist(Category category) {
        for (String name : COURSE_SPEC_NAMES) {
            if (placeRepository.findAllByNameIn(List.of(name)).isEmpty()) {
                savePlace(name, category);
            }
        }
        placeRepository.flush();
    }

    private BadgeConditionCourseStop courseStop(BadgeCondition condition, Place place, int order) {
        return BadgeConditionCourseStop.builder()
                .badgeCondition(condition).place(place).stepOrder(order).build();
    }

    private Place savePlace(String name, Category category) {
        return placeRepository.saveAndFlush(Place.builder()
                .name(name)
                .address("제주특별자치도")
                .latitude(new BigDecimal("33.49960000"))
                .longitude(new BigDecimal("126.53120000"))
                .category(category)
                .published(true)
                .build());
    }
}
