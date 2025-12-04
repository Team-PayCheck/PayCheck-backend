package com.example.wagemanager.api.user;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.user.dto.UserDto;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.user.service.UserService;
import com.example.wagemanager.domain.worker.dto.WorkerDto;
import com.example.wagemanager.domain.worker.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 정보 조회 및 관리 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WorkerService workerService;

    @Operation(summary = "사용자 조회 (ID)", description = "사용자 ID로 사용자 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ApiResponse<UserDto.Response> getUserById(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId) {
        return ApiResponse.success(userService.getUserById(userId));
    }

    @Operation(summary = "사용자 조회 (카카오 ID)", description = "카카오 ID로 사용자 정보를 조회합니다.")
    @GetMapping("/kakao/{kakaoId}")
    public ApiResponse<UserDto.Response> getUserByKakaoId(
            @Parameter(description = "카카오 ID", required = true) @PathVariable String kakaoId) {
        return ApiResponse.success(userService.getUserByKakaoId(kakaoId));
    }

    @Operation(summary = "사용자 정보 수정", description = "특정 사용자의 정보를 수정합니다.")
    @PutMapping("/{userId}")
    public ApiResponse<UserDto.Response> updateUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @RequestBody UserDto.UpdateRequest request) {
        return ApiResponse.success(userService.updateUser(userId, request));
    }

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @PostMapping("/register")
    public ApiResponse<UserDto.RegisterResponse> register(
            @RequestBody UserDto.RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자 본인의 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserDto.Response> getMyInfo(@AuthenticationPrincipal User user) {
        return ApiResponse.success(userService.getUserById(user.getId()));
    }

    @Operation(summary = "내 정보 수정", description = "로그인한 사용자 본인의 정보를 수정합니다.")
    @PutMapping("/me")
    public ApiResponse<UserDto.Response> updateMyInfo(
            @AuthenticationPrincipal User user,
            @RequestBody UserDto.UpdateRequest request) {
        return ApiResponse.success(userService.updateUser(user.getId(), request));
    }

    @Operation(summary = "계좌 정보 수정 (근로자 전용)", description = "로그인한 근로자의 계좌 정보를 수정합니다.")
    @PutMapping("/me/account")
    public ApiResponse<WorkerDto.Response> updateMyAccount(
            @AuthenticationPrincipal User user,
            @RequestBody WorkerDto.UpdateRequest request) {
        if (user.getUserType() != UserType.WORKER) {
            throw new IllegalArgumentException("근로자만 계좌 정보를 수정할 수 있습니다.");
        }
        return ApiResponse.success(workerService.updateWorkerByUserId(user.getId(), request));
    }
}
