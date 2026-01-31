package com.example.paycheck.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorResponse error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorResponse.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, List<ErrorResponse.FieldErrorDetail> fieldErrors) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorResponse.builder()
                        .code(code)
                        .message(message)
                        .fieldErrors(fieldErrors)
                        .build())
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorResponse {
        private String code;
        private String message;
        private List<FieldErrorDetail> fieldErrors;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class FieldErrorDetail {
            private String field;
            private String message;
        }
    }
}
