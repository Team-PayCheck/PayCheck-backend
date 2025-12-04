package com.example.wagemanager.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 관련 DTO 모음
 */
public class AuthDto {

    /**
     * 카카오 로그인 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AuthKakaoLoginRequest")
    public static class KakaoLoginRequest {

        @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
        private String kakaoAccessToken;
    }

    /**
     * 카카오 기반 회원가입 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AuthKakaoRegisterRequest")
    public static class KakaoRegisterRequest {

        @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
        private String kakaoAccessToken;

        @NotBlank(message = "사용자 유형은 필수입니다.")
        private String userType;
    }

    /**
     * 로그인 응답 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AuthLoginResponse")
    public static class LoginResponse {
        private String accessToken;
        private Long userId;
        private String name;
        private String userType;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(name = "AuthLogoutResponse")
    public static class LogoutResponse {
        private String message;

        public static LogoutResponse success() {
            return LogoutResponse.builder()
                    .message("로그아웃되었습니다.")
                    .build();
        }
    }
}
