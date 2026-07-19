package com.example.jejugilmoa.domain.auth.jwt;

import com.example.jejugilmoa.domain.user.enums.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// ACCESS_TOKEN 쿠키가 유효하면 SecurityContext에 인증 정보를 채워 넣는다.
// 무효/만료 토큰이어도 요청 자체는 막지 않는다 — 인가 정책(permitAll)은 SecurityConfig가 별도로 결정한다.
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        extractAccessToken(request).ifPresent(this::authenticate);
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtProvider.parseAccessClaims(token);
            Long userId = jwtProvider.getUserId(claims);
            Role role = Role.valueOf(claims.get("role", String.class));

            UserPrincipal principal = new UserPrincipal(userId, role);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.debug("액세스 토큰이 유효하지 않아 인증을 설정하지 않습니다: {}", e.getMessage());
        }
    }

    private Optional<String> extractAccessToken(HttpServletRequest request) {
        // Authorization: Bearer 헤더 우선 (Swagger UI 등 개발 도구)
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        // 쿠키 (프론트엔드)
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> CookieProvider.ACCESS_TOKEN_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
