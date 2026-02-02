package com.example.paycheck.domain.settings.enums;

public enum NotificationChannel {
    PUSH("push"),
    EMAIL("email"),
    SMS("sms");

    private final String value;

    NotificationChannel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
