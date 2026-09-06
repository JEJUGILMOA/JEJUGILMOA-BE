package com.example.jejugilmoa.domain.auth.dto;

import java.util.List;

public record AppleJwksResponse(List<Key> keys) {
    public record Key(String kid, String kty, String alg, String use, String n, String e) {
    }
}
