package com.example.wagemanager.api.auth;

import com.example.wagemanager.api.auth.dto.AuthDto;
import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.example.wagemanager.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * 개발용 임시 로그인 API
     * userId로 로그인하여 JWT 토큰 발급
     * TODO: 추후 카카오 OAuth 로그인으로 대체 예정
     */
    @PostMapping("/dev/login")
    public ApiResponse<AuthDto.LoginResponse> devLogin(@RequestBody AuthDto.DevLoginRequest request) {
        // 사용자 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. userId: " + request.getUserId()));

        // JWT 토큰 생성
        String token = jwtTokenProvider.generateToken(user.getId());

        // 응답 DTO 생성
        AuthDto.LoginResponse response = AuthDto.LoginResponse.builder()
                .accessToken(token)
                .userId(user.getId())
                .name(user.getName())
                .userType(user.getUserType().name())
                .build();

        return ApiResponse.success(response);
    }
}
