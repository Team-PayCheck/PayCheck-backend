package com.example.wagemanager.global.oauth.kakao;

import com.example.wagemanager.global.oauth.kakao.dto.KakaoUserInfo;
import com.example.wagemanager.global.oauth.kakao.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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
                throw new IllegalStateException("카카오 사용자 정보를 가져오지 못했습니다.");
            }

            return response.getBody().toUserInfo();
        } catch (HttpStatusCodeException e) {
            log.error("Kakao API error response: {}", e.getResponseBodyAsString(), e);
            throw new IllegalArgumentException("카카오 인증에 실패했습니다.");
        } catch (RestClientException e) {
            log.error("Kakao API communication error", e);
            throw new IllegalStateException("카카오 서버와 통신 중 오류가 발생했습니다.");
        }
    }
}
