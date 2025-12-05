package com.example.wagemanager.api.auth;

import com.example.wagemanager.api.auth.dto.AuthDto;
import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.auth.service.AuthService;
import com.example.wagemanager.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "로그인, 회원가입, 로그아웃 등 인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationTime; // 밀리초 단위

    @Operation(summary = "카카오 로그인", description = "카카오 액세스 토큰을 검증하고 자체 JWT를 발급합니다.")
    @PostMapping("/kakao/login")
    public ApiResponse<AuthDto.LoginResponse> kakaoLogin(
            @Valid @RequestBody AuthDto.KakaoLoginRequest request,
            HttpServletResponse response) {
        AuthService.LoginResult loginResult = authService.loginWithKakao(request.getKakaoAccessToken());

        // Refresh Token을 HttpOnly Cookie로 설정
        setRefreshTokenCookie(response, loginResult.getRefreshToken());

        return ApiResponse.success(loginResult.getLoginResponse());
    }

    @Operation(summary = "카카오 회원가입", description = "카카오 프로필 정보를 기반으로 사용자를 등록하고 JWT를 발급합니다.")
    @PostMapping("/kakao/register")
    public ApiResponse<AuthDto.LoginResponse> kakaoRegister(
            @Valid @RequestBody AuthDto.KakaoRegisterRequest request,
            HttpServletResponse response) {
        AuthService.LoginResult loginResult = authService.registerWithKakao(request);

        // Refresh Token을 HttpOnly Cookie로 설정
        setRefreshTokenCookie(response, loginResult.getRefreshToken());

        return ApiResponse.success(loginResult.getLoginResponse());
    }

    @Operation(summary = "로그아웃", description = "사용자를 로그아웃 처리하고 Refresh Token 쿠키를 삭제합니다.")
    @PostMapping("/logout")
    public ApiResponse<AuthDto.LogoutResponse> logout(
            @AuthenticationPrincipal User user,
            HttpServletResponse response) {
        authService.logout(user != null ? user.getId() : null);

        // Refresh Token 쿠키 삭제
        deleteRefreshTokenCookie(response);

        return ApiResponse.success(AuthDto.LogoutResponse.success());
    }

    @Operation(summary = "토큰 갱신", description = "Cookie의 Refresh Token을 사용하여 새로운 Access Token을 발급합니다.")
    @PostMapping("/refresh")
    public ApiResponse<AuthDto.RefreshResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        // Cookie에서 Refresh Token 추출
        String refreshToken = getRefreshTokenFromCookie(request);

        AuthService.RefreshResult refreshResult = authService.refreshAccessToken(refreshToken);

        // 새로운 Refresh Token을 Cookie로 설정
        setRefreshTokenCookie(response, refreshResult.getRefreshToken());

        return ApiResponse.success(refreshResult.getRefreshResponse());
    }

    /**
     * Refresh Token을 HttpOnly Cookie로 설정
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // HTTPS에서만 전송
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshExpirationTime / 1000)); // 밀리초를 초로 변환
        cookie.setAttribute("SameSite", "Strict"); // CSRF 방어
        response.addCookie(cookie);
    }

    /**
     * Refresh Token Cookie 삭제
     */
    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Cookie에서 Refresh Token 추출
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new IllegalArgumentException("Refresh Token이 없습니다. 다시 로그인해주세요.");
    }

}
