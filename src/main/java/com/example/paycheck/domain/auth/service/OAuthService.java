package com.example.paycheck.domain.auth.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.UnauthorizedException;
import com.example.paycheck.global.oauth.kakao.KakaoOAuthClient;
import com.example.paycheck.global.oauth.kakao.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * OAuth 인증 처리를 담당하는 서비스
 * 현재는 Kakao OAuth만 지원하지만, 추후 다른 OAuth 제공자 추가 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;

    /**
     * 카카오 액세스 토큰으로 사용자 정보 조회 및 검증
     *
     * @param kakaoAccessToken 카카오 액세스 토큰
     * @return 검증된 카카오 사용자 정보
     * @throws UnauthorizedException 카카오 사용자 식별자가 유효하지 않은 경우
     */
    public KakaoUserInfo getKakaoUserInfo(String kakaoAccessToken) {
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(kakaoAccessToken);

        if (!StringUtils.hasText(userInfo.kakaoId())) {
            throw new UnauthorizedException(ErrorCode.INVALID_KAKAO_ID, "카카오 사용자 식별자를 확인할 수 없습니다.");
        }

        return userInfo;
    }

    /**
     * 카카오 사용자 정보에서 이름 추출
     *
     * @param userInfo 카카오 사용자 정보
     * @return 이름
     * @throws BadRequestException 카카오 계정의 이름 정보를 확인할 수 없는 경우
     */
    public String resolveDisplayName(KakaoUserInfo userInfo) {
        if (StringUtils.hasText(userInfo.name())) {
            return userInfo.name();
        }

        throw new BadRequestException(ErrorCode.KAKAO_NAME_NOT_FOUND, "카카오 계정의 이름 정보를 확인할 수 없습니다.");
    }

    /**
     * 카카오 계정 연결 해제 (best-effort)
     * 어드민 키 방식으로 서버에서 직접 호출
     * 실패해도 예외를 삼키고 경고 로그만 남김
     *
     * @param kakaoId 카카오 사용자 고유 ID
     */
    public void unlinkKakaoAccount(String kakaoId) {
        try {
            kakaoOAuthClient.unlinkUser(kakaoId);
        } catch (Exception e) {
            log.warn("카카오 연결 해제 실패 (탈퇴 처리는 계속 진행): {}", e.getMessage());
        }
    }
}
