package com.example.wagemanager.domain.user.entity;

import com.example.wagemanager.domain.user.enums.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User 엔티티 테스트")
class UserTest {

    @Test
    @DisplayName("User 생성 성공")
    void createUser_Success() {
        // when
        User user = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("테스트 사용자")
                .phone("010-1234-5678")
                .userType(UserType.WORKER)
                .profileImageUrl("https://example.com/profile.jpg")
                .build();

        // then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getName()).isEqualTo("테스트 사용자");
        assertThat(user.getUserType()).isEqualTo(UserType.WORKER);
    }

    @Test
    @DisplayName("User 프로필 업데이트 성공")
    void updateProfile_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("원래 이름")
                .phone("010-1234-5678")
                .userType(UserType.WORKER)
                .build();

        // when
        user.updateProfile("새로운 이름", "010-9999-9999", "https://example.com/new.jpg");

        // then
        assertThat(user.getName()).isEqualTo("새로운 이름");
    }

    @Test
    @DisplayName("User 프로필 부분 업데이트 - 이름만")
    void updateProfile_PartialUpdate() {
        // given
        User user = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("원래 이름")
                .phone("010-1234-5678")
                .userType(UserType.WORKER)
                .build();

        // when
        user.updateProfile("새로운 이름", null, null);

        // then
        assertThat(user.getName()).isEqualTo("새로운 이름");
    }
}
