package com.example.jejugilmoa.global.config;

import com.example.jejugilmoa.domain.auth.jwt.JwtAuthenticationFilter;
import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtProvider jwtProvider) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/health",
                                "/"
                        ).permitAll()
                        .anyRequest().permitAll() // ← 개발 단계에서는 일단 허용
                )
                // ACCESS_TOKEN 쿠키가 유효하면 SecurityContext를 채워준다.
                // 인가 자체는 아직 permitAll이라 지금 당장 요구되지는 않지만,
                // 이후 컨트롤러가 인증된 사용자 식별이 필요해질 때 바로 쓸 수 있도록 미리 등록한다.
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
