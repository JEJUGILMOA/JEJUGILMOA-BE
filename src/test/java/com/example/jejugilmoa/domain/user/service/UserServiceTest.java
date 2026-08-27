package com.example.jejugilmoa.domain.user.service;

import com.example.jejugilmoa.domain.auth.repository.RefreshTokenRepository;
import com.example.jejugilmoa.domain.plan.repository.FavoriteRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.repository.NotificationSettingRepository;
import com.example.jejugilmoa.domain.user.repository.UserPreferenceRepository;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TravelRecordRepository travelRecordRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("탈퇴 처리하면 deletedAt이 기록되고 모든 리프레시 토큰이 폐기된다")
    void withdrawMarksDeletedAtAndRevokesTokens() {
        User user = User.builder()
                .id(1L)
                .nickname("제주러")
                .build();
        given(userRepository.findByIdAndDeletedAtIsNullForUpdate(1L)).willReturn(Optional.of(user));

        userService.withdraw(1L);

        assertThat(user.getDeletedAt()).isNotNull();
        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    @DisplayName("존재하지 않거나 이미 탈퇴한 회원이면 예외를 던지고 토큰은 폐기하지 않는다")
    void withdrawThrowsWhenUserNotFound() {
        given(userRepository.findByIdAndDeletedAtIsNullForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.withdraw(1L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", UserErrorCode.USER_NOT_FOUND);

        verify(refreshTokenRepository, never()).revokeAllByUserId(1L);
    }
}
