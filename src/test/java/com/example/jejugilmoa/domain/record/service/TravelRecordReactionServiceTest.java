package com.example.jejugilmoa.domain.record.service;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.dto.TravelRecordReactionRequest;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.enums.ReactionType;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.repository.TravelRecordReactionRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TravelRecordReactionServiceTest {

    @Mock UserRepository userRepository;
    @Mock TravelRecordRepository travelRecordRepository;
    @Mock TravelRecordReactionRepository reactionRepository;
    @InjectMocks TravelRecordReactionService service;

    private User requester;
    private User author;
    private TravelRecord publicRecord;

    @BeforeEach
    void setUp() {
        requester = User.builder().id(1L).nickname("요청자").build();
        author = User.builder().id(2L).nickname("작성자").build();
        publicRecord = TravelRecord.builder().id(10L).user(author)
                .title("공개 기록").visibility(Visibility.PUBLIC).build();
    }

    @Test
    void createsLikeForAnotherUsersPublicRecord() {
        givenPublicTarget();
        service.setReaction(1L, 10L, request(ReactionType.LIKE));

        verify(reactionRepository).upsertReaction(10L, 1L, "LIKE");
        verify(reactionRepository, never()).findActiveByTravelRecordIdAndUserId(10L, 1L);
    }

    @Test
    void createsDislikeForAnotherUsersPublicRecord() {
        givenPublicTarget();
        service.setReaction(1L, 10L, request(ReactionType.DISLIKE));

        verify(reactionRepository).upsertReaction(10L, 1L, "DISLIKE");
    }

    @Test
    void repeatedLikeIsIdempotentWithoutAddingRow() {
        assertRepeatedReactionIsIdempotent(ReactionType.LIKE);
    }

    @Test
    void repeatedDislikeIsIdempotentWithoutAddingRow() {
        assertRepeatedReactionIsIdempotent(ReactionType.DISLIKE);
    }

    @Test
    void changesLikeToDislikeOnExistingRow() {
        assertChangesReaction(ReactionType.DISLIKE);
    }

    @Test
    void changesDislikeToLikeOnExistingRow() {
        assertChangesReaction(ReactionType.LIKE);
    }

    @Test
    void deletesExistingLike() {
        assertDeletesReaction(ReactionType.LIKE);
    }

    @Test
    void deletesExistingDislike() {
        assertDeletesReaction(ReactionType.DISLIKE);
    }

    @Test
    void deletingMissingReactionIsIdempotent() {
        givenPublicTarget();
        given(reactionRepository.deleteActiveByTravelRecordIdAndUserId(10L, 1L)).willReturn(0);
        service.deleteReaction(1L, 10L);

        verify(reactionRepository).deleteActiveByTravelRecordIdAndUserId(10L, 1L);
        verify(reactionRepository, never()).findActiveByTravelRecordIdAndUserId(10L, 1L);
    }

    @Test
    void rejectsSelfReactionForPostAndDelete() {
        TravelRecord ownRecord = TravelRecord.builder().id(10L).user(requester)
                .title("내 기록").visibility(Visibility.PUBLIC).build();
        given(travelRecordRepository.findActiveByIdForUpdate(10L)).willReturn(Optional.of(ownRecord));
        given(userRepository.findAllByIdForUpdateOrderById(List.of(1L))).willReturn(List.of(requester));

        assertCode(() -> service.setReaction(1L, 10L, request(ReactionType.LIKE)),
                RecordErrorCode.RECORD_SELF_REACTION_NOT_ALLOWED);
        assertCode(() -> service.deleteReaction(1L, 10L),
                RecordErrorCode.RECORD_SELF_REACTION_NOT_ALLOWED);
    }

    @Test
    void hidesAnotherUsersPrivateRecord() {
        TravelRecord privateRecord = TravelRecord.builder().id(10L).user(author)
                .title("비공개 기록").visibility(Visibility.PRIVATE).build();
        given(travelRecordRepository.findActiveByIdForUpdate(10L)).willReturn(Optional.of(privateRecord));
        givenActiveUsers();

        assertCode(() -> service.setReaction(1L, 10L, request(ReactionType.LIKE)),
                RecordErrorCode.RECORD_NOT_FOUND);
    }

    @Test
    void treatsDeletedRecordAsNotFound() {
        assertInactiveRecordIsNotFound();
    }

    @Test
    void treatsWithdrawnAuthorsRecordAsNotFound() {
        assertInactiveRecordIsNotFound();
    }

    private void assertRepeatedReactionIsIdempotent(ReactionType type) {
        givenPublicTarget();

        service.setReaction(1L, 10L, request(type));

        verify(reactionRepository).upsertReaction(10L, 1L, type.name());
        verify(reactionRepository, never()).findActiveByTravelRecordIdAndUserId(10L, 1L);
    }

    private void assertChangesReaction(ReactionType after) {
        givenPublicTarget();

        service.setReaction(1L, 10L, request(after));

        verify(reactionRepository).upsertReaction(10L, 1L, after.name());
        verify(reactionRepository, never()).findActiveByTravelRecordIdAndUserId(10L, 1L);
    }

    private void assertDeletesReaction(ReactionType type) {
        givenPublicTarget();
        given(reactionRepository.deleteActiveByTravelRecordIdAndUserId(10L, 1L)).willReturn(1);

        service.deleteReaction(1L, 10L);

        verify(reactionRepository).deleteActiveByTravelRecordIdAndUserId(10L, 1L);
        verify(reactionRepository, never()).findActiveByTravelRecordIdAndUserId(10L, 1L);
    }

    private void assertInactiveRecordIsNotFound() {
        given(travelRecordRepository.findActiveByIdForUpdate(10L)).willReturn(Optional.empty());
        assertCode(() -> service.setReaction(1L, 10L, request(ReactionType.LIKE)),
                RecordErrorCode.RECORD_NOT_FOUND);
    }

    private void givenPublicTarget() {
        given(travelRecordRepository.findActiveByIdForUpdate(10L)).willReturn(Optional.of(publicRecord));
        givenActiveUsers();
    }

    private void givenActiveUsers() {
        given(userRepository.findAllByIdForUpdateOrderById(List.of(1L, 2L)))
                .willReturn(List.of(requester, author));
    }

    private TravelRecordReactionRequest request(ReactionType type) {
        return new TravelRecordReactionRequest(type);
    }

    private void assertCode(Runnable operation, RecordErrorCode code) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(GeneralException.class)
                .satisfies(error -> assertThat(((GeneralException) error).getCode()).isEqualTo(code));
    }
}
