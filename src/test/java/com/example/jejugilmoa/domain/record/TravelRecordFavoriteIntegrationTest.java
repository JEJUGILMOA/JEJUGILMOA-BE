package com.example.jejugilmoa.domain.record;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCardResponse;
import com.example.jejugilmoa.domain.record.entity.*;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.repository.*;
import com.example.jejugilmoa.domain.record.service.TravelRecordFavoriteService;
import com.example.jejugilmoa.domain.user.entity.*;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "app.sync.run-on-startup=false")
@Transactional
class TravelRecordFavoriteIntegrationTest {
    @Autowired TravelRecordFavoriteService service;
    @Autowired TravelRecordFavoriteRepository favorites;
    @Autowired TravelRecordRepository records;
    @Autowired UserRepository users;
    @Autowired EntityManager em;
    @Autowired JdbcClient jdbc;

    @Test
    void publicRecordCanBeSaved() {
        User viewer = user();
        TravelRecord record = record(user());
        service.add(viewer.getId(), record.getId());
        assertThat(favorites.existsByUserIdAndTravelRecordId(viewer.getId(), record.getId())).isTrue();
        assertThat(service.list(viewer.getId(), PageRequest.of(0, 20)).content())
                .extracting(TravelRecordCardResponse::recordId).containsExactly(record.getId());
    }

    @Test
    void ownRecordIsForbidden() {
        User viewer = user();
        fails(viewer, record(viewer), RecordErrorCode.RECORD_SELF_FAVORITE_NOT_ALLOWED);
    }

    @Test
    void privateRecordIsHidden() {
        TravelRecord record = record(user());
        record.updateContent(null, null, Visibility.PRIVATE);
        fails(user(), record, RecordErrorCode.RECORD_NOT_FOUND);
    }

    @Test
    void duplicateIsConflict() {
        User viewer = user();
        TravelRecord record = record(user());
        service.add(viewer.getId(), record.getId());
        fails(viewer, record, RecordErrorCode.RECORD_FAVORITE_ALREADY_EXISTS);
    }

    @Test
    void deleteIsIdempotentAndOnlyRemovesOwnRelationRegardlessOfTargetState() {
        User viewer = user();
        User other = user();
        User author = user();
        TravelRecord record = record(author);
        service.add(viewer.getId(), record.getId());
        service.add(other.getId(), record.getId());
        record.updateContent(null, null, Visibility.PRIVATE);
        record.delete();
        author.withdraw(LocalDateTime.now());
        em.flush();
        service.delete(viewer.getId(), record.getId());
        service.delete(viewer.getId(), record.getId());
        service.delete(viewer.getId(), Long.MAX_VALUE);
        assertThat(favorites.existsByUserIdAndTravelRecordId(viewer.getId(), record.getId())).isFalse();
        assertThat(favorites.existsByUserIdAndTravelRecordId(other.getId(), record.getId())).isTrue();
    }

    @Test
    void listAndCountFilterPrivateDeletedWithdrawnAndBothBlockDirections() {
        User viewer = user();
        TravelRecord first = record(user());
        TravelRecord second = record(user());
        TravelRecord privateRecord = record(user());
        TravelRecord deleted = record(user());
        TravelRecord withdrawn = record(user());
        TravelRecord blocked = record(user());
        TravelRecord blocking = record(user());
        for (TravelRecord r : java.util.List.of(second, first, privateRecord, deleted, withdrawn, blocked, blocking)) {
            service.add(viewer.getId(), r.getId());
        }
        // 같은 저장 시각에서는 즐겨찾기 ID 순서이며 기록 생성 순서와 무관하다.
        jdbc.sql("UPDATE travel_record_favorite SET created_at = '2026-01-01T00:00:00Z' WHERE user_id = :id")
                .param("id", viewer.getId()).update();
        privateRecord.updateContent(null, null, Visibility.PRIVATE);
        deleted.delete();
        withdrawn.getUser().withdraw(LocalDateTime.now());
        em.persist(UserBlock.builder().blocker(viewer).blocked(blocked.getUser()).build());
        em.persist(UserBlock.builder().blocker(blocking.getUser()).blocked(viewer).build());
        // 다른 사용자의 저장은 집계되지 않는다.
        service.add(user().getId(), record(user()).getId());
        em.flush();
        em.clear();
        var page = service.list(viewer.getId(), PageRequest.of(0, 1));
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(TravelRecordCardResponse::recordId).containsExactly(first.getId());
        assertThat(service.list(viewer.getId(), PageRequest.of(1, 1)).content())
                .extracting(TravelRecordCardResponse::recordId).containsExactly(second.getId());
        jdbc.sql("UPDATE travel_record_favorite SET created_at = '2026-02-01T00:00:00Z' WHERE user_id = :id AND travel_record_id = :recordId")
                .param("id", viewer.getId()).param("recordId", second.getId()).update();
        assertThat(service.list(viewer.getId(), PageRequest.of(0, 20)).content())
                .extracting(TravelRecordCardResponse::recordId).containsExactly(second.getId(), first.getId());
    }

