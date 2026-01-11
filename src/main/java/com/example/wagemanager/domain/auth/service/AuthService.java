package com.example.wagemanager.domain.auth.service;

import com.example.wagemanager.common.exception.BadRequestException;
import com.example.wagemanager.common.exception.ErrorCode;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.auth.dto.AuthDto;
import com.example.wagemanager.domain.employer.entity.Employer;
import com.example.wagemanager.domain.employer.repository.EmployerRepository;
import com.example.wagemanager.domain.user.dto.UserDto;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.example.wagemanager.domain.user.service.UserService;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.worker.repository.WorkerRepository;
import com.example.wagemanager.global.oauth.kakao.dto.KakaoUserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 인증 플로우를 조율하는 서비스
 * OAuth, Token 서비스를 조합하여 로그인/회원가입 등의 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OAuthService oAuthService;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmployerRepository employerRepository;
    private final WorkerRepository workerRepository;

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

    /**
     * 카카오 계정으로 로그인
     *
     * @param kakaoAccessToken 카카오 액세스 토큰
     * @return 로그인 결과 (응답 DTO + Refresh Token)
     * @throws NotFoundException 등록되지 않은 카카오 계정인 경우
     */
    @Transactional
    public LoginResult loginWithKakao(String kakaoAccessToken) {
        // 카카오 사용자 정보 조회 및 검증
        KakaoUserInfo userInfo = oAuthService.getKakaoUserInfo(kakaoAccessToken);

        // 사용자 조회
        User user = userRepository.findByKakaoId(userInfo.kakaoId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "등록되지 않은 카카오 계정입니다. 회원가입을 진행해주세요."
                ));

        // 토큰 생성
        TokenService.TokenPair tokenPair = tokenService.generateTokenPair(user.getId());

        // 응답 DTO 생성
        AuthDto.LoginResponse loginResponse = AuthDto.LoginResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .userId(user.getId())
                .name(user.getName())
                .userType(user.getUserType().name())
                .build();

        return LoginResult.builder()
                .loginResponse(loginResponse)
                .refreshToken(tokenPair.getRefreshToken())
                .build();
    }

    /**
     * 로그아웃
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void logout(Long userId) {
        tokenService.revokeRefreshToken(userId);
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token과 Refresh Token 발급
     *
     * @param refreshTokenString Refresh Token 문자열
     * @return 토큰 갱신 결과 (응답 DTO + Refresh Token)
     */
    @Transactional
    public RefreshResult refreshAccessToken(String refreshTokenString) {
        // 토큰 갱신
        TokenService.TokenPair tokenPair = tokenService.refreshAccessToken(refreshTokenString);

        // 응답 DTO 생성
        AuthDto.RefreshResponse refreshResponse = AuthDto.RefreshResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .build();

        return RefreshResult.builder()
                .refreshResponse(refreshResponse)
                .refreshToken(tokenPair.getRefreshToken())
                .build();
    }

    /**
     * 카카오 계정으로 회원가입
     *
     * @param request 회원가입 요청 DTO
     * @return 로그인 결과 (응답 DTO + Refresh Token)
     * @throws BadRequestException 이미 가입된 카카오 계정이거나 필수 정보가 누락된 경우
     */
    @Transactional
    public LoginResult registerWithKakao(AuthDto.KakaoRegisterRequest request) {
        // 카카오 사용자 정보 조회 및 검증
        KakaoUserInfo userInfo = oAuthService.getKakaoUserInfo(request.getKakaoAccessToken());

        // 중복 가입 확인
        if (userRepository.findByKakaoId(userInfo.kakaoId()).isPresent()) {
            throw new BadRequestException(ErrorCode.DUPLICATE_KAKAO_ACCOUNT, "이미 가입된 카카오 계정입니다.");
        }

        // 사용자 타입 파싱
        UserType userType = parseUserType(request.getUserType());

        // WORKER 타입인 경우 은행/계좌 정보 필수 검증
        if (userType == UserType.WORKER &&
                (!StringUtils.hasText(request.getBankName()) || !StringUtils.hasText(request.getAccountNumber()))) {
            throw new BadRequestException(
                    ErrorCode.WORKER_BANK_INFO_REQUIRED,
                    "근로자 타입은 은행명과 계좌번호가 필수입니다."
            );
        }

        // 회원가입 요청 DTO 생성
        UserDto.RegisterRequest registerRequest = UserDto.RegisterRequest.builder()
                .kakaoId(userInfo.kakaoId())
                .name(oAuthService.resolveDisplayName(userInfo))
                .phone(request.getPhone())
                .userType(userType)
                .profileImageUrl(request.getProfileImageUrl())
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .build();

        // 회원가입 처리
        UserDto.RegisterResponse registerResponse = userService.register(registerRequest);

        // 토큰 생성
        TokenService.TokenPair tokenPair = tokenService.generateTokenPair(registerResponse.getUserId());

        // 응답 DTO 생성
        AuthDto.LoginResponse loginResponse = AuthDto.LoginResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .userId(registerResponse.getUserId())
                .name(registerResponse.getName())
                .userType(registerResponse.getUserType().name())
                .build();

        return LoginResult.builder()
                .loginResponse(loginResponse)
                .refreshToken(tokenPair.getRefreshToken())
                .build();
    }

    /**
     * 사용자 타입 문자열을 UserType enum으로 변환
     *
     * @param userType 사용자 타입 문자열
     * @return UserType enum
     * @throws BadRequestException 유효하지 않은 사용자 유형인 경우
     */
    private UserType parseUserType(String userType) {
        try {
            return UserType.valueOf(userType.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException(ErrorCode.INVALID_USER_TYPE, "유효하지 않은 사용자 유형입니다. EMPLOYER 또는 WORKER를 입력해주세요.");
        }
    }

    /**
     * 개발용 사용자 생성 (Employer/Worker 엔티티도 함께 생성)
     *
     * @param userId 사용자 ID
     * @param name 사용자 이름
     * @param userType 사용자 타입
     * @return 생성된 사용자
     */
    @SuppressWarnings("null")
    private User createDevUser(Long userId, String name, UserType userType) {
        // User 생성
        User newUser = User.builder()
                .kakaoId("dev_" + userId) // 개발용 임시 kakaoId
                .name(name)
                .userType(userType)
                .profileImageUrl("")
                .build();
        User savedUser = userRepository.save(newUser);

        // 사용자 타입에 따라 Employer 또는 Worker 생성
        if (userType == UserType.EMPLOYER) {
            Employer employer = Employer.builder()
                    .user(savedUser)
                    .phone("010-0000-0000") // 개발용 임시 전화번호
                    .build();
            employerRepository.save(employer);
        } else if (userType == UserType.WORKER) {
            Worker worker = Worker.builder()
                    .user(savedUser)
                    .workerCode("DEV" + String.format("%03d", userId % 1000)) // 개발용 임시 근로자 코드
                    .bankName("카카오뱅크")
                    .accountNumber("3333000" + userId)
                    .build();
            workerRepository.save(worker);
        }

        return savedUser;
    }

    /**
     * 개발용 임시 로그인 (사용자가 없으면 생성하여 DB에 저장)
     * 주의: 개발 환경에서만 사용하고 배포 환경에서는 반드시 비활성화 해야 함
     *
     * @param request 개발용 로그인 요청 DTO
     * @return 로그인 결과 (응답 DTO + Refresh Token)
     */
    @Transactional
    public LoginResult devLogin(AuthDto.DevLoginRequest request) {
        Long requestedUserId = Long.parseLong(request.getUserId());
        UserType userType = parseUserType(request.getUserType());
        String devKakaoId = "dev_" + requestedUserId;

        // kakaoId로 사용자 조회 또는 생성 (findById가 아닌 findByKakaoId 사용)
        User user = userRepository.findByKakaoId(devKakaoId)
                .orElseGet(() -> createDevUser(requestedUserId, request.getName(), userType));

        // 기존 사용자인 경우, Employer/Worker가 없으면 생성
        if (user.getUserType() == UserType.EMPLOYER && employerRepository.findByUserId(user.getId()).isEmpty()) {
            Employer employer = Employer.builder()
                    .user(user)
                    .phone("010-0000-0000")
                    .build();
            employerRepository.save(employer);
        } else if (user.getUserType() == UserType.WORKER && workerRepository.findByUserId(user.getId()).isEmpty()) {
            Worker worker = Worker.builder()
                    .user(user)
                    .workerCode("DEV" + String.format("%03d", requestedUserId % 1000))
                    .bankName("카카오뱅크")
                    .accountNumber("3333000" + requestedUserId)
                    .build();
            workerRepository.save(worker);
        }

        // 토큰 생성
        TokenService.TokenPair tokenPair = tokenService.generateTokenPair(user.getId());

        // 응답 DTO 생성
        AuthDto.LoginResponse loginResponse = AuthDto.LoginResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .userId(user.getId())
                .name(user.getName())
                .userType(user.getUserType().name())
                .build();

        return LoginResult.builder()
                .loginResponse(loginResponse)
                .refreshToken(tokenPair.getRefreshToken())
                .build();
    }
}
