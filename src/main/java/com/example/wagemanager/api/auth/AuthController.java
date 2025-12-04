package com.example.wagemanager.api.auth;

import com.example.wagemanager.api.auth.dto.AuthDto;
import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.auth.service.AuthService;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.example.wagemanager.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "로그인, 회원가입, 로그아웃 등 인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Operation(summary = "카카오 로그인", description = "카카오 액세스 토큰을 검증하고 자체 JWT를 발급합니다.")
    @PostMapping("/kakao/login")
    public ApiResponse<AuthDto.LoginResponse> kakaoLogin(
            @Valid @RequestBody AuthDto.KakaoLoginRequest request
    ) {
        return ApiResponse.success(authService.loginWithKakao(request.getKakaoAccessToken()));
    }

    @Operation(summary = "카카오 회원가입", description = "카카오 프로필 정보를 기반으로 사용자를 등록하고 JWT를 발급합니다.")
    @PostMapping("/kakao/register")
    public ApiResponse<AuthDto.LoginResponse> kakaoRegister(
            @Valid @RequestBody AuthDto.KakaoRegisterRequest request
    ) {
        return ApiResponse.success(authService.registerWithKakao(request));
    }

    @Operation(summary = "로그아웃", description = "사용자를 로그아웃 처리합니다. (클라이언트에서 토큰 폐기 필요)")
    @PostMapping("/logout")
    public ApiResponse<AuthDto.LogoutResponse> logout(@AuthenticationPrincipal User user) {
        authService.logout(user != null ? user.getId() : null);
        return ApiResponse.success(AuthDto.LogoutResponse.success());
    }

}
