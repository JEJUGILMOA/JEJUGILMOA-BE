package com.example.jejugilmoa.domain.auth.service;

import com.example.jejugilmoa.domain.auth.client.SocialOAuthClient;
import com.example.jejugilmoa.domain.auth.converter.AuthConverter;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.dto.OAuthUserInfo;
import com.example.jejugilmoa.domain.auth.enums.SocialProvider;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SocialOAuthClient socialOAuthClient;
    private final UserRepository userRepository;

    // 전체를 하나의 트랜잭션으로 묶지 않는다. 동시 로그인 경합으로 createUser()가
    // 유니크 제약을 위반하면 재조회로 복구하는데, 같은 트랜잭션(커넥션) 안에서는
    // 위반 시점에 이미 커넥션이 abort 상태라 재조회 자체가 실패한다.
    // find/save 각각은 스프링 데이터 JPA가 별도 트랜잭션으로 처리해준다.
    public OAuthLoginResponse login(String providerValue, OAuthLoginRequest request) {
        SocialProvider provider = SocialProvider.from(providerValue);
        OAuthUserInfo userInfo = socialOAuthClient.fetchUserInfo(provider, request);

        try {
            return findOrCreateUser(userInfo);
        } catch (DataIntegrityViolationException ex) {
            // 두 요청이 동시에 신규 유저로 판단해 저장을 시도하면 uk_user_provider_external_id
            // 제약을 나중에 커밋한 쪽이 위반한다. 새 트랜잭션으로 재조회하면 먼저 커밋된
            // 유저를 찾을 수 있으므로 한 번만 재시도한다.
            return findOrCreateUser(userInfo);
        }
    }

    private OAuthLoginResponse findOrCreateUser(OAuthUserInfo userInfo) {
        return userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull(
                userInfo.provider().getKey(),
                userInfo.externalId()
            )
            .map(user -> AuthConverter.toResponse(user, false))
            .orElseGet(() -> createUser(userInfo));
    }

    private OAuthLoginResponse createUser(OAuthUserInfo userInfo) {
        User user = userRepository.save(AuthConverter.toUser(userInfo));
        return AuthConverter.toResponse(user, true);
    }
}