    @Test
    void creationHidesDeletedRecordAndWithdrawnAuthorAndBlocks() {
        User viewer = user();
        TravelRecord deleted = record(user());
        deleted.delete();
        em.flush();
        fails(viewer, deleted, RecordErrorCode.RECORD_NOT_FOUND);
    }

    @Test
    void withdrawnAuthorIsHidden() {
        TravelRecord record = record(user());
        record.getUser().withdraw(LocalDateTime.now());
        fails(user(), record, RecordErrorCode.RECORD_NOT_FOUND);
    }

    @Test
    void blockedAuthorIsHidden() {
        User viewer = user();
        TravelRecord record = record(user());
        em.persist(UserBlock.builder().blocker(record.getUser()).blocked(viewer).build());
        fails(viewer, record, RecordErrorCode.RECORD_NOT_FOUND);
    }

    @Test
    void inactiveRequesterCannotCreate() {
        User viewer = user();
        TravelRecord record = record(user());
        viewer.withdraw(LocalDateTime.now());
        assertThatThrownBy(() -> service.add(viewer.getId(), record.getId()))
                .isInstanceOfSatisfying(GeneralException.class, e -> assertThat(e.getCode())
                        .isEqualTo(com.example.jejugilmoa.domain.user.exception.UserErrorCode.USER_NOT_FOUND));
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentCreatesProduceOneSuccessAndOneConflict() throws Exception {
        User viewer = user();
        User author = user();
        TravelRecord record = record(author);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<String> create = () -> {
            ready.countDown();
            if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("대기 시간 초과");
            try {
                service.add(viewer.getId(), record.getId());
                return "created";
            } catch (GeneralException exception) {
                return exception.getCode().getCode();
            }
        };
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(create);
            var second = executor.submit(create);
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("created", "RECORD409_2");
            assertThat(jdbc.sql("SELECT count(*) FROM travel_record_favorite WHERE user_id = :id")
                    .param("id", viewer.getId()).query(Long.class).single()).isOne();
        } finally {
            service.delete(viewer.getId(), record.getId());
            records.deleteById(record.getId());
            users.deleteById(viewer.getId());
            users.deleteById(author.getId());
        }
    }

    @Test
    void databaseUniqueConstraintRejectsDirectDuplicates() {
        User viewer = user();
        TravelRecord record = record(user());
        service.add(viewer.getId(), record.getId());
        assertThatThrownBy(() -> favorites.saveAndFlush(TravelRecordFavorite.builder()
                .user(viewer).travelRecord(record).build()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private User user() { return users.saveAndFlush(User.builder().nickname("기록 즐겨찾기 테스트").build()); }
    private TravelRecord record(User author) {
        return records.saveAndFlush(TravelRecord.builder().user(author).title("즐겨찾기 테스트")
                .visibility(Visibility.PUBLIC).build());
    }
    private void fails(User viewer, TravelRecord record, RecordErrorCode code) {
        assertThatThrownBy(() -> service.add(viewer.getId(), record.getId()))
                .isInstanceOfSatisfying(GeneralException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
