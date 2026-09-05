package com.example.jejugilmoa.domain.auth.service;

import com.example.jejugilmoa.domain.auth.converter.AuthConverter;
import com.example.jejugilmoa.domain.auth.dto.AppleLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.verifier.AppleIdentityTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppleAuthService {
    private final AppleIdentityTokenVerifier verifier;
    private final AuthService authService;

    // 공통 회원 처리의 유니크 제약 충돌 재시도를 위해 전체 트랜잭션을 만들지 않는다.
    public OAuthLoginResponse login(AppleLoginRequest request) {
        var identity = verifier.verify(request.identityToken(), request.rawNonce());
        return authService.loginWithVerifiedIdentity(AuthConverter.toUserInfo(identity));
    }
}
