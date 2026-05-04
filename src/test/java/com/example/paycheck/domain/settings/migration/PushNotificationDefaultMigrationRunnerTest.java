package com.example.paycheck.domain.settings.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PushNotificationDefaultMigrationRunner 테스트")
class PushNotificationDefaultMigrationRunnerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("FCM 토큰이 없는 기존 사용자만 push_enabled를 false로 보정한다")
    void migrateWronglyEnabledPushSettings() {
        PushNotificationDefaultMigrationRunner runner =
                new PushNotificationDefaultMigrationRunner(jdbcTemplate);

        when(jdbcTemplate.update("""
                UPDATE user_settings us
                SET push_enabled = false
                WHERE us.push_enabled = true
                  AND NOT EXISTS (
                      SELECT 1
                      FROM fcm_token ft
                      WHERE ft.user_id = us.user_id
                  )
                """)).thenReturn(3);

        runner.migrateWronglyEnabledPushSettings();

        verify(jdbcTemplate).update("""
                UPDATE user_settings us
                SET push_enabled = false
                WHERE us.push_enabled = true
                  AND NOT EXISTS (
                      SELECT 1
                      FROM fcm_token ft
                      WHERE ft.user_id = us.user_id
                  )
                """);
    }
}
