package com.example.paycheck.global.oauth.kakao;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.UnauthorizedException;
import com.example.paycheck.global.oauth.kakao.dto.KakaoUserInfo;
import com.example.paycheck.global.oauth.kakao.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오 사용자 정보 API 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final RestTemplate restTemplate;

    @Value("${kakao.oauth.user-info-url:https://kapi.kakao.com/v2/user/me}")
    private String userInfoUrl;

    @Value("${kakao.oauth.unlink-url:https://kapi.kakao.com/v1/user/unlink}")
    private String unlinkUrl;

    @Value("${kakao.admin-key:}")
    private String adminKey;

    /**
     * 카카오 계정 연결 해제 (어드민 키 방식)
     * POST /v1/user/unlink
     * Authorization: KakaoAK {ADMIN_KEY}
     * target_id_type=user_id, target_id={kakaoId}
     *
     * @param kakaoId 카카오 사용자 고유 ID
     */
    public void unlinkUser(String kakaoId) {
        if (!StringUtils.hasText(adminKey)) {
            log.warn("카카오 어드민 키가 설정되지 않아 연결 해제를 건너뜁니다.");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", kakaoId);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    unlinkUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BadRequestException(ErrorCode.KAKAO_UNLINK_FAILED, "카카오 연결 해제에 실패했습니다.");
            }

            log.info("카카오 연결 해제 성공: kakaoId={}", kakaoId);
        } catch (HttpStatusCodeException e) {
            log.error("Kakao unlink API error: {}", e.getResponseBodyAsString(), e);
            throw new BadRequestException(ErrorCode.KAKAO_UNLINK_FAILED, "카카오 연결 해제에 실패했습니다.");
        } catch (RestClientException e) {
            log.error("Kakao unlink API communication error", e);
            throw new BadRequestException(ErrorCode.KAKAO_SERVER_ERROR, "카카오 서버와 통신 중 오류가 발생했습니다.");
        }
    }

    public KakaoUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    KakaoUserResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BadRequestException(ErrorCode.KAKAO_USER_INFO_FAILED, "카카오 사용자 정보를 가져오지 못했습니다.");
            }

            return response.getBody().toUserInfo();
        } catch (HttpStatusCodeException e) {
            log.error("Kakao API error response: {}", e.getResponseBodyAsString(), e);
            throw new UnauthorizedException(ErrorCode.KAKAO_AUTH_FAILED, "카카오 인증에 실패했습니다.");
        } catch (RestClientException e) {
            log.error("Kakao API communication error", e);
            throw new BadRequestException(ErrorCode.KAKAO_SERVER_ERROR, "카카오 서버와 통신 중 오류가 발생했습니다.");
        }
    }
}
