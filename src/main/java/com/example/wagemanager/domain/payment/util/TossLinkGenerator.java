package com.example.wagemanager.domain.payment.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TossLinkGenerator {

    private static final String BASE_SCHEME = "supertoss://send";
    private static final String DEFAULT_ORIGIN = "wage-manager";

    public static String generateSupertossLink(String bankName, String accountNumber, BigDecimal amount) {
        String encodedBank = encode(bankName);
        String encodedAccount = encode(stripHyphen(accountNumber));
        String encodedAmount = encode(normalizeAmount(amount));
        String encodedOrigin = encode(DEFAULT_ORIGIN);

        return String.format(
                "%s?accountNo=%s&bank=%s&amount=%s&origin=%s",
                BASE_SCHEME,
                encodedAccount,
                encodedBank,
                encodedAmount,
                encodedOrigin
        );
    }

    private static String normalizeAmount(BigDecimal amount) {
        BigDecimal normalized = amount.setScale(0, RoundingMode.HALF_UP);
        return normalized.toPlainString();
    }

    private static String stripHyphen(String value) {
        return value == null ? null : value.replaceAll("-", "");
    }

    private static String encode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
