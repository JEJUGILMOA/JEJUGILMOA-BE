package com.example.jejugilmoa.domain.auth.support;

import com.example.jejugilmoa.domain.auth.config.AppleAuthProperties;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Set;

public final class AppleTokenFixture {
    public static final String NONCE = "random-raw-nonce-for-apple-login-123456789";
    public static final String ISSUER = "https://appleid.apple.com";
    public static final String AUDIENCE = "com.jejugilmoa.ios.test";
    public static final String KID = "apple-test-key";
    public static final KeyPair KEYS = Jwts.SIG.RS256.keyPair().build();

    private AppleTokenFixture() {
    }

    public static AppleAuthProperties properties(String uri) {
        return new AppleAuthProperties(ISSUER, URI.create(uri), Set.of(AUDIENCE));
    }

    public static JwtBuilder token() {
        return Jwts.builder().header().keyId(KID).and().issuer(ISSUER)
                .audience().add(AUDIENCE).and().subject("apple-test-sub")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .claim("nonce", hash(NONCE)).claim("email", "apple@example.com")
                .claim("email_verified", "true");
    }

    public static String sign(JwtBuilder builder) {
        return builder.signWith(KEYS.getPrivate(), Jwts.SIG.RS256).compact();
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String jwks(String kid) {
        var key = (RSAPublicKey) KEYS.getPublic();
        return """
                {"keys":[{"kid":"%s","kty":"RSA","alg":"RS256","use":"sig","n":"%s","e":"%s"}]}
                """.formatted(kid, encode(key.getModulus().toByteArray()), encode(key.getPublicExponent().toByteArray()));
    }

    private static String encode(byte[] bytes) {
        if (bytes[0] == 0) {
            bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
