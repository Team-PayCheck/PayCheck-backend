package com.example.paycheck.domain.auth.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.auth.dto.AuthDto;
import com.example.paycheck.domain.user.dto.UserDto;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.user.repository.UserRepository;
import com.example.paycheck.domain.user.service.UserHardDeleteService;
import com.example.paycheck.domain.user.service.UserService;
import com.example.paycheck.domain.user.service.UserWithdrawService;
import com.example.paycheck.domain.employer.repository.EmployerRepository;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.global.oauth.kakao.dto.KakaoUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
    private UserWithdrawService userWithdrawService;

    @Mock
    private UserHardDeleteService userHardDeleteService;

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
                "카카오 닉네임",
                "https://kakao.com/profile.jpg"
        );

        tokenPair = TokenService.TokenPair.builder()
                .accessToken("test_access_token")
                .refreshToken("test_refresh_token")
                .build();
    }

    @Test
    @DisplayName("카카오 로그인 성공 - status=LOGGED_IN, 응답 평탄 구조 (클라이언트 호환)")
    void loginWithKakao_Success() {
        // given
        String kakaoAccessToken = "kakao_access_token";
        when(oAuthService.getKakaoUserInfo(kakaoAccessToken)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.of(testUser));
        when(tokenService.generateTokenPair(testUser.getId())).thenReturn(tokenPair);

        // when
        AuthDto.KakaoLoginResult result = authService.loginWithKakao(kakaoAccessToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("LOGGED_IN");
        assertThat(result.getWithdrawnAccount()).isNull();
        // 평탄 구조: top-level 필드에서 직접 접근 가능 (기존 클라이언트 호환)
        assertThat(result.getAccessToken()).isEqualTo("test_access_token");
        assertThat(result.getRefreshToken()).isEqualTo("test_refresh_token");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("테스트 사용자");
        assertThat(result.getUserType()).isEqualTo("WORKER");

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
                .name("홍길동")
                .phone("010-1234-5678")
                .userType("WORKER")
                .profileImageUrl("https://example.com/profile.jpg")
                .bankName("카카오뱅크")
                .accountNumber("3333123456789")
                .build();

        UserDto.RegisterResponse registerResponse = UserDto.RegisterResponse.builder()
                .userId(1L)
                .name("카카오 닉네임")
                .userType(UserType.WORKER)
                .workerCode("WORKER001")
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.empty());
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
        verify(userService).register(argThat(registerRequest ->
                "https://example.com/profile.jpg".equals(registerRequest.getProfileImageUrl())));
        verify(tokenService).generateTokenPair(1L);
    }

    @Test
    @DisplayName("카카오 회원가입 시 프로필 이미지 미입력이면 카카오 이미지 사용")
    void registerWithKakao_UsesKakaoProfileImageAsDefault() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("홍길동")
                .phone("010-1234-5678")
                .userType("WORKER")
                .bankName("카카오뱅크")
                .accountNumber("3333123456789")
                .build();

        UserDto.RegisterResponse registerResponse = UserDto.RegisterResponse.builder()
                .userId(1L)
                .name("홍길동")
                .userType(UserType.WORKER)
                .workerCode("WORKER001")
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.empty());
        when(userService.register(any(UserDto.RegisterRequest.class))).thenReturn(registerResponse);
        when(tokenService.generateTokenPair(1L)).thenReturn(tokenPair);

        // when
        authService.registerWithKakao(request);

        // then
        verify(userService).register(argThat(registerRequest ->
                "https://kakao.com/profile.jpg".equals(registerRequest.getProfileImageUrl())));
    }

    @Test
    @DisplayName("카카오 회원가입 시 잘못된 프로필 이미지 문자열이면 카카오 이미지 사용")
    void registerWithKakao_InvalidProfileImageString_UsesKakaoImage() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("홍길동")
                .phone("010-1234-5678")
                .userType("WORKER")
                .profileImageUrl("string")
                .bankName("카카오뱅크")
                .accountNumber("3333123456789")
                .build();

        UserDto.RegisterResponse registerResponse = UserDto.RegisterResponse.builder()
                .userId(1L)
                .name("홍길동")
                .userType(UserType.WORKER)
                .workerCode("WORKER001")
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.empty());
        when(userService.register(any(UserDto.RegisterRequest.class))).thenReturn(registerResponse);
        when(tokenService.generateTokenPair(1L)).thenReturn(tokenPair);

        // when
        authService.registerWithKakao(request);

        // then
        verify(userService).register(argThat(registerRequest ->
                "https://kakao.com/profile.jpg".equals(registerRequest.getProfileImageUrl())));
    }

    @Test
    @DisplayName("카카오 회원가입 시 사용자 URL도 카카오 이미지도 없으면 placeholder 반환")
    void registerWithKakao_NoBothImages_UsesPlaceholder() {
        // given
        KakaoUserInfo kakaoUserInfoWithoutImage = new KakaoUserInfo(
                "test_kakao_id",
                "카카오 닉네임",
                null // 카카오 프로필 이미지 없음
        );

        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("홍길동")
                .phone("010-1234-5678")
                .userType("WORKER")
                // profileImageUrl 미입력
                .bankName("카카오뱅크")
                .accountNumber("3333123456789")
                .build();

        UserDto.RegisterResponse registerResponse = UserDto.RegisterResponse.builder()
                .userId(1L)
                .name("홍길동")
                .userType(UserType.WORKER)
                .workerCode("WORKER001")
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfoWithoutImage);
        when(userRepository.findByKakaoId(kakaoUserInfoWithoutImage.kakaoId())).thenReturn(Optional.empty());
        when(userService.register(any(UserDto.RegisterRequest.class))).thenReturn(registerResponse);
        when(tokenService.generateTokenPair(1L)).thenReturn(tokenPair);

        // when
        authService.registerWithKakao(request);

        // then
        verify(userService).register(argThat(registerRequest ->
                "https://via.placeholder.com/150/CCCCCC/FFFFFF?text=User".equals(registerRequest.getProfileImageUrl())));
    }

    @Test
    @DisplayName("카카오 회원가입 시 placeholder URL을 직접 전송하면 카카오 이미지로 대체")
    void registerWithKakao_PlaceholderUrlSentExplicitly_UsesKakaoImage() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("홍길동")
                .phone("010-1234-5678")
                .userType("WORKER")
                .profileImageUrl("https://via.placeholder.com/150/CCCCCC/FFFFFF?text=User") // placeholder URL 명시 전송
                .bankName("카카오뱅크")
                .accountNumber("3333123456789")
                .build();

        UserDto.RegisterResponse registerResponse = UserDto.RegisterResponse.builder()
                .userId(1L)
                .name("홍길동")
                .userType(UserType.WORKER)
                .workerCode("WORKER001")
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.empty());
        when(userService.register(any(UserDto.RegisterRequest.class))).thenReturn(registerResponse);
        when(tokenService.generateTokenPair(1L)).thenReturn(tokenPair);

        // when
        authService.registerWithKakao(request);

        // then: placeholder URL은 "이미지 없음"으로 간주하여 카카오 이미지로 대체
        verify(userService).register(argThat(registerRequest ->
                "https://kakao.com/profile.jpg".equals(registerRequest.getProfileImageUrl())));
    }

    @Test
    @DisplayName("카카오 회원가입 실패 - 이미 가입된 사용자")
    void registerWithKakao_DuplicateUser() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("홍길동")
                .phone("010-1234-5678")
                .userType("WORKER")
                .bankName("카카오뱅크")
                .accountNumber("3333123456789")
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
    @DisplayName("카카오 회원가입 실패 - WORKER 타입인데 은행 정보 없음")
    void registerWithKakao_WorkerWithoutBankInfo() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("홍길동")
                .phone("010-1234-5678")
                .userType("WORKER")
                .bankName(null) // 은행 정보 없음
                .accountNumber(null)
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.registerWithKakao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("근로자 타입은 은행명과 계좌번호가 필수입니다");

        verify(userService, never()).register(any());
    }

    @Test
    @DisplayName("카카오 회원가입 성공 - EMPLOYER")
    void registerWithKakao_Success_Employer() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("고용주")
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
    @DisplayName("회원 탈퇴 성공")
    void withdraw_Success() {
        // given
        doNothing().when(userWithdrawService).withdraw(testUser);

        // when
        authService.withdraw(testUser);

        // then
        verify(oAuthService).unlinkKakaoAccount(testUser.getKakaoId());
        verify(userWithdrawService).withdraw(testUser);
    }

    @Test
    @DisplayName("카카오 로그인 - 탈퇴한 사용자는 status=WITHDRAWN_PENDING 반환")
    void loginWithKakao_DeletedUser_ReturnsWithdrawnPending() {
        // given
        String kakaoAccessToken = "kakao_access_token";
        User deletedUser = User.builder()
                .id(2L)
                .kakaoId("test_kakao_id")
                .name("탈퇴한 사용자")
                .userType(UserType.WORKER)
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
        deletedUser.withdraw();

        when(oAuthService.getKakaoUserInfo(kakaoAccessToken)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.of(deletedUser));

        // when
        AuthDto.KakaoLoginResult result = authService.loginWithKakao(kakaoAccessToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("WITHDRAWN_PENDING");
        // 평탄 구조: 정상 로그인용 필드는 모두 null
        assertThat(result.getAccessToken()).isNull();
        assertThat(result.getRefreshToken()).isNull();
        assertThat(result.getUserId()).isNull();
        assertThat(result.getWithdrawnAccount()).isNotNull();
        assertThat(result.getWithdrawnAccount().getName()).isEqualTo("탈퇴한 사용자");
        assertThat(result.getWithdrawnAccount().getUserType()).isEqualTo("WORKER");
        assertThat(result.getWithdrawnAccount().getWithdrawnAt()).isNotNull();
        assertThat(result.getWithdrawnAccount().getProfileImageUrl()).isEqualTo("https://example.com/profile.jpg");

        verify(tokenService, never()).generateTokenPair(anyLong());
    }

    @Test
    @DisplayName("탈퇴 계정 복구 성공 - User.deletedAt이 null로 되돌아감")
    void restoreWithKakao_Success() {
        // given
        String kakaoAccessToken = "kakao_access_token";
        User deletedUser = User.builder()
                .id(2L)
                .kakaoId("test_kakao_id")
                .name("탈퇴한 사용자")
                .userType(UserType.WORKER)
                .build();
        deletedUser.withdraw();

        when(oAuthService.getKakaoUserInfo(kakaoAccessToken)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.of(deletedUser));
        when(tokenService.generateTokenPair(deletedUser.getId())).thenReturn(tokenPair);

        // when
        AuthDto.LoginResponse response = authService.restoreWithKakao(kakaoAccessToken);

        // then
        assertThat(deletedUser.isDeleted()).isFalse();
        assertThat(response.getAccessToken()).isEqualTo("test_access_token");
        assertThat(response.getRefreshToken()).isEqualTo("test_refresh_token");
        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getName()).isEqualTo("탈퇴한 사용자");
        assertThat(response.getUserType()).isEqualTo("WORKER");
    }

    @Test
    @DisplayName("탈퇴 계정 복구 성공 - EMPLOYER 타입은 사업장 재활성화 호출")
    void restoreWithKakao_Employer_ActivatesWorkplaces() {
        // given
        String kakaoAccessToken = "kakao_access_token";
        User deletedEmployer = User.builder()
                .id(3L)
                .kakaoId("test_kakao_id")
                .name("탈퇴한 고용주")
                .userType(UserType.EMPLOYER)
                .build();
        deletedEmployer.withdraw();

        when(oAuthService.getKakaoUserInfo(kakaoAccessToken)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.of(deletedEmployer));
        when(tokenService.generateTokenPair(deletedEmployer.getId())).thenReturn(tokenPair);

        // when
        authService.restoreWithKakao(kakaoAccessToken);

        // then
        verify(userWithdrawService).restoreEmployerWorkplaces(deletedEmployer);
    }

    @Test
    @DisplayName("탈퇴 계정 복구 실패 - 정상 계정에 호출 시 USER_NOT_WITHDRAWN 예외")
    void restoreWithKakao_NotWithdrawn_ThrowsException() {
        // given
        String kakaoAccessToken = "kakao_access_token";
        when(oAuthService.getKakaoUserInfo(kakaoAccessToken)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> authService.restoreWithKakao(kakaoAccessToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("탈퇴 상태가 아닌 계정입니다");

        verify(tokenService, never()).generateTokenPair(anyLong());
    }

    @Test
    @DisplayName("탈퇴 계정 완전 삭제 후 재가입 성공 - hardDelete 직후 flush 호출로 unique 제약 회피")
    void purgeAndRegisterWithKakao_Success() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("새이름")
                .phone("010-1234-5678")
                .userType("WORKER")
                .bankName("카카오뱅크")
                .accountNumber("3333123456789")
                .build();

        User deletedUser = User.builder()
                .id(2L)
                .kakaoId("test_kakao_id")
                .name("탈퇴한 사용자")
                .userType(UserType.WORKER)
                .build();
        deletedUser.withdraw();

        UserDto.RegisterResponse registerResponse = UserDto.RegisterResponse.builder()
                .userId(3L)
                .name("새이름")
                .userType(UserType.WORKER)
                .workerCode("WORKER002")
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.of(deletedUser));
        when(userService.register(any(UserDto.RegisterRequest.class))).thenReturn(registerResponse);
        when(tokenService.generateTokenPair(3L)).thenReturn(tokenPair);

        // when
        AuthDto.LoginResponse response = authService.purgeAndRegisterWithKakao(request);

        // then: hardDelete → flush → register 순서 검증 (JPA flush 순서 이슈 방지)
        InOrder inOrder = inOrder(userHardDeleteService, userRepository, userService);
        inOrder.verify(userHardDeleteService).hardDeleteUser(deletedUser.getId());
        inOrder.verify(userRepository).flush();
        inOrder.verify(userService).register(any(UserDto.RegisterRequest.class));

        assertThat(response.getAccessToken()).isEqualTo("test_access_token");
        assertThat(response.getRefreshToken()).isEqualTo("test_refresh_token");
        assertThat(response.getUserId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("탈퇴 계정 완전 삭제 후 재가입 - register 실패 시 호출자에게 예외 전파 (트랜잭션 롤백 트리거)")
    void purgeAndRegisterWithKakao_RegisterFails_PropagatesException() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("새이름")
                .phone("010-1234-5678")
                .userType("WORKER")
                .bankName("카카오뱅크")
                .accountNumber("3333123456789")
                .build();

        User deletedUser = User.builder()
                .id(2L)
                .kakaoId("test_kakao_id")
                .name("탈퇴한 사용자")
                .userType(UserType.WORKER)
                .build();
        deletedUser.withdraw();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.of(deletedUser));
        when(userService.register(any(UserDto.RegisterRequest.class)))
                .thenThrow(new RuntimeException("DB 오류"));

        // when & then: 예외 전파 → @Transactional이 롤백을 트리거함 (실제 롤백은 통합 테스트로 검증)
        assertThatThrownBy(() -> authService.purgeAndRegisterWithKakao(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB 오류");

        verify(userHardDeleteService).hardDeleteUser(deletedUser.getId());
        verify(userRepository).flush();
        verify(userService).register(any(UserDto.RegisterRequest.class));
    }

    @Test
    @DisplayName("탈퇴 계정 완전 삭제 후 재가입 실패 - 정상 계정 차단")
    void purgeAndRegisterWithKakao_ActiveUser_ThrowsException() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("이름")
                .phone("010-1234-5678")
                .userType("WORKER")
                .bankName("카카오뱅크")
                .accountNumber("3333123456789")
                .build();

        when(oAuthService.getKakaoUserInfo(request.getKakaoAccessToken())).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.kakaoId())).thenReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> authService.purgeAndRegisterWithKakao(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이미 가입된 카카오 계정입니다");

        verify(userHardDeleteService, never()).hardDeleteUser(anyLong());
        verify(userService, never()).register(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 잘못된 UserType")
    void registerWithKakao_InvalidUserType() {
        // given
        AuthDto.KakaoRegisterRequest request = AuthDto.KakaoRegisterRequest.builder()
                .kakaoAccessToken("kakao_access_token")
                .name("홍길동")
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
