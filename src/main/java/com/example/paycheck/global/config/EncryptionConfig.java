package com.example.paycheck.global.config;

import com.example.paycheck.global.encryption.AesEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Bean
    public AesEncryptionUtil aesEncryptionUtil(@Value("${encryption.aes-key}") String aesKey) {
        return new AesEncryptionUtil(aesKey);
    }
}
