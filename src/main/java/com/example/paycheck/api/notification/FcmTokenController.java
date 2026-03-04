package com.example.paycheck.api.notification;

import com.example.paycheck.common.dto.ApiResponse;
import com.example.paycheck.domain.fcm.dto.FcmTokenDto;
import com.example.paycheck.domain.fcm.service.FcmTokenService;
import com.example.paycheck.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FCM 토큰", description = "FCM 디바이스 토큰 관리 API")
@RestController
@RequestMapping("/api/notifications/fcm-token")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @Operation(summary = "FCM 토큰 등록", description = "디바이스의 FCM 토큰을 등록합니다. 앱 시작 시 호출합니다.")
    @PostMapping
    public ApiResponse<Void> registerToken(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FcmTokenDto.RegisterRequest request) {
        fcmTokenService.registerToken(user.getId(), request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "FCM 토큰 삭제", description = "디바이스의 FCM 토큰을 삭제합니다. 로그아웃 시 호출합니다.")
    @DeleteMapping
    public ApiResponse<Void> deleteToken(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FcmTokenDto.DeleteRequest request) {
        fcmTokenService.deleteToken(request.getToken());
        return ApiResponse.success(null);
    }
}
