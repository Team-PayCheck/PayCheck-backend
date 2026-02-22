package com.example.paycheck.global.oauth.kakao.dto;

import lombok.Builder;

@Builder
public record KakaoUserInfo(
        String kakaoId,
        String name,
        String profileImageUrl
) {
}
