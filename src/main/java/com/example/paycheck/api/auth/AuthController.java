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

    @Operation(
            summary = "카카오 로그인",
            description = "카카오 액세스 토큰을 검증하고 자체 JWT를 발급합니다. " +
                    "탈퇴 상태 계정인 경우 status=WITHDRAWN_PENDING으로 응답하여 " +
                    "클라이언트가 복구(/kakao/restore) 또는 완전 삭제 후 재가입(/kakao/purge-and-register)을 안내할 수 있습니다."
    )
    @PostMapping("/kakao/login")
    public ApiResponse<AuthDto.KakaoLoginResult> kakaoLogin(
            @Valid @RequestBody AuthDto.KakaoLoginRequest request) {
        return ApiResponse.success(authService.loginWithKakao(request.getKakaoAccessToken()));
    }

    @Operation(
            summary = "탈퇴 계정 복구",
            description = "30일 hard-delete 전 탈퇴 계정을 다시 활성화합니다. " +
                    "User.deletedAt이 null로 되돌아가며, 탈퇴 시 보존된 UserSettings는 그대로 유지됩니다. " +
                    "비활성화된 사업장/계약/근무기록은 자동 복구되지 않습니다."
    )
    @PostMapping("/kakao/restore")
    public ApiResponse<AuthDto.LoginResponse> kakaoRestore(
            @Valid @RequestBody AuthDto.KakaoLoginRequest request) {
        return ApiResponse.success(authService.restoreWithKakao(request.getKakaoAccessToken()));
    }

    @Operation(
            summary = "탈퇴 계정 완전 삭제 후 재가입",
            description = "기존 탈퇴 계정과 모든 산하 데이터를 영구 삭제(30일 스케줄러와 동일 경로)한 뒤 " +
                    "신규 사용자로 회원가입합니다. 정상 계정에 호출 시 차단됩니다."
    )
    @PostMapping("/kakao/purge-and-register")
    public ApiResponse<AuthDto.LoginResponse> kakaoPurgeAndRegister(
            @Valid @RequestBody AuthDto.KakaoRegisterRequest request) {
        return ApiResponse.success(authService.purgeAndRegisterWithKakao(request));
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
