package com.example.wagemanager.domain.user.dto;

import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "UserResponse")
    public static class Response {
        private Long id;
        private String kakaoId;
        private String name;
        private String phone;
        private UserType userType;
        private String profileImageUrl;

        public static Response from(User user) {
            return Response.builder()
                    .id(user.getId())
                    .kakaoId(user.getKakaoId())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .userType(user.getUserType())
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "UserUpdateRequest")
    public static class UpdateRequest {
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요.")
        private String name;

        @Pattern(regexp = "^01[0-9]-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
        private String phone;

        private String profileImageUrl;
    }

    /**
     * 내부 전용 DTO - 서비스 레이어에서만 사용
     * AuthService에서 OAuth 정보와 클라이언트 입력을 조합하여 생성
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "UserRegisterRequest")
    public static class RegisterRequest {
        private String kakaoId;
        private String name;
        private String phone;
        private UserType userType;
        private String profileImageUrl;

        @Size(max = 50, message = "은행명은 50자 이하로 입력해주세요.")
        @Schema(description = "은행명 (근로자 타입 필수)", example = "KB국민은행")
        private String bankName;
        

        @Size(max = 50, message = "계좌번호는 50자 이하로 입력해주세요.")
        @Schema(description = "계좌번호 (근로자 타입 필수)", example = "12345678901234")
        private String accountNumber;

        @AssertTrue(message = "근로자 타입은 은행명과 계좌번호가 필수입니다.")
        @JsonIgnore
        public boolean isValidBankInfoForWorker() {
            if (userType != UserType.WORKER) {
                return true;
            }
            boolean hasBankName = bankName != null && !bankName.isBlank();
            boolean hasAccountNumber = accountNumber != null && !accountNumber.isBlank();
            return hasBankName && hasAccountNumber;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "UserRegisterResponse")
    public static class RegisterResponse {
        private Long userId;
        private String name;
        private UserType userType;
        private String workerCode; // WORKER인 경우에만 값이 있음

        public static RegisterResponse from(User user, String workerCode) {
            return RegisterResponse.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .userType(user.getUserType())
                    .workerCode(workerCode)
                    .build();
        }
    }
}
