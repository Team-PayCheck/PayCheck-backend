package com.example.wagemanager.api.user;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.user.dto.UserDto;
import com.example.wagemanager.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
}
