package com.example.paycheck.domain.auth.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.auth.dto.AuthDto;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.employer.repository.EmployerRepository;
import com.example.paycheck.domain.user.dto.UserDto;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.user.repository.UserRepository;
import com.example.paycheck.domain.user.scheduler.UserHardDeleteScheduler;
import com.example.paycheck.domain.user.service.UserHardDeleteService;
import com.example.paycheck.domain.user.service.UserService;
import com.example.paycheck.domain.user.service.UserWithdrawService;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.global.oauth.kakao.dto.KakaoUserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;

/**
 * 인증 플로우를 조율하는 서비스
 * OAuth, Token 서비스를 조합하여 로그인/회원가입 등의 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://via.placeholder.com/150/CCCCCC/FFFFFF?text=User";

    private final OAuthService oAuthService;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserWithdrawService userWithdrawService;
    private final UserHardDeleteService userHardDeleteService;
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
     * 탈퇴 상태 사용자는 예외 대신 status="WITHDRAWN_PENDING"으로 응답하여
     * 클라이언트가 복구/재가입을 안내할 수 있게 한다.
     *
     * @param kakaoAccessToken 카카오 액세스 토큰
     * @return 로그인 결과 (LOGGED_IN 또는 WITHDRAWN_PENDING)
     * @throws NotFoundException 등록되지 않은 카카오 계정인 경우
     */
    @Transactional
    public AuthDto.KakaoLoginResult loginWithKakao(String kakaoAccessToken) {
        // 카카오 사용자 정보 조회 및 검증
        KakaoUserInfo userInfo = oAuthService.getKakaoUserInfo(kakaoAccessToken);

        // 사용자 조회
        User user = userRepository.findByKakaoId(userInfo.kakaoId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "등록되지 않은 카카오 계정입니다. 회원가입을 진행해주세요."
                ));

        // 탈퇴한 사용자: 복구/재가입 선택지를 안내하기 위한 정보 반환
        if (user.isDeleted()) {
            return AuthDto.KakaoLoginResult.withdrawnPending(
                    AuthDto.WithdrawnAccountInfo.builder()
                            .name(user.getName())
                            .userType(user.getUserType().name())
                            .withdrawnAt(user.getDeletedAt())
                            .profileImageUrl(user.getProfileImageUrl())
                            .build()
            );
        }

        // 정상 로그인
        return AuthDto.KakaoLoginResult.loggedIn(buildLoginResponse(user));
    }

    /**
     * 사용자 + 새로 발급한 토큰으로 LoginResponse 생성 (refreshToken 포함)
     */
    private AuthDto.LoginResponse buildLoginResponse(User user) {
        TokenService.TokenPair tokenPair = tokenService.generateTokenPair(user.getId());
        return AuthDto.LoginResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .refreshToken(tokenPair.getRefreshToken())
                .userId(user.getId())
                .name(user.getName())
                .userType(user.getUserType().name())
                .build();
    }

    /**
     * 탈퇴 후 복구 가능 기간(30일) 이내인지 검증한다.
     * Hard-delete 스케줄러 지연/장애로 30일을 초과한 계정이 남아있어도 복구/재가입을 차단한다.
     */
    private void assertWithinRecoveryPeriod(User user) {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusDays(UserHardDeleteScheduler.RETENTION_DAYS);
        if (user.getDeletedAt().isBefore(cutoff)) {
            throw new BadRequestException(
                    ErrorCode.RECOVERY_PERIOD_EXPIRED,
                    "복구 가능 기간(30일)이 지난 계정입니다."
            );
        }
    }

    /**
     * 탈퇴한 카카오 계정 복구 (탈퇴 취소)
     * - User.deletedAt = null로 되돌림
     * - 고용주의 경우 탈퇴 시 비활성화된 사업장을 함께 복원 (사업자번호 재등록 가능)
     * - UserSettings는 탈퇴 시 보존되었으므로 그대로 사용 (이전 알림 설정 유지)
     * - 계약/근무기록은 자동 복구하지 않음 (다른 사용자 데이터 일관성 보호)
     *
     * @param kakaoAccessToken 카카오 액세스 토큰
     * @return 로그인 응답 (refreshToken 포함)
     */
    @Transactional
    public AuthDto.LoginResponse restoreWithKakao(String kakaoAccessToken) {
        KakaoUserInfo userInfo = oAuthService.getKakaoUserInfo(kakaoAccessToken);

        User user = userRepository.findByKakaoId(userInfo.kakaoId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "등록되지 않은 카카오 계정입니다."
                ));

        if (!user.isDeleted()) {
            throw new BadRequestException(
                    ErrorCode.USER_NOT_WITHDRAWN,
                    "탈퇴 상태가 아닌 계정입니다."
            );
        }
        assertWithinRecoveryPeriod(user);

        user.restore();
        userWithdrawService.restoreEmployerWorkplaces(user);

        return buildLoginResponse(user);
    }

    /**
     * 탈퇴한 카카오 계정을 영구 삭제 후 신규 가입
     * - 기존 사용자 hard delete (30일 스케줄러와 동일 경로 재사용)
     * - 신규 회원가입 진행
     *
     * 트랜잭션: 메서드 전체를 하나의 트랜잭션으로 묶어 hard delete + register 원자성 보장.
     * register 단계에서 실패하면 hard delete도 함께 롤백되어 데이터 영구 손실을 방지한다.
     *
     * @param request 회원가입 요청 (기존 KakaoRegisterRequest 그대로 재사용)
     * @return 로그인 응답 (refreshToken 포함)
     */
    @Transactional
    public AuthDto.LoginResponse purgeAndRegisterWithKakao(AuthDto.KakaoRegisterRequest request) {
        KakaoUserInfo userInfo = oAuthService.getKakaoUserInfo(request.getKakaoAccessToken());

        // 기존 사용자가 탈퇴 상태이면 hard delete, 정상 계정이면 차단
        userRepository.findByKakaoId(userInfo.kakaoId())
                .ifPresent(existing -> {
                    if (!existing.isDeleted()) {
                        throw new BadRequestException(
                                ErrorCode.DUPLICATE_KAKAO_ACCOUNT,
                                "이미 가입된 카카오 계정입니다."
                        );
                    }
                    assertWithinRecoveryPeriod(existing);
                    userHardDeleteService.hardDeleteUser(existing.getId());
                    // JPA 기본 flush 순서(INSERT → DELETE)로 인해 같은 트랜잭션에서
                    // 신규 가입(INSERT)이 기존 사용자 DELETE보다 먼저 실행되면
                    // unique kakao_id 제약 충돌이 발생한다. 명시적 flush로 DELETE를 먼저 반영한다.
                    userRepository.flush();
                });

        return registerWithKakaoInternal(request, userInfo);
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
     * 회원 탈퇴
     * 카카오 연결 해제(어드민 키 방식) 후 내부 데이터 정리 및 소프트 삭제
     *
     * @param user 탈퇴할 사용자
     */
    public void withdraw(User user) {
        // 카카오 연결 해제 (best-effort, 트랜잭션 밖, 어드민 키 방식)
        oAuthService.unlinkKakaoAccount(user.getKakaoId());

        // 탈퇴 처리 (트랜잭션)
        userWithdrawService.withdraw(user);
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
                .refreshToken(tokenPair.getRefreshToken())
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

        AuthDto.LoginResponse loginResponse = registerWithKakaoInternal(request, userInfo);

        return LoginResult.builder()
                .loginResponse(AuthDto.LoginResponse.builder()
                        .accessToken(loginResponse.getAccessToken())
                        .userId(loginResponse.getUserId())
                        .name(loginResponse.getName())
                        .userType(loginResponse.getUserType())
                        .build())
                .refreshToken(loginResponse.getRefreshToken())
                .build();
    }

    /**
     * 카카오 회원가입 내부 로직 (OAuth 검증과 중복 확인은 호출자가 담당)
     * registerWithKakao와 purgeAndRegisterWithKakao에서 공유.
     *
     * @return refreshToken을 포함한 LoginResponse
     */
    @Transactional
    public AuthDto.LoginResponse registerWithKakaoInternal(
            AuthDto.KakaoRegisterRequest request,
            KakaoUserInfo userInfo) {
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

        // 이름 필수 검증
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "이름은 필수입니다.");
        }

        // 회원가입 요청 DTO 생성
        UserDto.RegisterRequest registerRequest = UserDto.RegisterRequest.builder()
                .kakaoId(userInfo.kakaoId())
                .name(request.getName().trim())
                .phone(request.getPhone())
                .userType(userType)
                .profileImageUrl(resolveRegisterProfileImageUrl(request.getProfileImageUrl(), userInfo.profileImageUrl()))
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .build();

        // 회원가입 처리
        UserDto.RegisterResponse registerResponse = userService.register(registerRequest);

        // 토큰 생성
        TokenService.TokenPair tokenPair = tokenService.generateTokenPair(registerResponse.getUserId());

        // 응답 DTO (refreshToken 포함)
        return AuthDto.LoginResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .refreshToken(tokenPair.getRefreshToken())
                .userId(registerResponse.getUserId())
                .name(registerResponse.getName())
                .userType(registerResponse.getUserType().name())
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
     * 회원가입 시 최종 프로필 이미지 URL을 결정하는 우선순위 로직
     * 우선순위: 사용자 입력 URL > 카카오 프로필 이미지 > 기본 placeholder
     *
     * 클라이언트가 placeholder URL을 그대로 전송한 경우에도 "이미지 없음" 으로 간주하여
     * 카카오 이미지로 대체한다. (클라이언트가 기본값을 재전송하는 경우 방어)
     */
    private String resolveRegisterProfileImageUrl(String requestedProfileImageUrl, String kakaoProfileImageUrl) {
        // 클라이언트가 명시적으로 보낸 URL이 있으면 우선 사용 (placeholder는 제외)
        if (isValidHttpUrl(requestedProfileImageUrl) && !DEFAULT_PROFILE_IMAGE_URL.equals(requestedProfileImageUrl)) {
            return requestedProfileImageUrl;
        }
        // 값이 없거나 placeholder면 카카오 이미지를 기본값으로 사용
        if (isValidHttpUrl(kakaoProfileImageUrl)) {
            return kakaoProfileImageUrl;
        }
        return DEFAULT_PROFILE_IMAGE_URL;
    }

    /**
     * 주어진 문자열이 유효한 http/https URL인지 확인
     * scheme(http/https) 존재 여부와 host 유효성을 검사한다.
     */
    private boolean isValidHttpUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && StringUtils.hasText(uri.getHost());
        } catch (URISyntaxException e) {
            return false;
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
