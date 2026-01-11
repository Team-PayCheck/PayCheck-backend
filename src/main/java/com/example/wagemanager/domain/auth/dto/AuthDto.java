package com.example.wagemanager.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

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

        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^01[0-9]-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
        private String phone;

        private String bankName; // WORKER 타입인 경우 필수
        private String accountNumber; // WORKER 타입인 경우 필수

        @Builder.Default
        private String profileImageUrl = "https://via.placeholder.com/150/CCCCCC/FFFFFF?text=User";

        @AssertTrue(message = "근로자 타입은 은행명과 계좌번호가 필수입니다.")
        @JsonIgnore
        @Schema(hidden = true)
        public boolean isBankInfoProvidedForWorker() {
            if (!StringUtils.hasText(userType)) {
                return true;
            }
            String normalizedUserType = userType.trim();
            boolean isWorker = "WORKER".equalsIgnoreCase(normalizedUserType);
            boolean hasBankName = StringUtils.hasText(bankName);
            boolean hasAccountNumber = StringUtils.hasText(accountNumber);
            return !isWorker || (hasBankName && hasAccountNumber);
        }
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


    /**
     * 토큰 갱신 응답 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AuthRefreshResponse")
    public static class RefreshResponse {
        private String accessToken;
    }

    /**
     * 개발용 임시 로그인 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AuthDevLoginRequest")
    public static class DevLoginRequest {
        
        @NotBlank(message = "사용자 ID는 필수입니다.")
        @Schema(example = "1")
        private String userId;

        @NotBlank(message = "사용자 이름은 필수입니다.")
        @Schema(example = "테스트 사용자")
        private String name;

        @NotBlank(message = "사용자 유형은 필수입니다.")
        @Schema(example = "WORKER")
        private String userType;
    }
}
