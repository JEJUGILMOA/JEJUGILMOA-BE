package com.example.jejugilmoa.domain.user.service;

import com.example.jejugilmoa.domain.user.converter.UserBlockConverter;
import com.example.jejugilmoa.domain.user.dto.BlockedUserResponse;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.entity.UserBlock;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.repository.UserBlockRepository;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final UserBlockConverter userBlockConverter;

    @Transactional
    public void blockUser(Long blockerId, Long targetUserId) {
        if (blockerId.equals(targetUserId)) {
            throw new GeneralException(UserErrorCode.BLOCK_SELF);
        }

        User blocker = getUserOrThrow(blockerId);
        User target = getUserOrThrow(targetUserId);

        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId)) {
            throw new GeneralException(UserErrorCode.ALREADY_BLOCKED);
        }

        userBlockRepository.save(UserBlock.builder()
                .blocker(blocker)
                .blocked(target)
                .build());
    }

    @Transactional
    public void unblockUser(Long blockerId, Long targetUserId) {
        UserBlock block = userBlockRepository.findByBlockerIdAndBlockedId(blockerId, targetUserId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.BLOCK_NOT_FOUND));
        userBlockRepository.delete(block);
    }

    public List<BlockedUserResponse> getBlockedUsers(Long blockerId) {
        List<UserBlock> blocks = userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(blockerId);
        return userBlockConverter.toBlockedUserResponseList(blocks);
    }

    /**
     * 내가 차단했거나 나를 차단한 사용자 ID를 모두 반환한다. 콘텐츠 피드 필터링에 사용한다.
     */
    public Set<Long> getMutuallyBlockedUserIds(Long userId) {
        Set<Long> result = new HashSet<>(userBlockRepository.findBlockedIdsByBlockerId(userId));
        result.addAll(userBlockRepository.findBlockerIdsByBlockedId(userId));
        return result;
    }

    /**
     * 두 사용자 사이에 어느 방향으로든 차단 관계가 있으면 true를 반환한다.
     */
    public boolean isMutuallyBlocked(Long userA, Long userB) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(userA, userB)
                || userBlockRepository.existsByBlockerIdAndBlockedId(userB, userA);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }
}
