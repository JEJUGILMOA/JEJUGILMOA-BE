package com.example.jejugilmoa.domain.record;

import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TravelRecordTitleConstraintIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired JdbcClient jdbcClient;

    @Test
    void travelRecord_savesWhenTitleHasExactly50Characters() {
        Long userId = saveUser().getId();

        int inserted = saveTravelRecord(userId, "가".repeat(50));

        assertThat(inserted).isOne();
    }

    @Test
    void travelRecord_failsAtDatabaseWhenTitleExceeds50Characters() {
        Long userId = saveUser().getId();

        assertThatThrownBy(() -> saveTravelRecord(userId, "가".repeat(51)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("travel_record_title_required_and_length_check");
    }

    private User saveUser() {
        return userRepository.saveAndFlush(User.builder().nickname("기록 작성자").build());
    }

    private int saveTravelRecord(Long userId, String title) {
        return jdbcClient.sql("""
                        INSERT INTO travel_record
                            (created_at, updated_at, user_id, title, visibility, like_count, dislike_count)
                        VALUES
                            (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :userId, :title, 'PRIVATE', 0, 0)
                        """)
                .param("userId", userId)
                .param("title", title)
                .update();
    }
}
