package com.example.paycheck.api.user;

import com.example.paycheck.common.dto.ApiResponse;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.UnauthorizedException;
import com.example.paycheck.domain.user.dto.UserDto;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.user.service.UserService;
import com.example.paycheck.domain.worker.dto.WorkerDto;
import com.example.paycheck.domain.worker.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 정보 조회 및 관리 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WorkerService workerService;

    @Operation(summary = "사용자 정보 수정", description = "특정 사용자의 정보를 수정합니다.")
    @PreAuthorize("@permissionEvaluator.canAccessUser(#userId)")
    @PutMapping("/{userId}")
    public ApiResponse<UserDto.Response> updateUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return ApiResponse.success(userService.updateUser(userId, request));
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
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return ApiResponse.success(userService.updateUser(user.getId(), request));
    }

    @Operation(summary = "계좌 정보 수정 (근로자 전용)", description = "로그인한 근로자의 계좌 정보를 수정합니다.")
    @PutMapping("/me/account")
    public ApiResponse<WorkerDto.Response> updateMyAccount(
            @AuthenticationPrincipal User user,
            @RequestBody WorkerDto.UpdateRequest request) {
        if (user.getUserType() != UserType.WORKER) {
            throw new UnauthorizedException(ErrorCode.WORKER_ONLY, "근로자만 계좌 정보를 수정할 수 있습니다.");
        }
        return ApiResponse.success(workerService.updateWorkerByUserId(user.getId(), request));
    }
}
