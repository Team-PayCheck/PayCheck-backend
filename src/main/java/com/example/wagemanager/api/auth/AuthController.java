package com.example.wagemanager.api.auth;

import com.example.wagemanager.api.auth.dto.AuthDto;
import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.auth.service.AuthService;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.example.wagemanager.global.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * 카카오 소셜 로그인 API
     * 프론트엔드에서 전달한 카카오 액세스 토큰을 검증하고 자체 JWT 발급
     */
    @PostMapping("/kakao/login")
    public ApiResponse<AuthDto.LoginResponse> kakaoLogin(
            @Valid @RequestBody AuthDto.KakaoLoginRequest request
    ) {
        return ApiResponse.success(authService.loginWithKakao(request.getKakaoAccessToken()));
    }

    /**
     * 카카오 소셜 회원가입 API
     * 카카오 프로필 정보를 기반으로 내부 사용자 데이터 생성 후 JWT 발급
     */
    @PostMapping("/kakao/register")
    public ApiResponse<AuthDto.LoginResponse> kakaoRegister(
            @Valid @RequestBody AuthDto.KakaoRegisterRequest request
    ) {
        return ApiResponse.success(authService.registerWithKakao(request));
    }

    /**
     * JWT 기반 로그아웃 API
     * 현재는 클라이언트가 토큰을 폐기하도록 안내
     */
    @PostMapping("/logout")
    public ApiResponse<AuthDto.LogoutResponse> logout(@AuthenticationPrincipal User user) {
        authService.logout(user != null ? user.getId() : null);
        return ApiResponse.success(AuthDto.LogoutResponse.success());
    }

}
