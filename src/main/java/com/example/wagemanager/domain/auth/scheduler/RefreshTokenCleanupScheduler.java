package com.example.wagemanager.domain.auth.scheduler;

import com.example.wagemanager.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료된 Refresh Token을 주기적으로 삭제하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 매일 새벽 3시에 만료된 Refresh Token 삭제
     * cron 표현식: 초(0) 분(0) 시(0) 일(*) 월(*) 요일(*)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("만료된 Refresh Token 정리 작업 시작");

        try {
            LocalDateTime now = LocalDateTime.now();
            refreshTokenRepository.deleteByExpiresAtBefore(now);

            log.info("만료된 Refresh Token 정리 작업 완료");
        } catch (Exception e) {
            log.error("만료된 Refresh Token 정리 중 오류 발생", e);
        }
    }
}
