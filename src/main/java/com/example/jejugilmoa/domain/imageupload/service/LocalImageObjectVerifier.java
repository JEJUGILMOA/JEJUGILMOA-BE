package com.example.jejugilmoa.domain.imageupload.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalImageObjectVerifier implements ImageObjectVerifier {

    @Override
    public void verify(String objectKey) {
        // local 프로필에서는 외부 S3 객체 검증을 생략한다.
    }
}
