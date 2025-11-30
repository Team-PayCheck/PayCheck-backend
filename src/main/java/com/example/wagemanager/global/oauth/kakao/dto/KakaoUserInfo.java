package com.example.wagemanager.global.oauth.kakao.dto;

import lombok.Builder;

@Builder
public record KakaoUserInfo(
        String kakaoId,
        String name,
        String nickname,
        String phoneNumber,
        String profileImageUrl
) {

    public String displayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return nickname;
    }
}
