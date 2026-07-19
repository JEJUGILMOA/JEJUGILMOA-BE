package com.example.jejugilmoa.domain.auth.jwt;

import com.example.jejugilmoa.domain.user.enums.Role;

/**
 * SecurityContext에 저장되는 인증 주체.
 * JWT Claims에서 추출한 userId와 role만 담는다 — DB 조회 없이 생성.
 */
public record UserPrincipal(Long userId, Role role) {
}
