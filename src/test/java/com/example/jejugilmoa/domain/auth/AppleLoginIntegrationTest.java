package com.example.jejugilmoa.domain.auth;

import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.domain.auth.repository.RefreshTokenRepository;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.example.jejugilmoa.domain.auth.support.AppleTokenFixture.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.sync.run-on-startup=false", "jwt.cookie-secure=true"})
class AppleLoginIntegrationTest {
    static final HttpServer JWKS = startJwks();
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired JwtProvider jwt;
    @Autowired JdbcClient jdbc;
    final HttpClient http = HttpClient.newHttpClient();
    final ObjectMapper mapper = new ObjectMapper();
    final String subject = "apple-integration-" + UUID.randomUUID();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.apple.allowed-audiences", () -> AUDIENCE);
        registry.add("app.apple.jwks-uri", () -> "http://localhost:" + JWKS.getAddress().getPort() + "/keys");
    }

    @AfterAll static void stopJwks() {
        JWKS.stop(0);
    }

    @AfterEach void cleanUpCreatedMember() {
        users.findByExternalProviderAndExternalIdAndDeletedAtIsNull("apple", subject).ifPresent(user -> {
            jdbc.sql("DELETE FROM refresh_token WHERE user_id = :id").param("id", user.getId()).update();
            users.deleteById(user.getId());
        });
    }

    @Test void realHttpSignupExistingLoginAndRefreshUseExistingJwtFlow() throws Exception {
        String identity = sign(token().subject(subject));
        var first = login(identity, NONCE);
        assertThat(first.statusCode()).isEqualTo(200);
        var body = mapper.readTree(first.body());
        assertThat(body.path("isSuccess").asBoolean()).isTrue();
        assertThat(body.path("code").asText()).isEqualTo("COMMON200");
        assertThat(body.path("result").path("newUser").asBoolean()).isTrue();
        long userId = body.path("result").path("userId").asLong();
        var user = users.findByExternalProviderAndExternalIdAndDeletedAtIsNull("apple", subject).orElseThrow();
        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getEmail()).isEqualTo("apple@example.com");
        assertThat(user.getNickname()).isEqualTo("애플 사용자");
        assertThat(first.headers().allValues("Set-Cookie")).hasSize(2)
                .allSatisfy(cookie -> assertThat(cookie).contains("HttpOnly", "Secure", "Path=/", "SameSite=Lax"));
        String access = cookie(first, "ACCESS_TOKEN");
        assertThat(jwt.getUserId(jwt.parseAccessClaims(access))).isEqualTo(userId);
        String refresh = cookie(first, "REFRESH_TOKEN");
        String tokenId = jwt.parseRefreshClaims(refresh).getId();
        assertThat(refreshTokens.findByTokenId(tokenId)).isPresent();

        var second = login(identity, NONCE);
        assertThat(second.statusCode()).isEqualTo(200);
        var secondBody = mapper.readTree(second.body());
        assertThat(secondBody.path("result").path("newUser").asBoolean()).isFalse();
        assertThat(secondBody.path("result").path("userId").asLong()).isEqualTo(userId);

        var rotated = http.send(HttpRequest.newBuilder(uri("/api/auth/reissue"))
                .header("Cookie", "REFRESH_TOKEN=" + refresh).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(rotated.statusCode()).isEqualTo(200);
        assertThat(refreshTokens.findByTokenId(tokenId).orElseThrow().isRevoked()).isTrue();
        assertThat(cookie(rotated, "REFRESH_TOKEN")).isNotEqualTo(refresh);
    }

    @Test void realHttpBadNonceHasNoMemberOrCookies() throws Exception {
        var response = login(sign(token().subject(subject)), "wrong-nonce-".repeat(4));
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(mapper.readTree(response.body()).path("code").asText()).isEqualTo("AUTH401_7");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(users.findByExternalProviderAndExternalIdAndDeletedAtIsNull("apple", subject)).isEmpty();
    }

    private HttpResponse<String> login(String identity, String nonce) throws Exception {
        String body = mapper.writeValueAsString(java.util.Map.of("identityToken", identity, "rawNonce", nonce));
        return http.send(HttpRequest.newBuilder(uri("/api/auth/apple/login"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String cookie(HttpResponse<?> response, String name) {
        return response.headers().allValues("Set-Cookie").stream().filter(value -> value.startsWith(name + "="))
                .findFirst().orElseThrow().split(";", 2)[0].substring(name.length() + 1);
    }

    private static HttpServer startJwks() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/keys", exchange -> {
                byte[] body = jwks(KID).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });
            server.start();
            return server;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
