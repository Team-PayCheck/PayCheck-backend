package com.example.paycheck.global.oauth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserResponse {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {
        private KakaoProfile profile;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoProfile {
        private String nickname;

        @JsonProperty("profile_image_url")
        private String profileImageUrl;
    }

    public KakaoUserInfo toUserInfo() {
        String kakaoIdValue = id != null ? String.valueOf(id) : null;
        String nameValue = null;
        String profileImageUrlValue = null;

        if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
            nameValue = kakaoAccount.getProfile().getNickname();
            profileImageUrlValue = kakaoAccount.getProfile().getProfileImageUrl();
        }

        return KakaoUserInfo.builder()
                .kakaoId(kakaoIdValue)
                .name(nameValue)
                .profileImageUrl(profileImageUrlValue)
                .build();
    }
}
