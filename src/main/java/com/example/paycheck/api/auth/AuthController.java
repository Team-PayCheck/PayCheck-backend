package com.example.paycheck.api.auth;

import com.example.paycheck.common.dto.ApiResponse;
import com.example.paycheck.domain.auth.dto.AuthDto;
import com.example.paycheck.domain.auth.service.AuthService;
import com.example.paycheck.domain.user.entity.User;
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

    @Operation(summary = "카카오 로그인", description = "카카오 액세스 토큰을 검증하고 자체 JWT를 발급합니다.")
    @PostMapping("/kakao/login")
    public ApiResponse<AuthDto.LoginResponse> kakaoLogin(
            @Valid @RequestBody AuthDto.KakaoLoginRequest request) {
        AuthService.LoginResult loginResult = authService.loginWithKakao(request.getKakaoAccessToken());

        // Refresh Token을 응답에 포함 (body 방식)
        AuthDto.LoginResponse response = AuthDto.LoginResponse.builder()
                .accessToken(loginResult.getLoginResponse().getAccessToken())
                .userId(loginResult.getLoginResponse().getUserId())
                .name(loginResult.getLoginResponse().getName())
                .userType(loginResult.getLoginResponse().getUserType())
                .refreshToken(loginResult.getRefreshToken())
                .build();

        return ApiResponse.success(response);
    }

    @Operation(summary = "카카오 회원가입", description = "카카오 프로필 정보를 기반으로 사용자를 등록하고 JWT를 발급합니다.")
    @PostMapping("/kakao/register")
    public ApiResponse<AuthDto.LoginResponse> kakaoRegister(
            @Valid @RequestBody AuthDto.KakaoRegisterRequest request) {
        AuthService.LoginResult loginResult = authService.registerWithKakao(request);

        // Refresh Token을 응답에 포함 (body 방식)
        AuthDto.LoginResponse response = AuthDto.LoginResponse.builder()
                .accessToken(loginResult.getLoginResponse().getAccessToken())
                .userId(loginResult.getLoginResponse().getUserId())
                .name(loginResult.getLoginResponse().getName())
                .userType(loginResult.getLoginResponse().getUserType())
                .refreshToken(loginResult.getRefreshToken())
                .build();

        return ApiResponse.success(response);
    }

    @Operation(summary = "로그아웃", description = "사용자를 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ApiResponse<AuthDto.LogoutResponse> logout(
            @AuthenticationPrincipal User user) {
        authService.logout(user != null ? user.getId() : null);

        return ApiResponse.success(AuthDto.LogoutResponse.success());
    }

    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 탈퇴 처리하고 관련 데이터를 정리합니다. " +
            "카카오 연결 해제는 서버에서 어드민 키로 자동 처리됩니다.")
    @DeleteMapping("/withdraw")
    public ApiResponse<AuthDto.WithdrawResponse> withdraw(
            @AuthenticationPrincipal User user) {

        authService.withdraw(user);

        return ApiResponse.success(AuthDto.WithdrawResponse.success());
    }

    @Operation(summary = "토큰 갱신", description = "Request Body의 Refresh Token을 사용하여 새로운 Access Token을 발급합니다. (Token Rotation 적용)")
    @PostMapping("/refresh")
    public ApiResponse<AuthDto.RefreshResponse> refresh(
            @Valid @RequestBody AuthDto.RefreshRequest request) {
        AuthService.RefreshResult refreshResult = authService.refreshAccessToken(request.getRefreshToken());

        return ApiResponse.success(refreshResult.getRefreshResponse());
    }

    @Operation(
            summary = "[개발용] 임시 로그인",
            description = "개발용 임시 로그인 API입니다. 실제 사용자 검증 없이 토큰을 발급합니다. " +
                    "배포 환경에서는 반드시 비활성화해야 합니다."
    )
    @PostMapping("/dev/login")
    public ApiResponse<AuthDto.LoginResponse> devLogin(
            @Valid @RequestBody AuthDto.DevLoginRequest request) {
        AuthService.LoginResult loginResult = authService.devLogin(request);

        // Refresh Token을 응답에 포함 (body 방식)
        AuthDto.LoginResponse response = AuthDto.LoginResponse.builder()
                .accessToken(loginResult.getLoginResponse().getAccessToken())
                .userId(loginResult.getLoginResponse().getUserId())
                .name(loginResult.getLoginResponse().getName())
                .userType(loginResult.getLoginResponse().getUserType())
                .refreshToken(loginResult.getRefreshToken())
                .build();

        return ApiResponse.success(response);
    }

}
