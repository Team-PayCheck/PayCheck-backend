package com.example.wagemanager.domain.user.dto;

import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
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
        private String name;
        private String phone;
        private String profileImageUrl;
    }

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
