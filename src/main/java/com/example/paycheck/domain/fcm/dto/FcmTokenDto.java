package com.example.paycheck.domain.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class FcmTokenDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FcmTokenRegisterRequest")
    public static class RegisterRequest {
        @NotBlank(message = "FCM 토큰은 필수입니다")
        @Schema(description = "Firebase Cloud Messaging 디바이스 토큰")
        private String token;

        @Schema(description = "디바이스 정보 (선택)")
        private String deviceInfo;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FcmTokenDeleteRequest")
    public static class DeleteRequest {
        @NotBlank(message = "FCM 토큰은 필수입니다")
        @Schema(description = "삭제할 FCM 토큰")
        private String token;
    }
}
