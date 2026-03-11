package com.example.paycheck.domain.auth.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.UnauthorizedException;
import com.example.paycheck.global.oauth.kakao.KakaoOAuthClient;
import com.example.paycheck.global.oauth.kakao.dto.KakaoUserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthService 단위 테스트")
class OAuthServiceTest {

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @InjectMocks
    private OAuthService oAuthService;

    @Nested
    @DisplayName("getKakaoUserInfo")
    class GetKakaoUserInfo {

        @Test
        @DisplayName("유효한 카카오 토큰으로 사용자 정보를 성공적으로 조회한다")
        void success() {
            // given
            String accessToken = "valid-kakao-token";
            KakaoUserInfo expectedUserInfo = KakaoUserInfo.builder()
                    .kakaoId("12345")
                    .name("홍길동")
                    .profileImageUrl("https://example.com/profile.jpg")
                    .build();

            when(kakaoOAuthClient.getUserInfo(accessToken)).thenReturn(expectedUserInfo);

            // when
            KakaoUserInfo result = oAuthService.getKakaoUserInfo(accessToken);

            // then
            assertThat(result).isNotNull();
            assertThat(result.kakaoId()).isEqualTo("12345");
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.profileImageUrl()).isEqualTo("https://example.com/profile.jpg");
        }

        @Test
        @DisplayName("카카오 사용자 식별자가 null이면 UnauthorizedException을 발생시킨다")
        void throwsWhenKakaoIdIsNull() {
            // given
            String accessToken = "invalid-token";
            KakaoUserInfo userInfoWithNullId = KakaoUserInfo.builder()
                    .kakaoId(null)
                    .name("홍길동")
                    .build();

            when(kakaoOAuthClient.getUserInfo(accessToken)).thenReturn(userInfoWithNullId);

            // when & then
            assertThatThrownBy(() -> oAuthService.getKakaoUserInfo(accessToken))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("카카오 사용자 식별자가 빈 문자열이면 UnauthorizedException을 발생시킨다")
        void throwsWhenKakaoIdIsEmpty() {
            // given
            String accessToken = "invalid-token";
            KakaoUserInfo userInfoWithEmptyId = KakaoUserInfo.builder()
                    .kakaoId("")
                    .name("홍길동")
                    .build();

            when(kakaoOAuthClient.getUserInfo(accessToken)).thenReturn(userInfoWithEmptyId);

            // when & then
            assertThatThrownBy(() -> oAuthService.getKakaoUserInfo(accessToken))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("resolveDisplayName")
    class ResolveDisplayName {

        @Test
        @DisplayName("카카오 사용자 정보에 이름이 있으면 이름을 반환한다")
        void returnsNameWhenPresent() {
            // given
            KakaoUserInfo userInfo = KakaoUserInfo.builder()
                    .kakaoId("12345")
                    .name("홍길동")
                    .build();

            // when
            String result = oAuthService.resolveDisplayName(userInfo);

            // then
            assertThat(result).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("카카오 사용자 정보에 이름이 없으면 BadRequestException을 발생시킨다")
        void throwsWhenNameIsNull() {
            // given
            KakaoUserInfo userInfo = KakaoUserInfo.builder()
                    .kakaoId("12345")
                    .name(null)
                    .build();

            // when & then
            assertThatThrownBy(() -> oAuthService.resolveDisplayName(userInfo))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("카카오 사용자 정보에 이름이 빈 문자열이면 BadRequestException을 발생시킨다")
        void throwsWhenNameIsEmpty() {
            // given
            KakaoUserInfo userInfo = KakaoUserInfo.builder()
                    .kakaoId("12345")
                    .name("")
                    .build();

            // when & then
            assertThatThrownBy(() -> oAuthService.resolveDisplayName(userInfo))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("unlinkKakaoAccount")
    class UnlinkKakaoAccount {

        @Test
        @DisplayName("카카오 연결 해제를 정상적으로 호출한다")
        void success() {
            // given
            String kakaoId = "12345";
            doNothing().when(kakaoOAuthClient).unlinkUser(kakaoId);

            // when
            oAuthService.unlinkKakaoAccount(kakaoId);

            // then
            verify(kakaoOAuthClient).unlinkUser(kakaoId);
        }

        @Test
        @DisplayName("카카오 연결 해제 실패 시 예외를 삼키고 정상 리턴한다")
        void swallowsExceptionOnFailure() {
            // given
            String kakaoId = "12345";
            doThrow(new RuntimeException("카카오 서버 오류"))
                    .when(kakaoOAuthClient).unlinkUser(kakaoId);

            // when & then (예외가 발생하지 않아야 함)
            oAuthService.unlinkKakaoAccount(kakaoId);

            verify(kakaoOAuthClient).unlinkUser(kakaoId);
        }
    }
}
