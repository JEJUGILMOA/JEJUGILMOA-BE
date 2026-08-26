package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TravelRecordQueryRepositoryTest {

    private static final Sort STABLE_SORT = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    @Autowired TravelRecordRepository travelRecordRepository;
    @Autowired UserRepository userRepository;
    @Autowired JdbcClient jdbcClient;

    @Test
    void mineIncludesPublicAndPrivateExcludesDeletedAndUsesIdAsTieBreaker() {
        User owner = userRepository.saveAndFlush(User.builder().nickname("내 기록 작성자").build());
        Instant sameCreatedAt = Instant.parse("2026-08-19T00:00:00Z");
        Long publicId = insertRecord(owner.getId(), "PUBLIC", null, sameCreatedAt);
        Long privateId = insertRecord(owner.getId(), "PRIVATE", null, sameCreatedAt);
        insertRecord(owner.getId(), "PUBLIC", sameCreatedAt, sameCreatedAt);

        var result = travelRecordRepository.findActiveByUserId(
                owner.getId(), PageRequest.of(0, 20, STABLE_SORT));

        assertThat(result.getContent()).extracting(record -> record.getId())
                .containsExactly(privateId, publicId);
        assertThat(result.getContent()).extracting(record -> record.getVisibility().name())
                .containsExactly("PRIVATE", "PUBLIC");
    }

    @Test
    void publicBrowseIncludesOwnAndOthersPublicButExcludesPrivateAndDeleted() {
        User me = userRepository.saveAndFlush(User.builder().nickname("나").build());
        User other = userRepository.saveAndFlush(User.builder().nickname("타인").build());
        Instant createdAt = Instant.parse("2026-08-19T00:00:00Z");
        Long ownPublicId = insertRecord(me.getId(), "PUBLIC", null, createdAt);
        Long otherPublicId = insertRecord(other.getId(), "PUBLIC", null, createdAt.plusSeconds(1));
        insertRecord(other.getId(), "PRIVATE", null, createdAt.plusSeconds(2));
        insertRecord(other.getId(), "PUBLIC", createdAt.plusSeconds(3), createdAt.plusSeconds(3));

        var result = travelRecordRepository.findActivePublic(PageRequest.of(0, 20, STABLE_SORT));

        assertThat(result.getContent()).extracting(record -> record.getId())
                .containsExactly(otherPublicId, ownPublicId);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void detailLookupTreatsSoftDeletedRecordAsMissing() {
        User owner = userRepository.saveAndFlush(User.builder().nickname("상세 작성자").build());
        Instant now = Instant.parse("2026-08-19T00:00:00Z");
        Long activeId = insertRecord(owner.getId(), "PRIVATE", null, now);
        Long deletedId = insertRecord(owner.getId(), "PRIVATE", now, now.plusSeconds(1));

        assertThat(travelRecordRepository.findActiveByIdWithUserAndPlan(activeId)).isPresent();
        assertThat(travelRecordRepository.findActiveByIdWithUserAndPlan(deletedId)).isEmpty();
    }

    private Long insertRecord(Long userId, String visibility, Instant deletedAt, Instant createdAt) {
        return jdbcClient.sql("""
                        INSERT INTO travel_record
                            (created_at, updated_at, user_id, title, visibility,
                             like_count, dislike_count, deleted_at)
                        VALUES
                            (:createdAt, :createdAt, :userId, :title, :visibility,
                             0, 0, :deletedAt)
                        RETURNING id
                        """)
                .param("createdAt", Timestamp.from(createdAt))
                .param("userId", userId)
                .param("title", "조회 테스트")
                .param("visibility", visibility)
                .param("deletedAt", deletedAt == null ? null : Timestamp.from(deletedAt))
                .query(Long.class)
                .single();
    }
}
