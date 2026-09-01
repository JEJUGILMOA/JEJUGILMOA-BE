package com.example.jejugilmoa.domain.record;

import com.example.jejugilmoa.domain.imageupload.service.ImageUrlResolver;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCardResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordReactionRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateRequest;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.enums.ReactionType;
import com.example.jejugilmoa.domain.record.enums.RecordView;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.repository.TravelRecordReactionRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.record.service.TravelRecordQueryService;
import com.example.jejugilmoa.domain.record.service.TravelRecordReactionService;
import com.example.jejugilmoa.domain.record.service.TravelRecordService;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.service.UserService;
import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TravelRecordReactionIntegrationTest {

    @Autowired TravelRecordReactionService reactionService;
    @Autowired TravelRecordQueryService queryService;
    @Autowired TravelRecordReactionRepository reactionRepository;
    @Autowired TravelRecordRepository recordRepository;
    @Autowired UserRepository userRepository;
    @Autowired TravelRecordService recordService;
    @Autowired UserService userService;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired JdbcClient jdbcClient;
    @MockitoBean ImageUrlResolver imageUrlResolver;

    @Test
    void uniqueConstraintAndRepositoryLookupRemainAvailable() {
        Integer constraintCount = jdbcClient.sql("""
                        SELECT COUNT(*)
                        FROM pg_constraint
                        WHERE conname = 'uk_record_reaction'
                          AND conrelid = 'record_reaction'::regclass
                          AND contype = 'u'
                        """)
                .query(Integer.class)
                .single();

        assertThat(constraintCount).isOne();
    }

    @Test
    void createChangeAndDeleteAreReflectedInCardAndDetailQueries() {
        User requester = userRepository.saveAndFlush(User.builder().nickname("반응 요청자").build());
        User author = userRepository.saveAndFlush(User.builder().nickname("반응 기록 작성자").build());
        TravelRecord record = recordRepository.saveAndFlush(TravelRecord.builder()
                .user(author)
                .title("반응 통합 테스트 공개 기록")
                .visibility(Visibility.PUBLIC)
                .build());

        reactionService.setReaction(requester.getId(), record.getId(), request(ReactionType.LIKE));

        assertThat(reactionRepository.findActiveByTravelRecordIdAndUserId(record.getId(), requester.getId()))
                .get().extracting(reaction -> reaction.getReactionType()).isEqualTo(ReactionType.LIKE);
        assertReactionSummary(record.getId(), requester.getId(), 1, 0, ReactionType.LIKE);

        reactionService.setReaction(requester.getId(), record.getId(), request(ReactionType.DISLIKE));

        assertThat(reactionRepository.count()).isPositive();
        assertThat(reactionRepository.findActiveByTravelRecordIdAndUserId(record.getId(), requester.getId()))
                .get().extracting(reaction -> reaction.getReactionType()).isEqualTo(ReactionType.DISLIKE);
        assertReactionSummary(record.getId(), requester.getId(), 0, 1, ReactionType.DISLIKE);

        reactionService.deleteReaction(requester.getId(), record.getId());

        assertThat(reactionRepository.findActiveByTravelRecordIdAndUserId(record.getId(), requester.getId())).isEmpty();
        assertReactionSummary(record.getId(), requester.getId(), 0, 0, null);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentPostsBySameUserConvergeToOneReaction() throws Exception {
        User requester = userRepository.saveAndFlush(User.builder().nickname("동시 반응 요청자").build());
        User author = userRepository.saveAndFlush(User.builder().nickname("동시 반응 작성자").build());
        TravelRecord record = recordRepository.saveAndFlush(TravelRecord.builder()
                .user(author)
                .title("동시 반응 공개 기록")
                .visibility(Visibility.PUBLIC)
                .build());
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> setReactionAfterStart(requester.getId(), record.getId(), ready, start));
            var second = executor.submit(() -> setReactionAfterStart(requester.getId(), record.getId(), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);

            Integer reactionCount = jdbcClient.sql("""
                            SELECT COUNT(*) FROM record_reaction
                            WHERE travel_record_id = :recordId AND user_id = :userId
                            """)
                    .param("recordId", record.getId())
                    .param("userId", requester.getId())
                    .query(Integer.class)
                    .single();
            assertThat(reactionCount).isOne();
            assertThat(reactionRepository.findActiveByTravelRecordIdAndUserId(record.getId(), requester.getId()))
                    .get().extracting(reaction -> reaction.getReactionType()).isEqualTo(ReactionType.LIKE);
        } finally {
            jdbcClient.sql("DELETE FROM record_reaction WHERE travel_record_id = :recordId")
                    .param("recordId", record.getId()).update();
            jdbcClient.sql("DELETE FROM travel_record WHERE id = :recordId")
                    .param("recordId", record.getId()).update();
            jdbcClient.sql("DELETE FROM \"user\" WHERE id IN (:requesterId, :authorId)")
                    .param("requesterId", requester.getId()).param("authorId", author.getId()).update();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentPostAndDeleteCompleteWithoutStaleEntityFailure() throws Exception {
        User requester = userRepository.saveAndFlush(User.builder().nickname("경합 반응 요청자").build());
        User author = userRepository.saveAndFlush(User.builder().nickname("경합 반응 작성자").build());
        TravelRecord record = recordRepository.saveAndFlush(TravelRecord.builder()
                .user(author)
                .title("POST DELETE 경합 공개 기록")
                .visibility(Visibility.PUBLIC)
                .build());
        reactionService.setReaction(requester.getId(), record.getId(), request(ReactionType.LIKE));
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var post = executor.submit(() -> runAfterStart(ready, start,
                    () -> reactionService.setReaction(
                            requester.getId(), record.getId(), request(ReactionType.DISLIKE))));
            var delete = executor.submit(() -> runAfterStart(ready, start,
                    () -> reactionService.deleteReaction(requester.getId(), record.getId())));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            post.get(5, TimeUnit.SECONDS);
            delete.get(5, TimeUnit.SECONDS);

            Integer reactionCount = jdbcClient.sql("""
                            SELECT COUNT(*) FROM record_reaction
                            WHERE travel_record_id = :recordId AND user_id = :userId
                            """)
                    .param("recordId", record.getId())
                    .param("userId", requester.getId())
                    .query(Integer.class)
                    .single();
            assertThat(reactionCount).isBetween(0, 1);
        } finally {
            deleteFixtures(record.getId(), requester.getId(), author.getId());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void postWaitsForRecordDeleteAndRejectsDeletedRecord() throws Exception {
        assertPostWaitsForStateChange(RecordErrorCode.RECORD_NOT_FOUND, (requester, author, record) -> {
            recordService.delete(author.getId(), record.getId());
            recordRepository.flush();
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void postWaitsForPrivateVisibilityUpdateAndRejectsPrivateRecord() throws Exception {
        assertPostWaitsForStateChange(RecordErrorCode.RECORD_NOT_FOUND,
                (requester, author, record) -> recordService.update(
                author.getId(), record.getId(),
                new TravelRecordUpdateRequest(null, null, Visibility.PRIVATE, List.of(), null)));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void postWaitsForAuthorWithdrawalAndRejectsWithdrawnAuthorsRecord() throws Exception {
        assertPostWaitsForStateChange(RecordErrorCode.RECORD_NOT_FOUND, (requester, author, record) -> {
            userService.withdraw(author.getId());
            userRepository.flush();
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void postWaitsForRequesterWithdrawalAndRejectsWithdrawnRequester() throws Exception {
        assertPostWaitsForStateChange(UserErrorCode.USER_NOT_FOUND, (requester, author, record) -> {
            userService.withdraw(requester.getId());
            userRepository.flush();
        });
    }

    private void assertReactionSummary(
            Long recordId, Long requesterId, long likeCount, long dislikeCount, ReactionType myReaction) {
        var detail = queryService.getDetail(recordId, requesterId);
        assertThat(detail.likeCount()).isEqualTo(likeCount);
        assertThat(detail.dislikeCount()).isEqualTo(dislikeCount);
        assertThat(detail.myReaction()).isEqualTo(myReaction);

        var page = queryService.getRecords(
                requesterId, RecordView.CARD, false, PageRequest.of(0, 100));
        TravelRecordCardResponse card = page.content().stream()
                .map(TravelRecordCardResponse.class::cast)
                .filter(item -> item.recordId().equals(recordId))
                .findFirst()
                .orElseThrow();
        assertThat(card.likeCount()).isEqualTo(likeCount);
        assertThat(card.dislikeCount()).isEqualTo(dislikeCount);
        assertThat(card.myReaction()).isEqualTo(myReaction);
    }

    private TravelRecordReactionRequest request(ReactionType type) {
        return new TravelRecordReactionRequest(type);
    }

    private void assertPostWaitsForStateChange(BaseCode expectedCode, StateChange stateChange) throws Exception {
        User requester = userRepository.saveAndFlush(User.builder().nickname("상태 경합 요청자").build());
        User author = userRepository.saveAndFlush(User.builder().nickname("상태 경합 작성자").build());
        TravelRecord record = recordRepository.saveAndFlush(TravelRecord.builder()
                .user(author).title("상태 경합 공개 기록").visibility(Visibility.PUBLIC).build());
        var stateChanged = new CountDownLatch(1);
        var releaseCommit = new CountDownLatch(1);
        var blockerPid = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> mutation = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                stateChange.run(requester, author, record);
                blockerPid.set(jdbcClient.sql("SELECT pg_backend_pid()")
                        .query(Integer.class).single());
                stateChanged.countDown();
                await(releaseCommit);
            }));
            assertThat(stateChanged.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> post = executor.submit(() -> reactionService.setReaction(
                    requester.getId(), record.getId(), request(ReactionType.LIKE)));
            assertThat(awaitBlockedBy(blockerPid.get())).isTrue();
            releaseCommit.countDown();
            mutation.get(5, TimeUnit.SECONDS);

            assertThat(assertThatThrownByFuture(post))
                    .isInstanceOf(GeneralException.class)
                    .extracting(error -> ((GeneralException) error).getCode())
                    .isEqualTo(expectedCode);
            Integer reactionCount = jdbcClient.sql("""
                            SELECT COUNT(*) FROM record_reaction
                            WHERE travel_record_id = :recordId AND user_id = :userId
                            """)
                    .param("recordId", record.getId()).param("userId", requester.getId())
                    .query(Integer.class).single();
            assertThat(reactionCount).isZero();
        } finally {
            releaseCommit.countDown();
            deleteFixtures(record.getId(), requester.getId(), author.getId());
        }
    }

    private boolean awaitBlockedBy(int blockerPid) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Integer blocked = jdbcClient.sql("""
                            SELECT COUNT(*) FROM pg_stat_activity
                            WHERE :blockerPid = ANY(pg_blocking_pids(pid))
                            """)
                    .param("blockerPid", blockerPid).query(Integer.class).single();
            if (blocked > 0) {
                return true;
            }
            Thread.onSpinWait();
        }
        return false;
    }

    private Throwable assertThatThrownByFuture(Future<?> future) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Reaction POST가 실패해야 합니다.");
        } catch (ExecutionException exception) {
            return exception.getCause();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("트랜잭션 해제 대기 시간 초과");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    @FunctionalInterface
    private interface StateChange {
        void run(User requester, User author, TravelRecord record);
    }

    private void setReactionAfterStart(
            Long userId, Long recordId, CountDownLatch ready, CountDownLatch start) {
        runAfterStart(ready, start,
                () -> reactionService.setReaction(userId, recordId, request(ReactionType.LIKE)));
    }

    private void runAfterStart(CountDownLatch ready, CountDownLatch start, Runnable operation) {
        try {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 요청 시작 대기 시간 초과");
            }
            operation.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void deleteFixtures(Long recordId, Long requesterId, Long authorId) {
        jdbcClient.sql("DELETE FROM record_reaction WHERE travel_record_id = :recordId")
                .param("recordId", recordId).update();
        jdbcClient.sql("DELETE FROM travel_record WHERE id = :recordId")
                .param("recordId", recordId).update();
        jdbcClient.sql("DELETE FROM \"user\" WHERE id IN (:requesterId, :authorId)")
                .param("requesterId", requesterId).param("authorId", authorId).update();
    }
}
