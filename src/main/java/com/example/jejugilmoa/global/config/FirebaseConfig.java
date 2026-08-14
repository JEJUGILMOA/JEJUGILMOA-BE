package com.example.jejugilmoa.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Slf4j
@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        String encoded = properties.credentials().encoded();
        if (encoded == null || encoded.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS_JSON이 설정되지 않아 Firebase 초기화를 건너뜁니다.");
            return null;
        }

        byte[] decoded = Base64.getDecoder().decode(encoded);
        try (InputStream serviceAccount = new ByteArrayInputStream(decoded)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            log.info("Firebase 초기화 완료");
            return FirebaseApp.initializeApp(options);
        }
    }
}
