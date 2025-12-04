package com.example.wagemanager.api.common;

import com.example.wagemanager.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "헬스체크", description = "서버 상태 확인 API")
@RestController
@RequestMapping("/api")
public class HealthCheckController {

    @Operation(summary = "서버 상태 확인", description = "API 서버가 정상적으로 동작하는지 확인합니다.")
    @GetMapping("/health")
    public ApiResponse<HealthCheckResponse> healthCheck() {
        return ApiResponse.success(
                HealthCheckResponse.builder()
                        .status("OK")
                        .message("WageManager API is running")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class HealthCheckResponse {
        private String status;
        private String message;
        private LocalDateTime timestamp;
    }
}
