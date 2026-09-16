package com.example.jejugilmoa.global.config;

import com.example.jejugilmoa.domain.auth.jwt.JwtAuthenticationFilter;
import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.global.apiPayload.code.GeneralErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final Environment environment;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtProvider jwtProvider) throws Exception {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> {
                    // 비회원 공개 GET 엔드포인트
                    auth.requestMatchers(HttpMethod.GET, "/api/shared/plans/**").permitAll();
                    // /api/records/favorites 는 인증 필요 — wildcards보다 먼저 선언해야 first-match 보장
                    auth.requestMatchers(HttpMethod.GET, "/api/records/favorites").authenticated();
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/home/places",
                            "/api/home/courses",
                            "/api/places",
                            "/api/places/**",
                            "/api/courses/recommended",
                            "/api/courses/recommended/**",
                            "/api/plans/*/routes",
                            "/api/plans/*",
                            "/api/records",
                            "/api/records/*"
                    ).permitAll();
                    auth.requestMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/health",
                            "/",
                            "/api/auth/**",
                            "/api/map/**"
                    ).permitAll();

                    if (isProd) {
                        auth.requestMatchers("/dev/auth/**").denyAll();
                    } else {
                        auth.requestMatchers("/dev/auth/**").permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, GeneralErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN, GeneralErrorCode.FORBIDDEN))
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJson(HttpServletResponse response, int status, GeneralErrorCode code) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"isSuccess\":false,\"code\":\"" + code.getCode() + "\",\"message\":\"" + code.getMessage() + "\",\"result\":null}"
        );
    }
}
