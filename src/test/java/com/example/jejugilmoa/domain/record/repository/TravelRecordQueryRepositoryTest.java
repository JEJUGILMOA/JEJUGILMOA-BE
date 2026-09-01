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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TravelRecordQueryRepositoryTest {

    private static final Sort STABLE_SORT = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    @Autowired TravelRecordRepository travelRecordRepository;
    @Autowired TravelRecordImageRepository travelRecordImageRepository;
    @Autowired TravelRecordPlaceRepository travelRecordPlaceRepository;
    @Autowired TravelRecordReactionRepository travelRecordReactionRepository;
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

    @Test
    void ownedMutationLookupAndExistenceExcludeDeletedRecordsAndUsers() {
        User activeOwner = userRepository.saveAndFlush(User.builder().nickname("수정 활성 작성자").build());
        User deletedOwner = userRepository.saveAndFlush(User.builder().nickname("수정 탈퇴 작성자").build());
        Instant now = Instant.parse("2026-08-19T00:00:00Z");
        Long activeId = insertRecord(activeOwner.getId(), "PRIVATE", null, now);
        Long deletedRecordId = insertRecord(activeOwner.getId(), "PRIVATE", now, now.plusSeconds(1));
        Long deletedOwnerRecordId = insertRecord(deletedOwner.getId(), "PRIVATE", null, now.plusSeconds(2));
        jdbcClient.sql("UPDATE \"user\" SET deleted_at = :deletedAt WHERE id = :userId")
                .param("deletedAt", Timestamp.from(now))
                .param("userId", deletedOwner.getId())
                .update();

        assertThat(travelRecordRepository.findActiveOwnedRecord(activeId, activeOwner.getId())).isPresent();
        assertThat(travelRecordRepository.existsActiveById(activeId)).isTrue();
        assertThat(travelRecordRepository.findActiveOwnedRecord(deletedRecordId, activeOwner.getId())).isEmpty();
        assertThat(travelRecordRepository.existsActiveById(deletedRecordId)).isFalse();
        assertThat(travelRecordRepository.findActiveOwnedRecord(deletedOwnerRecordId, deletedOwner.getId())).isEmpty();
        assertThat(travelRecordRepository.existsActiveById(deletedOwnerRecordId)).isFalse();
    }

    @Test
    void childQueriesExcludeSoftDeletedRecordsAndUsers() {
        User owner = userRepository.saveAndFlush(User.builder().nickname("하위 조회 작성자").build());
        Instant now = Instant.parse("2026-08-19T00:00:00Z");
        Long activeId = insertRecord(owner.getId(), "PUBLIC", null, now);
        Long deletedId = insertRecord(owner.getId(), "PUBLIC", now, now.plusSeconds(1));
        Long placeId = insertPlace(now);

        insertRecordImage(activeId, "records/active.jpg", 0, now);
        insertRecordImage(deletedId, "records/deleted.jpg", 0, now);
        insertRecordPlace(activeId, placeId, 0, now);
        insertRecordPlace(deletedId, placeId, 0, now);
        insertReaction(activeId, owner.getId(), "LIKE");
        insertReaction(deletedId, owner.getId(), "DISLIKE");

        assertThat(travelRecordImageRepository.findFirstImagesByRecordIds(List.of(activeId, deletedId)))
                .extracting(image -> image.getTravelRecord().getId())
                .containsExactly(activeId);
        assertThat(travelRecordImageRepository.countByRecordIds(List.of(activeId, deletedId)))
                .extracting(TravelRecordImageRepository.RecordImageCount::getRecordId)
                .containsExactly(activeId);
        assertThat(travelRecordImageRepository.findAllByRecordIdOrderBySequence(deletedId)).isEmpty();

        assertThat(travelRecordPlaceRepository.countVisitedByRecordIds(List.of(activeId, deletedId)))
                .extracting(TravelRecordPlaceRepository.VisitedPlaceCount::getRecordId)
                .containsExactly(activeId);
        assertThat(travelRecordPlaceRepository.findAllByRecordIdsInSnapshotOrder(List.of(activeId, deletedId)))
                .extracting(place -> place.getTravelRecord().getId())
                .containsExactly(activeId);
        assertThat(travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(deletedId)).isEmpty();

        assertThat(travelRecordReactionRepository.countByRecordIdsAndType(List.of(activeId, deletedId)))
                .extracting(TravelRecordReactionRepository.ReactionCount::getRecordId)
                .containsExactly(activeId);
        assertThat(travelRecordReactionRepository.findMineByRecordIds(
                List.of(activeId, deletedId), owner.getId()))
                .extracting(reaction -> reaction.getTravelRecord().getId())
                .containsExactly(activeId);

        jdbcClient.sql("UPDATE \"user\" SET deleted_at = :deletedAt WHERE id = :userId")
                .param("deletedAt", Timestamp.from(now))
                .param("userId", owner.getId())
                .update();

        assertThat(travelRecordReactionRepository.findMineByRecordIds(List.of(activeId), owner.getId()))
                .isEmpty();
    }

    @Test
    void activeReactionLookupExcludesSoftDeletedRecordAndUser() {
        User activeUser = userRepository.saveAndFlush(User.builder().nickname("활성 반응 사용자").build());
        User deletedUser = userRepository.saveAndFlush(User.builder().nickname("탈퇴 반응 사용자").build());
        Instant now = Instant.parse("2026-08-19T00:00:00Z");
        Long activeRecordId = insertRecord(activeUser.getId(), "PUBLIC", null, now);
        Long deletedRecordId = insertRecord(activeUser.getId(), "PUBLIC", now, now.plusSeconds(1));
        insertReaction(activeRecordId, activeUser.getId(), "LIKE");
        insertReaction(deletedRecordId, activeUser.getId(), "DISLIKE");
        insertReaction(activeRecordId, deletedUser.getId(), "DISLIKE");
        jdbcClient.sql("UPDATE \"user\" SET deleted_at = :deletedAt WHERE id = :userId")
                .param("deletedAt", Timestamp.from(now))
                .param("userId", deletedUser.getId())
                .update();

        assertThat(travelRecordReactionRepository.findActiveByTravelRecordIdAndUserId(
                activeRecordId, activeUser.getId())).isPresent();
        assertThat(travelRecordReactionRepository.findActiveByTravelRecordIdAndUserId(
                deletedRecordId, activeUser.getId())).isEmpty();
        assertThat(travelRecordReactionRepository.findActiveByTravelRecordIdAndUserId(
                activeRecordId, deletedUser.getId())).isEmpty();
    }

    @Test
    void directDeleteRemovesOnlyRequestedUsersReaction() {
        User first = userRepository.saveAndFlush(User.builder().nickname("삭제 대상 사용자").build());
        User second = userRepository.saveAndFlush(User.builder().nickname("삭제 비대상 사용자").build());
        User author = userRepository.saveAndFlush(User.builder().nickname("삭제 테스트 작성자").build());
        Long recordId = insertRecord(author.getId(), "PUBLIC", null, Instant.parse("2026-08-19T00:00:00Z"));
        insertReaction(recordId, first.getId(), "LIKE");
        insertReaction(recordId, second.getId(), "DISLIKE");

        int deleted = travelRecordReactionRepository.deleteActiveByTravelRecordIdAndUserId(
                recordId, first.getId());

        assertThat(deleted).isOne();
        assertThat(travelRecordReactionRepository.findActiveByTravelRecordIdAndUserId(
                recordId, first.getId())).isEmpty();
        assertThat(travelRecordReactionRepository.findActiveByTravelRecordIdAndUserId(
                recordId, second.getId())).isPresent();
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

    private Long insertPlace(Instant createdAt) {
        Long categoryId = jdbcClient.sql("""
                        INSERT INTO category (created_at, updated_at, name)
                        VALUES (:createdAt, :createdAt, :name)
                        RETURNING id
                        """)
                .param("createdAt", Timestamp.from(createdAt))
                .param("name", "조회 테스트 카테고리 " + System.nanoTime())
                .query(Long.class)
                .single();

        return jdbcClient.sql("""
                        INSERT INTO place
                            (created_at, updated_at, name, address, latitude, longitude,
                             visitor_count, is_published, category_id)
                        VALUES
                            (:createdAt, :createdAt, '조회 테스트 장소', '제주 테스트 주소',
                             33.00000000, 126.00000000, 0, true, :categoryId)
                        RETURNING id
                        """)
                .param("createdAt", Timestamp.from(createdAt))
                .param("categoryId", categoryId)
                .query(Long.class)
                .single();
    }

    private void insertRecordImage(Long recordId, String objectKey, int sequenceOrder, Instant createdAt) {
        jdbcClient.sql("""
                        INSERT INTO travel_record_image
                            (created_at, updated_at, travel_record_id, object_key, sequence_order)
                        VALUES (:createdAt, :createdAt, :recordId, :objectKey, :sequenceOrder)
                        """)
                .param("createdAt", Timestamp.from(createdAt))
                .param("recordId", recordId)
                .param("objectKey", objectKey)
                .param("sequenceOrder", sequenceOrder)
                .update();
    }

    private void insertRecordPlace(Long recordId, Long placeId, int sequenceOrder, Instant createdAt) {
        jdbcClient.sql("""
                        INSERT INTO travel_record_place
                            (created_at, updated_at, travel_record_id, travel_place_id,
                             place_name, address, latitude, longitude, visit_date,
                             sequence_order, visited)
                        VALUES
                            (:createdAt, :createdAt, :recordId, :placeId,
                             '조회 테스트 장소', '제주 테스트 주소', 33.00000000, 126.00000000,
                             DATE '2026-08-19', :sequenceOrder, true)
                        """)
                .param("createdAt", Timestamp.from(createdAt))
                .param("recordId", recordId)
                .param("placeId", placeId)
                .param("sequenceOrder", sequenceOrder)
                .update();
    }

    private void insertReaction(Long recordId, Long userId, String reactionType) {
        jdbcClient.sql("""
                        INSERT INTO record_reaction (travel_record_id, user_id, reaction_type)
                        VALUES (:recordId, :userId, :reactionType)
                        """)
                .param("recordId", recordId)
                .param("userId", userId)
                .param("reactionType", reactionType)
                .update();
    }
}
