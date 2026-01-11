package com.example.wagemanager.domain.user.service;

import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.employer.entity.Employer;
import com.example.wagemanager.domain.employer.service.EmployerService;
import com.example.wagemanager.domain.settings.service.UserSettingsService;
import com.example.wagemanager.domain.user.dto.UserDto;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.worker.service.WorkerService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkerService workerService;

    @Mock
    private EmployerService employerService;

    @Mock
    private UserSettingsService userSettingsService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .kakaoId("test_kakao_123")
                .name("테스트 사용자")
                .phone("010-1234-5678")
                .userType(UserType.WORKER)
                .build();
    }

    @Test
    @DisplayName("사용자 ID로 조회 실패 - 사용자 없음")
    void getUserById_Fail_NotFound() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 사용자 없음")
    void updateUser_Fail_NotFound() {
        // given
        UserDto.UpdateRequest request = UserDto.UpdateRequest.builder()
                .name("수정된 이름")
                .phone("010-9999-9999")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUser(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("근로자 회원가입 성공")
    void register_Worker_Success() {
        // given
        UserDto.RegisterRequest request = UserDto.RegisterRequest.builder()
                .kakaoId("new_kakao_123")
                .name("신규 근로자")
                .phone("010-1111-2222")
                .userType(UserType.WORKER)
                .bankName("카카오뱅크")
                .accountNumber("333312341234")
                .build();

        User savedUser = User.builder()
                .id(2L)
                .kakaoId(request.getKakaoId())
                .name(request.getName())
                .phone(request.getPhone())
                .userType(request.getUserType())
                .build();

        Worker worker = mock(Worker.class);
        when(worker.getWorkerCode()).thenReturn("WRK123");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(workerService.createWorker(any(User.class), any(), any())).thenReturn(worker);

        // when
        UserDto.RegisterResponse result = userService.register(request);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(userSettingsService).createDefaultSettings(2L);
        verify(workerService).createWorker(any(User.class), eq("카카오뱅크"), eq("333312341234"));
        verify(employerService, never()).createEmployer(any(), any());
    }

    @Test
    @DisplayName("고용주 회원가입 성공")
    void register_Employer_Success() {
        // given
        UserDto.RegisterRequest request = UserDto.RegisterRequest.builder()
                .kakaoId("new_employer_123")
                .name("신규 고용주")
                .phone("010-3333-4444")
                .userType(UserType.EMPLOYER)
                .build();

        User savedUser = User.builder()
                .id(3L)
                .kakaoId(request.getKakaoId())
                .name(request.getName())
                .phone(request.getPhone())
                .userType(request.getUserType())
                .build();

        Employer employer = mock(Employer.class);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(employerService.createEmployer(any(User.class), any())).thenReturn(employer);

        // when
        UserDto.RegisterResponse result = userService.register(request);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(userSettingsService).createDefaultSettings(3L);
        verify(employerService).createEmployer(any(User.class), eq("010-3333-4444"));
        verify(workerService, never()).createWorker(any(), any(), any());
    }
}
