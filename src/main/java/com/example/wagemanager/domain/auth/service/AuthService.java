package com.example.wagemanager.domain.auth.service;

import com.example.wagemanager.api.auth.dto.AuthDto;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.user.dto.UserDto;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.example.wagemanager.domain.user.service.UserService;
import com.example.wagemanager.global.oauth.kakao.KakaoOAuthClient;
import com.example.wagemanager.global.oauth.kakao.dto.KakaoUserInfo;
import com.example.wagemanager.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthDto.LoginResponse loginWithKakao(String kakaoAccessToken) {
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

        return AuthDto.LoginResponse.builder()
                .accessToken(accessToken)
                .userId(user.getId())
                .name(user.getName())
                .userType(user.getUserType().name())
                .build();
    }

    @Transactional(readOnly = true)
    public void logout(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
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
    public AuthDto.LoginResponse registerWithKakao(AuthDto.KakaoRegisterRequest request) {
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

        UserDto.RegisterRequest registerRequest = UserDto.RegisterRequest.builder()
                .kakaoId(userInfo.kakaoId())
                .name(resolveDisplayName(userInfo))
                .phone(normalizePhoneNumber(userInfo.phoneNumber()))
                .profileImageUrl(userInfo.profileImageUrl())
                .userType(parseUserType(request.getUserType()))
                .build();

        UserDto.RegisterResponse registerResponse = userService.register(registerRequest);

        String token = jwtTokenProvider.generateToken(registerResponse.getUserId());

        return AuthDto.LoginResponse.builder()
                .accessToken(token)
                .userId(registerResponse.getUserId())
                .name(registerResponse.getName())
                .userType(registerResponse.getUserType().name())
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
