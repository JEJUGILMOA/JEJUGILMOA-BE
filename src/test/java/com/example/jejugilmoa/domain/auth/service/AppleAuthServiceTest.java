package com.example.jejugilmoa.domain.auth.service;

import com.example.jejugilmoa.domain.auth.dto.*;
import com.example.jejugilmoa.domain.auth.enums.SocialProvider;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.domain.auth.verifier.AppleIdentityTokenVerifier;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppleAuthServiceTest {
    AppleIdentityTokenVerifier verifier = mock(AppleIdentityTokenVerifier.class);
    AuthService auth = mock(AuthService.class);
    AppleAuthService service = new AppleAuthService(verifier, auth);

    @Test void forwardsOnlyVerifiedIdentity() {
        when(verifier.verify("token", "nonce")).thenReturn(new AppleIdentityClaims("apple-sub", "a@example.com"));
        service.login(new AppleLoginRequest("token", "nonce"));
        verify(auth).loginWithVerifiedIdentity(new OAuthUserInfo(
                SocialProvider.APPLE, "apple-sub", "애플 사용자", null, "a@example.com"));
    }

    @Test void neverTouchesMembersWhenVerificationFails() {
        when(verifier.verify("token", "nonce"))
                .thenThrow(new GeneralException(AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN));
        assertThatThrownBy(() -> service.login(new AppleLoginRequest("token", "nonce")))
                .isInstanceOf(GeneralException.class);
        verifyNoInteractions(auth);
    }
}
