package com.example.paycheck.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

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

        @NotBlank(message = "이름은 필수입니다.")
        private String name;

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
        private String refreshToken;
        private Long userId;
        private String name;
        private String userType;
    }

    /**
     * 카카오 로그인 결과 (정상 로그인 또는 탈퇴 계정 발견)
     * 정상 로그인 필드는 기존 클라이언트 호환을 위해 top-level에 평탄하게 둔다.
     * - status="LOGGED_IN": accessToken/refreshToken/userId/name/userType 채움
     * - status="WITHDRAWN_PENDING": withdrawnAccount만 채움
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "AuthKakaoLoginResult")
    public static class KakaoLoginResult {
        @Schema(description = "로그인 결과 상태", allowableValues = {"LOGGED_IN", "WITHDRAWN_PENDING"})
        private String status;

        // 정상 로그인(LOGGED_IN) 시 채워지는 필드 — 기존 LoginResponse와 동일 경로 유지
        private String accessToken;
        private String refreshToken;
        private Long userId;
        private String name;
        private String userType;

        @Schema(description = "탈퇴 계정 발견 시 안내 정보")
        private WithdrawnAccountInfo withdrawnAccount;

        public static KakaoLoginResult loggedIn(LoginResponse login) {
            return KakaoLoginResult.builder()
                    .status("LOGGED_IN")
                    .accessToken(login.getAccessToken())
                    .refreshToken(login.getRefreshToken())
                    .userId(login.getUserId())
                    .name(login.getName())
                    .userType(login.getUserType())
                    .build();
        }

        public static KakaoLoginResult withdrawnPending(WithdrawnAccountInfo withdrawnAccount) {
            return KakaoLoginResult.builder()
                    .status("WITHDRAWN_PENDING")
                    .withdrawnAccount(withdrawnAccount)
                    .build();
        }
    }

    /**
     * 탈퇴 계정 안내 정보
     * 클라이언트가 사용자에게 "기존 계정을 복구할지 / 완전 삭제 후 새로 가입할지" 선택을 안내할 때 사용.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AuthWithdrawnAccountInfo")
    public static class WithdrawnAccountInfo {
        private String name;
        private String userType;
        private LocalDateTime withdrawnAt;
        private String profileImageUrl;
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
     * 토큰 갱신 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "AuthRefreshRequest")
    public static class RefreshRequest {
        @NotBlank(message = "Refresh Token은 필수입니다.")
        private String refreshToken;
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
        private String refreshToken;
    }

    /**
     * 회원 탈퇴 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(name = "AuthWithdrawResponse")
    public static class WithdrawResponse {
        private String message;

        public static WithdrawResponse success() {
            return WithdrawResponse.builder()
                    .message("회원 탈퇴가 완료되었습니다.")
                    .build();
        }
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
