package com.example.wagemanager.domain.employer.service;

import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.employer.entity.Employer;
import com.example.wagemanager.domain.employer.repository.EmployerRepository;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployerService 테스트")
class EmployerServiceTest {

    @Mock
    private EmployerRepository employerRepository;

    @InjectMocks
    private EmployerService employerService;

    private User testUser;
    private Employer testEmployer;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("테스트 고용주")
                .userType(UserType.EMPLOYER)
                .build();

        testEmployer = Employer.builder()
                .id(1L)
                .user(testUser)
                .phone("010-1234-5678")
                .build();
    }

    @Test
    @DisplayName("고용주 생성 성공")
    void createEmployer_Success() {
        // given
        when(employerRepository.save(any(Employer.class))).thenReturn(testEmployer);

        // when
        Employer result = employerService.createEmployer(testUser, "010-1234-5678");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPhone()).isEqualTo("010-1234-5678");
        verify(employerRepository).save(any(Employer.class));
    }

    @Test
    @DisplayName("사용자 ID로 고용주 조회 성공")
    void getEmployerByUserId_Success() {
        // given
        when(employerRepository.findByUserId(1L)).thenReturn(Optional.of(testEmployer));

        // when
        Employer result = employerService.getEmployerByUserId(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(1L);
        verify(employerRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("사용자 ID로 고용주 조회 실패")
    void getEmployerByUserId_NotFound() {
        // given
        when(employerRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> employerService.getEmployerByUserId(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("고용주 정보를 찾을 수 없습니다");

        verify(employerRepository).findByUserId(999L);
    }
}
