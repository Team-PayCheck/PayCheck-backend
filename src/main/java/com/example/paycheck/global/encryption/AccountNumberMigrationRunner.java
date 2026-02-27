package com.example.paycheck.global.encryption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 기존 평문 계좌번호를 AES-256-GCM으로 암호화하는 일회성 마이그레이션 러너.
 * 마이그레이션 완료 후 이 클래스를 제거하세요.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class AccountNumberMigrationRunner {

    private static final int GCM_MIN_ENCRYPTED_LENGTH = 28;

    private final JdbcTemplate jdbcTemplate;
    private final AesEncryptionUtil aesEncryptionUtil;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateAccountNumbers() {
        log.info("=== 계좌번호 암호화 마이그레이션 시작 ===");

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, account_number FROM worker WHERE account_number IS NOT NULL AND account_number != ''"
            );

            int migrated = 0;
            for (Map<String, Object> row : rows) {
                Long id = ((Number) row.get("id")).longValue();
                String accountNumber = (String) row.get("account_number");

                if (isAlreadyEncrypted(accountNumber)) {
                    continue;
                }

                String encrypted = aesEncryptionUtil.encrypt(accountNumber);
                jdbcTemplate.update(
                        "UPDATE worker SET account_number = ? WHERE id = ?",
                        encrypted, id
                );
                migrated++;
            }

            log.info("=== 계좌번호 암호화 마이그레이션 완료: {}건 처리 ===", migrated);
        } catch (Exception e) {
            log.error("=== 계좌번호 암호화 마이그레이션 실패 ===", e);
        }
    }

    private boolean isAlreadyEncrypted(String value) {
        if (value == null || value.length() < 40) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length >= GCM_MIN_ENCRYPTED_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
