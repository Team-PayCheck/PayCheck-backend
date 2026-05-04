package com.example.paycheck.domain.user.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProfileImageUrlResolver 테스트")
class ProfileImageUrlResolverTest {

    private final ProfileImageUrlResolver resolver = new ProfileImageUrlResolver();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("상대 경로 프로필 이미지를 현재 서버 기준 절대 URL로 변환한다")
    void resolve_RelativePathToAbsoluteUrl() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("api.example.com");
        request.setServerPort(443);
        request.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String resolved = resolver.resolve("/uploads/profile-images/avatar.png");

        assertThat(resolved).isEqualTo("https://api.example.com/uploads/profile-images/avatar.png");
    }

    @Test
    @DisplayName("이미 절대 URL이면 그대로 반환한다")
    void resolve_AbsoluteUrl() {
        String resolved = resolver.resolve("https://cdn.example.com/avatar.png");

        assertThat(resolved).isEqualTo("https://cdn.example.com/avatar.png");
    }
}
