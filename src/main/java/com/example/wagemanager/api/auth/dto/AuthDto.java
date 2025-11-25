package com.example.wagemanager.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 관련 DTO 모음
 */
public class AuthDto {

    /**
     * 개발용 임시 로그인 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DevLoginRequest {
        private Long userId;
    }

    /**
     * 로그인 응답 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginResponse {
        private String accessToken;
        private Long userId;
        private String name;
        private String userType;
    }
}
