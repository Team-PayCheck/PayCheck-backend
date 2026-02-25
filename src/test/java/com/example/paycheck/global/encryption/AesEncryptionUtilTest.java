package com.example.paycheck.global.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class AesEncryptionUtilTest {

    private static final String TEST_KEY = "r7KiwvQzjnw3G6xvp6iSYYgOVX25uQE1GJ278qA2kEE=";

    private AesEncryptionUtil aesEncryptionUtil;

    @BeforeEach
    void setUp() {
        aesEncryptionUtil = new AesEncryptionUtil(TEST_KEY);
    }

    @Test
    @DisplayName("암호화 후 복호화하면 원본 텍스트와 동일하다")
    void encryptAndDecrypt_roundTrip() {
        String plainText = "1234-5678-9012";

        String encrypted = aesEncryptionUtil.encrypt(plainText);
        String decrypted = aesEncryptionUtil.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("같은 평문을 두 번 암호화하면 서로 다른 암호문이 생성된다")
    void encrypt_sameInput_differentOutput() {
        String plainText = "333312341234";

        String encrypted1 = aesEncryptionUtil.encrypt(plainText);
        String encrypted2 = aesEncryptionUtil.encrypt(plainText);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("null 입력 시 null을 반환한다")
    void encrypt_null_returnsNull() {
        assertThat(aesEncryptionUtil.encrypt(null)).isNull();
        assertThat(aesEncryptionUtil.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("빈 문자열 입력 시 빈 문자열을 반환한다")
    void encrypt_emptyString_returnsEmpty() {
        assertThat(aesEncryptionUtil.encrypt("")).isEmpty();
        assertThat(aesEncryptionUtil.decrypt("")).isEmpty();
    }

    @Test
    @DisplayName("다양한 계좌번호 형식을 암호화/복호화할 수 있다")
    void encryptAndDecrypt_variousFormats() {
        String[] accountNumbers = {"333312341234", "1111-2222-3333", "110-123-456789"};

        for (String account : accountNumbers) {
            String encrypted = aesEncryptionUtil.encrypt(account);
            String decrypted = aesEncryptionUtil.decrypt(encrypted);
            assertThat(decrypted).isEqualTo(account);
        }
    }

    @Test
    @DisplayName("암호화 결과는 유효한 Base64 문자열이다")
    void encrypt_resultIsBase64() {
        String encrypted = aesEncryptionUtil.encrypt("1234567890");

        assertThatCode(() -> Base64.getDecoder().decode(encrypted))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("잘못된 키 길이로 생성 시 예외가 발생한다")
    void constructor_invalidKeyLength_throwsException() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new AesEncryptionUtil(shortKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32바이트");
    }

    @Test
    @DisplayName("잘못된 암호문으로 복호화 시 EncryptionException이 발생한다")
    void decrypt_invalidCipherText_throwsException() {
        String invalidCipherText = Base64.getEncoder().encodeToString("invalid-data".getBytes());

        assertThatThrownBy(() -> aesEncryptionUtil.decrypt(invalidCipherText))
                .isInstanceOf(EncryptionException.class);
    }

    @Test
    @DisplayName("다른 키로 복호화 시 EncryptionException이 발생한다")
    void decrypt_differentKey_throwsException() {
        String encrypted = aesEncryptionUtil.encrypt("1234567890");

        String otherKey = Base64.getEncoder().encodeToString(new byte[32]);
        AesEncryptionUtil otherUtil = new AesEncryptionUtil(otherKey);

        assertThatThrownBy(() -> otherUtil.decrypt(encrypted))
                .isInstanceOf(EncryptionException.class);
    }
}
