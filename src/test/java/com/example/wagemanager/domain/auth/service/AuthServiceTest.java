package com.example.wagemanager.domain.auth.service;

import com.example.wagemanager.common.exception.BadRequestException;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.auth.dto.AuthDto;
import com.example.wagemanager.domain.user.dto.UserDto;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.example.wagemanager.domain.user.service.UserService;
import com.example.wagemanager.domain.employer.repository.EmployerRepository;
import com.example.wagemanager.domain.worker.repository.WorkerRepository;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.employer.entity.Employer;
import com.example.wagemanager.global.oauth.kakao.dto.KakaoUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private OAuthService oAuthService;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private EmployerRepository employerRepository;

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private KakaoUserInfo kakaoUserInfo;
    private TokenService.TokenPair tokenPair;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .kakaoId("test_kakao_id")
                .name("테스트 사용자")
                .phone("010-1234-5678")
                .userType(UserType.WORKER)
                .profileImageUrl("https://example.com/profile.jpg")
                .build();

        kakaoUserInfo = new KakaoUserInfo(
                "test_kakao_id",
                "카카오 닉네임"
        );

        tokenPair = TokenService.TokenPair.builder()
                .accessToken("test_access_token")
                .refreshToken("test_refresh_token")
                .build();
    }

    @Test
    @DisplayName("카카오 로그인 성공")
    void loginWithKakao_Success() {
        // given
        String kakaoAccessToken = "kakao_access_token";
        when(oAuthService.getKakaoUserInfo(kakaoAccessToken)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.of(testUser));
        when(tokenService.generateTokenPair(testUser.getId())).thenReturn(tokenPair);

        // when
        AuthService.LoginResult result = authService.loginWithKakao(kakaoAccessToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginResponse().getAccessToken()).isEqualTo("test_access_token");
        assertThat(result.getRefreshToken()).isEqualTo("test_refresh_token");
        assertThat(result.getLoginResponse().getUserId()).isEqualTo(1L);
        assertThat(result.getLoginResponse().getName()).isEqualTo("테스트 사용자");
        assertThat(result.getLoginResponse().getUserType()).isEqualTo("WORKER");

        verify(oAuthService).getKakaoUserInfo(kakaoAccessToken);
        verify(userRepository).findByKakaoId(kakaoUserInfo.kakaoId());
        verify(tokenService).generateTokenPair(testUser.getId());
    }

    @Test
    @DisplayName("카카오 로그인 실패 - 등록되지 않은 사용자")
    void loginWithKakao_UserNotFound() {
        // given
        String kakaoAccessToken = "kakao_access_token";
        when(oAuthService.getKakaoUserInfo(kakaoAccessToken)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.loginWithKakao(kakaoAccessToken))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("등록되지 않은 카카오 계정입니다");

        verify(oAuthService).getKakaoUserInfo(kakaoAccessToken);
        verify(userRepository).findByKakaoId(kakaoUserInfo.kakaoId());
        verify(tokenService, never()).generateTokenPair(anyLong());
    }

    @Test
    @DisplayName("카카오 회원가입 성공 - WORKER")
    void registerWithKakao_Success_Worker() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .phone("010-1234-5678")
                .userType("WORKER")
                .profileImageUrl("https://example.com/profile.jpg")
                .kakaoPayLink("https://qr.kakaopay.com/test")
                .build();

        UserDto.RegisterResponse registerResponse = UserDto.RegisterResponse.builder()
                .userId(1L)
                .name("카카오 닉네임")
                .userType(UserType.WORKER)
                .workerCode("WORKER001")
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.empty());
        when(oAuthService.resolveDisplayName(kakaoUserInfo)).thenReturn("카카오 닉네임");
        when(userService.register(any(UserDto.RegisterRequest.class))).thenReturn(registerResponse);
        when(tokenService.generateTokenPair(1L)).thenReturn(tokenPair);

        // when
        AuthService.LoginResult result = authService.registerWithKakao(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginResponse().getAccessToken()).isEqualTo("test_access_token");
        assertThat(result.getRefreshToken()).isEqualTo("test_refresh_token");
        assertThat(result.getLoginResponse().getUserId()).isEqualTo(1L);
        assertThat(result.getLoginResponse().getUserType()).isEqualTo("WORKER");

        verify(oAuthService).getKakaoUserInfo(request.getKakaoAccessToken());
        verify(userRepository).findByKakaoId(kakaoUserInfo.kakaoId());
        verify(userService).register(any(UserDto.RegisterRequest.class));
        verify(tokenService).generateTokenPair(1L);
    }

    @Test
    @DisplayName("카카오 회원가입 실패 - 이미 가입된 사용자")
    void registerWithKakao_DuplicateUser() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .phone("010-1234-5678")
                .userType("WORKER")
                .kakaoPayLink("https://qr.kakaopay.com/test")
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> authService.registerWithKakao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이미 가입된 카카오 계정입니다");

        verify(userService, never()).register(any());
        verify(tokenService, never()).generateTokenPair(anyLong());
    }

    @Test
    @DisplayName("카카오 회원가입 실패 - WORKER 타입인데 카카오페이 링크 없음")
    void registerWithKakao_WorkerWithoutKakaoPayLink() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .phone("010-1234-5678")
                .userType("WORKER")
                .kakaoPayLink(null) // 카카오페이 링크 없음
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.registerWithKakao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("근로자 타입은 카카오페이 링크가 필수입니다");

        verify(userService, never()).register(any());
    }

    @Test
    @DisplayName("카카오 회원가입 성공 - EMPLOYER")
    void registerWithKakao_Success_Employer() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .phone("010-9876-5432")
                .userType("EMPLOYER")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();

        UserDto.RegisterResponse registerResponse = UserDto.RegisterResponse.builder()
                .userId(2L)
                .name("카카오 닉네임")
                .userType(UserType.EMPLOYER)
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.empty());
        when(oAuthService.resolveDisplayName(kakaoUserInfo)).thenReturn("카카오 닉네임");
        when(userService.register(any(UserDto.RegisterRequest.class))).thenReturn(registerResponse);
        when(tokenService.generateTokenPair(2L)).thenReturn(tokenPair);

        // when
        AuthService.LoginResult result = authService.registerWithKakao(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginResponse().getUserType()).isEqualTo("EMPLOYER");

        verify(userService).register(any(UserDto.RegisterRequest.class));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given
        Long userId = 1L;
        doNothing().when(tokenService).revokeRefreshToken(userId);

        // when
        authService.logout(userId);

        // then
        verify(tokenService).revokeRefreshToken(userId);
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshAccessToken_Success() {
        // given
        String refreshTokenString = "old_refresh_token";
        when(tokenService.refreshAccessToken(refreshTokenString)).thenReturn(tokenPair);

        // when
        AuthService.RefreshResult result = authService.refreshAccessToken(refreshTokenString);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRefreshResponse().getAccessToken()).isEqualTo("test_access_token");
        assertThat(result.getRefreshToken()).isEqualTo("test_refresh_token");

        verify(tokenService).refreshAccessToken(refreshTokenString);
    }

    @Test
    @DisplayName("개발용 로그인 성공 - 기존 사용자")
    void devLogin_ExistingUser() {
        // given
        AuthDto.DevLoginRequest request = AuthDto.DevLoginRequest.builder()
                .userId("1")
                .name("테스트 사용자")
                .userType("WORKER")
                .build();

        when(userRepository.findByKakaoId("dev_1")).thenReturn(Optional.of(testUser));
        when(workerRepository.findByUserId(1L)).thenReturn(Optional.of(mock(Worker.class)));
        when(tokenService.generateTokenPair(1L)).thenReturn(tokenPair);

        // when
        AuthService.LoginResult result = authService.devLogin(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginResponse().getUserId()).isEqualTo(1L);
        assertThat(result.getLoginResponse().getName()).isEqualTo("테스트 사용자");

        verify(userRepository).findByKakaoId("dev_1");
        verify(tokenService).generateTokenPair(1L);
    }

    @Test
    @DisplayName("개발용 로그인 성공 - 신규 사용자 자동 생성")
    void devLogin_NewUser() {
        // given
        AuthDto.DevLoginRequest request = AuthDto.DevLoginRequest.builder()
                .userId("999")
                .name("새로운 사용자")
                .userType("EMPLOYER")
                .build();

        User newUser = User.builder()
                .id(999L)
                .kakaoId("dev_999")
                .name("새로운 사용자")
                .userType(UserType.EMPLOYER)
                .build();

        when(userRepository.findByKakaoId("dev_999")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(employerRepository.save(any(Employer.class)))
                .thenReturn(mock(Employer.class));
        when(tokenService.generateTokenPair(999L)).thenReturn(tokenPair);

        // when
        AuthService.LoginResult result = authService.devLogin(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginResponse().getUserId()).isEqualTo(999L);

        verify(userRepository).findByKakaoId("dev_999");
        verify(userRepository).save(any(User.class));
        verify(tokenService).generateTokenPair(999L);
    }

    @Test
    @DisplayName("회원가입 실패 - 잘못된 UserType")
    void registerWithKakao_InvalidUserType() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .phone("010-1234-5678")
                .userType("INVALID_TYPE")
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.registerWithKakao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("유효하지 않은 사용자 유형입니다");
    }
}
