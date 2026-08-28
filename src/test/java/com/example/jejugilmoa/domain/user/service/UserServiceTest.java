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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

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

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-08-02T12:34:56.789Z"), ZoneOffset.UTC);

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

    @Test
    @DisplayName("JVM 기본 시간대가 UTC가 아니어도 탈퇴 시각은 Clock 기준 UTC로 기록된다")
    void withdrawUsesUtcClockRegardlessOfDefaultTimeZone() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        try {
            User user = User.builder().id(1L).nickname("제주러").build();
            given(userRepository.findByIdAndDeletedAtIsNullForUpdate(1L)).willReturn(Optional.of(user));

            userService.withdraw(1L);

            assertThat(user.getDeletedAt())
                    .isEqualTo(LocalDateTime.of(2026, 8, 2, 12, 34, 56, 789_000_000));
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    @DisplayName("탈퇴 30일 경과 회원은 JVM 기본 시간대와 무관하게 Clock 기준 UTC 시각으로 익명화된다")
    void anonymizeExpiredWithdrawalsUsesUtcClockRegardlessOfDefaultTimeZone() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        try {
            User expiredUser = User.builder().id(2L).nickname("탈퇴예정").build();
            LocalDateTime cutoff = LocalDateTime.of(2026, 7, 3, 4, 0);
            given(userRepository.findByDeletedAtBeforeAndAnonymizedAtIsNullForUpdate(cutoff))
                    .willReturn(List.of(expiredUser));

            int anonymizedCount = userService.anonymizeExpiredWithdrawals(cutoff);

            assertThat(anonymizedCount).isEqualTo(1);
            assertThat(expiredUser.getAnonymizedAt())
                    .isEqualTo(LocalDateTime.of(2026, 8, 2, 12, 34, 56, 789_000_000));
            assertThat(expiredUser.getNickname()).isEqualTo("탈퇴한 사용자");
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    @DisplayName("탈퇴 상태의 소셜 계정을 행 잠금 조회로 찾아 복구한다")
    void restoreByExternalAccountRestoresWithdrawnUser() {
        User withdrawnUser = User.builder().id(3L).nickname("탈퇴자").build();
        withdrawnUser.withdraw(LocalDateTime.of(2026, 7, 20, 10, 0));
        given(userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNotNullForUpdate("kakao", "12345"))
                .willReturn(Optional.of(withdrawnUser));

        Optional<User> restored = userService.restoreByExternalAccount("kakao", "12345");

        assertThat(restored).containsSame(withdrawnUser);
        assertThat(withdrawnUser.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("익명화되었거나 이미 복구된 계정은 탈퇴 상태 조회에서 제외되어 빈 Optional을 반환한다")
    void restoreByExternalAccountReturnsEmptyWhenNoWithdrawnAccount() {
        given(userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNotNullForUpdate("kakao", "gone"))
                .willReturn(Optional.empty());

        assertThat(userService.restoreByExternalAccount("kakao", "gone")).isEmpty();
    }
}
