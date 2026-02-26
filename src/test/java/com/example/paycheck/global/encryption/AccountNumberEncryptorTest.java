package com.example.paycheck.global.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AccountNumberEncryptorTest {

    private static final String TEST_KEY = "r7KiwvQzjnw3G6xvp6iSYYgOVX25uQE1GJ278qA2kEE=";

    private AccountNumberEncryptor encryptor;

    @BeforeEach
    void setUp() {
        AesEncryptionUtil aesEncryptionUtil = new AesEncryptionUtil(TEST_KEY);
        encryptor = new AccountNumberEncryptor(aesEncryptionUtil);
    }

    @Test
    @DisplayName("convertToDatabaseColumn: 정상 계좌번호를 암호화한다")
    void convertToDatabaseColumn_encryptsValue() {
        String plainAccount = "333312341234";

        String encrypted = encryptor.convertToDatabaseColumn(plainAccount);

        assertThat(encrypted).isNotEqualTo(plainAccount);
        assertThat(encrypted).isNotBlank();
    }

    @Test
    @DisplayName("convertToEntityAttribute: 암호문을 정상 복호화한다")
    void convertToEntityAttribute_decryptsValue() {
        String plainAccount = "1111-2222-3333";
        String encrypted = encryptor.convertToDatabaseColumn(plainAccount);

        String decrypted = encryptor.convertToEntityAttribute(encrypted);

        assertThat(decrypted).isEqualTo(plainAccount);
    }

    @Test
    @DisplayName("convertToDatabaseColumn: null 입력 시 null을 반환한다")
    void convertToDatabaseColumn_null_returnsNull() {
        assertThat(encryptor.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    @DisplayName("convertToEntityAttribute: null 입력 시 null을 반환한다")
    void convertToEntityAttribute_null_returnsNull() {
        assertThat(encryptor.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("convertToDatabaseColumn: 빈 문자열 입력 시 빈 문자열을 반환한다")
    void convertToDatabaseColumn_empty_returnsEmpty() {
        assertThat(encryptor.convertToDatabaseColumn("")).isEmpty();
    }

    @Test
    @DisplayName("convertToEntityAttribute: 빈 문자열 입력 시 빈 문자열을 반환한다")
    void convertToEntityAttribute_empty_returnsEmpty() {
        assertThat(encryptor.convertToEntityAttribute("")).isEmpty();
    }

    @Test
    @DisplayName("라운드트립: DB 저장 후 조회 시 원본과 동일하다")
    void roundTrip_columnToEntityAndBack() {
        String original = "110-123-456789";

        String dbValue = encryptor.convertToDatabaseColumn(original);
        String restored = encryptor.convertToEntityAttribute(dbValue);

        assertThat(restored).isEqualTo(original);
    }
}
