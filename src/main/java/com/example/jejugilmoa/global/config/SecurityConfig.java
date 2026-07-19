package com.example.jejugilmoa.global.config;

import com.example.jejugilmoa.domain.auth.jwt.JwtAuthenticationFilter;
import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.global.apiPayload.code.GeneralErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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
                                "/",
                                "/api/auth/**",
                                "/dev/auth/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
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
