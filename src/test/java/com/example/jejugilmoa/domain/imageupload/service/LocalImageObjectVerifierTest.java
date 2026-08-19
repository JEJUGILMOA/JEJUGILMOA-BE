package com.example.jejugilmoa.domain.imageupload.service;

import org.junit.jupiter.api.Test;

class LocalImageObjectVerifierTest {

    private final LocalImageObjectVerifier verifier = new LocalImageObjectVerifier();

    @Test
    void verify_succeedsWithoutExternalCall() {
        verifier.verify("records/42/image.jpg");
    }
}
