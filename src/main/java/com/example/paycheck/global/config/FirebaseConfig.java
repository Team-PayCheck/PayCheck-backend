package com.example.paycheck.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-file:}")
    private String serviceAccountFile;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (serviceAccountFile == null || serviceAccountFile.isBlank()) {
            log.warn("Firebase 서비스 계정 파일이 설정되지 않았습니다 - FCM 푸시 알림이 비활성화됩니다");
            return null;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = new ClassPathResource(serviceAccountFile).getInputStream();
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase 초기화 완료");
            }
            return FirebaseMessaging.getInstance();
        } catch (IOException e) {
            log.warn("Firebase 초기화 실패 - FCM 푸시 알림이 비활성화됩니다: {}", e.getMessage());
            return null;
        }
    }
}
