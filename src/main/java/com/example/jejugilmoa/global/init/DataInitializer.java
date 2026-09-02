package com.example.jejugilmoa.global.init;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Order(1) // BadgeDataInitializer(카페 카테고리 조건)보다 먼저 실행되어야 한다
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    private static final List<String[]> DEFAULT_CATEGORIES = List.of(
        new String[]{"자연", "자연 관광지 (산, 해변, 오름 등)"},
        new String[]{"음식", "제주 맛집 및 음식점"},
        new String[]{"카페", "카페 및 디저트"},
        new String[]{"체험", "체험 및 액티비티"},
        new String[]{"역사", "역사 및 문화"},
        new String[]{"쇼핑", "쇼핑 및 시장"},
        new String[]{"축제", "축제 및 이벤트"}
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String[] pair : DEFAULT_CATEGORIES) {
            String name = pair[0];
            if (!categoryRepository.findByName(name).isPresent()) {
                categoryRepository.save(Category.builder()
                    .name(name)
                    .description(pair[1])
                    .build());
                log.info("카테고리 초기화: {}", name);
            }
        }
    }
}
