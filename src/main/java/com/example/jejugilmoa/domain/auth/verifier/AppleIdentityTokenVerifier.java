package com.example.jejugilmoa.domain.auth.verifier;

import com.example.jejugilmoa.domain.auth.dto.AppleIdentityClaims;

public interface AppleIdentityTokenVerifier {
    AppleIdentityClaims verify(String identityToken, String rawNonce);
}
