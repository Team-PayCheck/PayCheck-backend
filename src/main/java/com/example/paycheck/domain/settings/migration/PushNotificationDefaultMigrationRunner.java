package com.example.paycheck.domain.settings.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 기본값 버그로 원치 않게 push_enabled=true가 저장된 기존 사용자를 보정합니다.
 * FCM 토큰이 없는 사용자만 대상으로 하여 실제 푸시 사용자의 설정은 유지합니다.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "migration.push-settings-fix.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class PushNotificationDefaultMigrationRunner {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateWronglyEnabledPushSettings() {
        log.info("=== 푸시 알림 기본값 보정 마이그레이션 시작 ===");

        try {
            int updated = jdbcTemplate.update("""
                    UPDATE user_settings us
                    SET push_enabled = false
                    WHERE us.push_enabled = true
                      AND NOT EXISTS (
                          SELECT 1
                          FROM fcm_token ft
                          WHERE ft.user_id = us.user_id
                      )
                    """);

            log.info("=== 푸시 알림 기본값 보정 마이그레이션 완료: {}건 처리 ===", updated);
        } catch (Exception e) {
            log.error("=== 푸시 알림 기본값 보정 마이그레이션 실패 ===", e);
        }
    }
}
