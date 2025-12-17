package com.example.wagemanager.domain.employer.entity;

import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Employer 엔티티 테스트")
class EmployerTest {

    @Test
    @DisplayName("Employer 생성 성공")
    void createEmployer_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("테스트 고용주")
                .userType(UserType.EMPLOYER)
                .build();

        // when
        Employer employer = Employer.builder()
                .id(1L)
                .user(user)
                .phone("010-1234-5678")
                .build();

        // then
        assertThat(employer).isNotNull();
        assertThat(employer.getPhone()).isEqualTo("010-1234-5678");
        assertThat(employer.getUser()).isEqualTo(user);
    }
}
