package com.example.wagemanager.domain.auth.service;

import com.example.wagemanager.api.auth.dto.AuthDto;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.auth.entity.RefreshToken;
import com.example.wagemanager.domain.auth.repository.RefreshTokenRepository;
import com.example.wagemanager.domain.user.dto.UserDto;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.example.wagemanager.domain.user.service.UserService;
import com.example.wagemanager.global.oauth.kakao.KakaoOAuthClient;
import com.example.wagemanager.global.oauth.kakao.dto.KakaoUserInfo;
import com.example.wagemanager.global.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 로그인 결과 (응답 DTO + Refresh Token)
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class LoginResult {
        private AuthDto.LoginResponse loginResponse;
        private String refreshToken;
    }

    /**
     * 토큰 갱신 결과 (응답 DTO + Refresh Token)
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class RefreshResult {
        private AuthDto.RefreshResponse refreshResponse;
        private String refreshToken;
    }

    @Transactional
    public LoginResult loginWithKakao(String kakaoAccessToken) {
        if (!StringUtils.hasText(kakaoAccessToken)) {
            throw new IllegalArgumentException("카카오 액세스 토큰은 필수입니다.");
        }

        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(kakaoAccessToken);
        if (!StringUtils.hasText(userInfo.kakaoId())) {
            throw new IllegalArgumentException("카카오 사용자 식별자를 확인할 수 없습니다.");
        }

        User user = userRepository.findByKakaoId(userInfo.kakaoId())
                .orElseThrow(() -> new NotFoundException(
                        "USER_NOT_FOUND",
                        "등록되지 않은 카카오 계정입니다. 회원가입을 진행해주세요."
                ));

        updateProfileIfEmpty(user, userInfo);

        String accessToken = jwtTokenProvider.generateToken(user.getId());
        String refreshToken = createOrUpdateRefreshToken(user.getId());

        AuthDto.LoginResponse loginResponse = AuthDto.LoginResponse.builder()
                .accessToken(accessToken)
                .userId(user.getId())
                .name(user.getName())
                .userType(user.getUserType().name())
                .build();

        return LoginResult.builder()
                .loginResponse(loginResponse)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        // Refresh Token 삭제
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token과 Refresh Token 발급
     */
    @Transactional
    public RefreshResult refreshAccessToken(String refreshTokenString) {
        if (!StringUtils.hasText(refreshTokenString)) {
            throw new IllegalArgumentException("Refresh Token은 필수입니다.");
        }

        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshTokenString)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // DB에서 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new NotFoundException("REFRESH_TOKEN_NOT_FOUND", "Refresh Token을 찾을 수 없습니다."));

        // 만료 확인
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("만료된 Refresh Token입니다. 다시 로그인해주세요.");
        }

        // 사용자 확인
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        // 새로운 Access Token 및 Refresh Token 생성
        String newAccessToken = jwtTokenProvider.generateToken(user.getId());
        String newRefreshToken = createOrUpdateRefreshToken(user.getId());

        AuthDto.RefreshResponse refreshResponse = AuthDto.RefreshResponse.builder()
                .accessToken(newAccessToken)
                .build();

        return RefreshResult.builder()
                .refreshResponse(refreshResponse)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * Refresh Token 생성 또는 업데이트
     */
    private String createOrUpdateRefreshToken(Long userId) {
        String refreshTokenString = jwtTokenProvider.generateRefreshToken(userId);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshExpirationTime() / 1000);

        // 기존 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
                .orElse(null);

        if (refreshToken == null) {
            // 새로 생성
            refreshToken = RefreshToken.builder()
                    .userId(userId)
                    .token(refreshTokenString)
                    .expiresAt(expiresAt)
                    .build();
        } else {
            // 기존 토큰 업데이트
            refreshToken.updateToken(refreshTokenString, expiresAt);
        }

        refreshTokenRepository.save(refreshToken);
        return refreshTokenString;
    }

    private void updateProfileIfEmpty(User user, KakaoUserInfo kakaoUserInfo) {
        String name = user.getName() == null ? kakaoUserInfo.displayName() : null;
        String phone = user.getPhone() == null ? normalizePhoneNumber(kakaoUserInfo.phoneNumber()) : null;
        String profileImage = user.getProfileImageUrl() == null ? kakaoUserInfo.profileImageUrl() : null;

        if (name != null || phone != null || profileImage != null) {
            user.updateProfile(name, phone, profileImage);
        }
    }

    @Transactional
    public LoginResult registerWithKakao(AuthDto.KakaoRegisterRequest request) {
        if (!StringUtils.hasText(request.getKakaoAccessToken())) {
            throw new IllegalArgumentException("카카오 액세스 토큰은 필수입니다.");
        }

        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(request.getKakaoAccessToken());
        if (!StringUtils.hasText(userInfo.kakaoId())) {
            throw new IllegalArgumentException("카카오 사용자 식별자를 확인할 수 없습니다.");
        }

        if (userRepository.findByKakaoId(userInfo.kakaoId()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 카카오 계정입니다.");
        }

        UserType userType = parseUserType(request.getUserType());

        // WORKER 타입인 경우 카카오페이 링크 필수 검증
        if (userType == UserType.WORKER && !StringUtils.hasText(request.getKakaoPayLink())) {
            throw new IllegalArgumentException("근로자 타입은 카카오페이 링크가 필수입니다.");
        }

        UserDto.RegisterRequest registerRequest = UserDto.RegisterRequest.builder()
                .kakaoId(userInfo.kakaoId())
                .name(resolveDisplayName(userInfo))
                .phone(request.getPhone())
                .profileImageUrl(userInfo.profileImageUrl())
                .userType(userType)
                .kakaoPayLink(request.getKakaoPayLink())
                .build();

        UserDto.RegisterResponse registerResponse = userService.register(registerRequest);

        String accessToken = jwtTokenProvider.generateToken(registerResponse.getUserId());
        String refreshToken = createOrUpdateRefreshToken(registerResponse.getUserId());

        AuthDto.LoginResponse loginResponse = AuthDto.LoginResponse.builder()
                .accessToken(accessToken)
                .userId(registerResponse.getUserId())
                .name(registerResponse.getName())
                .userType(registerResponse.getUserType().name())
                .build();

        return LoginResult.builder()
                .loginResponse(loginResponse)
                .refreshToken(refreshToken)
                .build();
    }

    private String resolveDisplayName(KakaoUserInfo userInfo) {
        String displayName = userInfo.displayName();
        if (StringUtils.hasText(displayName)) {
            return displayName;
        }
        throw new IllegalArgumentException("카카오 계정의 이름 정보를 확인할 수 없습니다.");
    }

    private UserType parseUserType(String userType) {
        try {
            return UserType.valueOf(userType.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 유형입니다. EMPLOYER 또는 WORKER를 입력해주세요.");
        }
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return null;
        }

        String normalized = phoneNumber.trim();
        if (normalized.startsWith("+82")) {
            normalized = normalized.replaceFirst("\\+82", "0");
        }

        normalized = normalized.replaceAll("[^0-9]", "");
        return normalized.isEmpty() ? null : normalized;
    }
}
