package com.example.jejugilmoa.domain.record.service;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.dto.TravelRecordReactionRequest;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.repository.TravelRecordReactionRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelRecordReactionService {

    private final UserRepository userRepository;
    private final TravelRecordRepository travelRecordRepository;
    private final TravelRecordReactionRepository travelRecordReactionRepository;

    @Transactional
    public void setReaction(Long userId, Long recordId, TravelRecordReactionRequest request) {
        validateReactionTarget(userId, recordId);

        travelRecordReactionRepository.upsertReaction(
                recordId, userId, request.reactionType().name());
    }

    @Transactional
    public void deleteReaction(Long userId, Long recordId) {
        validateReactionTarget(userId, recordId);
        travelRecordReactionRepository.deleteActiveByTravelRecordIdAndUserId(recordId, userId);
    }

    private void validateReactionTarget(Long userId, Long recordId) {
        TravelRecord record = travelRecordRepository.findActiveByIdForUpdate(recordId)
                .orElseThrow(() -> new GeneralException(RecordErrorCode.RECORD_NOT_FOUND));

        Long authorId = record.getUser().getId();
        List<Long> userIds = userId.equals(authorId) ? List.of(userId) : List.of(userId, authorId);
        Map<Long, User> lockedUsers = userRepository.findAllByIdForUpdateOrderById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        User requester = lockedUsers.get(userId);
        if (requester == null || requester.getDeletedAt() != null) {
            throw new GeneralException(UserErrorCode.USER_NOT_FOUND);
        }
        User author = lockedUsers.get(authorId);
        if (author == null || author.getDeletedAt() != null) {
            throw new GeneralException(RecordErrorCode.RECORD_NOT_FOUND);
        }

        if (authorId.equals(userId)) {
            throw new GeneralException(RecordErrorCode.RECORD_SELF_REACTION_NOT_ALLOWED);
        }
        if (record.getVisibility() != Visibility.PUBLIC) {
            throw new GeneralException(RecordErrorCode.RECORD_NOT_FOUND);
        }
    }
}
