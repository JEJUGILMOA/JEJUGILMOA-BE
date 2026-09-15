package com.example.jejugilmoa.domain.record.service;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCardResponse;
import com.example.jejugilmoa.domain.record.entity.TravelRecordFavorite;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.repository.TravelRecordFavoriteRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.domain.user.service.UserBlockService;
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
public class TravelRecordFavoriteService {

    private final UserRepository userRepository;
    private final TravelRecordRepository travelRecordRepository;
    private final TravelRecordFavoriteRepository favoriteRepository;
    private final UserBlockService userBlockService;

    private final TravelRecordQueryService queryService;

    @Transactional
    public void add(Long userId, Long recordId) {
        TravelRecord record = validateFavoriteTarget(userId, recordId);
        if (favoriteRepository.existsByUserIdAndTravelRecordId(userId, recordId)) {
            throw new GeneralException(RecordErrorCode.RECORD_FAVORITE_ALREADY_EXISTS);
        }
        try {
            favoriteRepository.saveAndFlush(TravelRecordFavorite.builder()
                    .user(userRepository.getReferenceById(userId)).travelRecord(record).build());
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "uk_record_favorite".equals(violation.getConstraintName())) {
                    throw new GeneralException(RecordErrorCode.RECORD_FAVORITE_ALREADY_EXISTS);
                }
            }
            throw exception;
        }
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        favoriteRepository.deleteByOwnerAndRecord(userId, recordId);
    }

    @Transactional(readOnly = true)
    public PageResponse<TravelRecordCardResponse> list(Long userId, Pageable pageable) {
        return queryService.toCardPage(favoriteRepository.findVisibleRecords(userId, pageable), userId);
    }

    private TravelRecord validateFavoriteTarget(Long userId, Long recordId) {
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

        if (record.getVisibility() != Visibility.PUBLIC) {
            throw new GeneralException(RecordErrorCode.RECORD_NOT_FOUND);
        }
        if (userBlockService.isMutuallyBlocked(userId, authorId)) {
            throw new GeneralException(RecordErrorCode.RECORD_NOT_FOUND);
        }
        if (authorId.equals(userId)) {
            throw new GeneralException(RecordErrorCode.RECORD_SELF_FAVORITE_NOT_ALLOWED);
        }
        return record;
    }
}
