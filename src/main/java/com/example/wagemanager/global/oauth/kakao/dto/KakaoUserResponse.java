package com.example.wagemanager.global.oauth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserResponse {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    private KakaoProperties properties;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {
        private String name;

        @JsonProperty("phone_number")
        private String phoneNumber;

        private KakaoProfile profile;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoProfile {
        private String nickname;

        @JsonProperty("profile_image_url")
        private String profileImageUrl;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoProperties {
        private String nickname;

        @JsonProperty("profile_image")
        private String profileImage;
    }

    public KakaoUserInfo toUserInfo() {
        String kakaoIdValue = id != null ? String.valueOf(id) : null;
        String nameValue = null;
        String nicknameValue = null;
        String phoneValue = null;
        String profileImageValue = null;

        if (kakaoAccount != null) {
            nameValue = kakaoAccount.getName();
            phoneValue = kakaoAccount.getPhoneNumber();

            KakaoProfile profile = kakaoAccount.getProfile();
            if (profile != null) {
                nicknameValue = profile.getNickname();
                profileImageValue = profile.getProfileImageUrl();
            }
        }

        if (nicknameValue == null && properties != null) {
            nicknameValue = properties.getNickname();
        }
        if (profileImageValue == null && properties != null) {
            profileImageValue = properties.getProfileImage();
        }

        return KakaoUserInfo.builder()
                .kakaoId(kakaoIdValue)
                .name(nameValue)
                .nickname(nicknameValue)
                .phoneNumber(phoneValue)
                .profileImageUrl(profileImageValue)
                .build();
    }
}
