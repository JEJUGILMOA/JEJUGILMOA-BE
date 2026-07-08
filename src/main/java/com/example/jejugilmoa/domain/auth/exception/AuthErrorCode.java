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
    MISSING_OAUTH_USER_INFO_URI(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_6", "소셜 로그인 사용자 정보 URI가 누락되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
