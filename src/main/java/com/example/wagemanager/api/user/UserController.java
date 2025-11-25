package com.example.wagemanager.api.user;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.user.dto.UserDto;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.user.service.UserService;
import com.example.wagemanager.domain.worker.dto.WorkerDto;
import com.example.wagemanager.domain.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WorkerService workerService;

    @GetMapping("/{userId}")
    public ApiResponse<UserDto.Response> getUserById(@PathVariable Long userId) {
        return ApiResponse.success(userService.getUserById(userId));
    }

    @GetMapping("/kakao/{kakaoId}")
    public ApiResponse<UserDto.Response> getUserByKakaoId(@PathVariable String kakaoId) {
        return ApiResponse.success(userService.getUserByKakaoId(kakaoId));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserDto.Response> updateUser(
            @PathVariable Long userId,
            @RequestBody UserDto.UpdateRequest request) {
        return ApiResponse.success(userService.updateUser(userId, request));
    }

    @PostMapping("/register")
    public ApiResponse<UserDto.RegisterResponse> register(
            @RequestBody UserDto.RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ApiResponse<UserDto.Response> getMyInfo(@AuthenticationPrincipal User user) {
        return ApiResponse.success(userService.getUserById(user.getId()));
    }

    // 내 정보 수정
    @PutMapping("/me")
    public ApiResponse<UserDto.Response> updateMyInfo(
            @AuthenticationPrincipal User user,
            @RequestBody UserDto.UpdateRequest request) {
        return ApiResponse.success(userService.updateUser(user.getId(), request));
    }

    // 계좌 정보 수정 (근로자 전용)
    @PutMapping("/me/account")
    public ApiResponse<WorkerDto.Response> updateMyAccount(
            @AuthenticationPrincipal User user,
            @RequestBody WorkerDto.UpdateRequest request) {
        // WORKER 타입 확인
        if (user.getUserType() != UserType.WORKER) {
            throw new IllegalArgumentException("근로자만 계좌 정보를 수정할 수 있습니다.");
        }
        return ApiResponse.success(workerService.updateWorkerByUserId(user.getId(), request));
    }
}
