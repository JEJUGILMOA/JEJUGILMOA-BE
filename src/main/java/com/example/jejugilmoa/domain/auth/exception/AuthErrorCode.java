package com.example.jejugilmoa.domain.auth.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseCode {
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH400_1", "지원하지 않는 소셜 로그인 제공자입니다."),
    INVALID_AUTHORIZATION_CODE(HttpStatus.BAD_REQUEST, "AUTH400_2", "소셜 로그인 인가코드가 유효하지 않습니다."),
    INVALID_OAUTH_PROFILE(HttpStatus.BAD_GATEWAY, "AUTH502_1", "소셜 사용자 정보 응답이 올바르지 않습니다."),
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH502_2", "소셜 로그인 제공자와 통신하지 못했습니다."),

    MISSING_OAUTH_CONFIGURATION(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_1", "소셜 로그인 설정이 누락되었습니다."),
    MISSING_OAUTH_CLIENT_ID(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_2", "소셜 로그인 클라이언트 ID가 누락되었습니다."),
    MISSING_OAUTH_CLIENT_SECRET(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_3", "소셜 로그인 클라이언트 시크릿이 누락되었습니다."),
    MISSING_OAUTH_REDIRECT_URI(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_4", "소셜 로그인 리디렉션 URI가 누락되었습니다."),
    MISSING_OAUTH_TOKEN_URI(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_5", "소셜 로그인 토큰 URI가 누락되었습니다."),
    MISSING_OAUTH_USER_INFO_URI(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_6", "소셜 로그인 사용자 정보 URI가 누락되었습니다."),


    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_2", "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH401_3", "리프레시 토큰이 존재하지 않습니다."),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "AUTH401_4", "이미 사용되었거나 탈취 의심되는 리프레시 토큰입니다. 다시 로그인해주세요."),

    INVALID_APPLE_IDENTITY_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_5", "유효하지 않은 Apple 인증 토큰입니다."),
    EXPIRED_APPLE_IDENTITY_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_6", "Apple 인증 토큰이 만료되었습니다. 다시 로그인해주세요."),
    INVALID_APPLE_NONCE(HttpStatus.UNAUTHORIZED, "AUTH401_7", "Apple 로그인 요청이 유효하지 않습니다. 다시 로그인해주세요."),

    APPLE_IDENTITY_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "AUTH401_8", "이미 사용된 Apple 인증 토큰입니다. 다시 로그인해주세요."),
    APPLE_REPLAY_STORE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AUTH503_1", "Apple 로그인 인증을 일시적으로 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),

    DEV_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH409_1", "이미 가입된 이메일입니다."),
    DEV_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404_1", "개발용 유저를 찾을 수 없습니다. 먼저 회원가입을 해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
