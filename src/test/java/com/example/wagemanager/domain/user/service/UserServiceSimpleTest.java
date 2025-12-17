package com.example.wagemanager.domain.user.service;

import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.employer.service.EmployerService;
import com.example.wagemanager.domain.settings.service.UserSettingsService;
import com.example.wagemanager.domain.user.dto.UserDto;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.user.repository.UserRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 간단 테스트")
class UserServiceSimpleTest {

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

    private User testWorker;

    @BeforeEach
    void setUp() {
        testWorker = User.builder()
                .id(1L)
                .kakaoId("worker_kakao_id")
                .name("근로자 테스트")
                .phone("010-1111-1111")
                .userType(UserType.WORKER)
                .profileImageUrl("https://example.com/worker.jpg")
                .build();
    }

    @Test
    @DisplayName("사용자 ID로 조회 성공")
    void getUserById_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testWorker));

        // when
        UserDto.Response result = userService.getUserById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("근로자 테스트");
        assertThat(result.getUserType()).isEqualTo(UserType.WORKER);

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("사용자 ID로 조회 실패 - 존재하지 않는 사용자")
    void getUserById_NotFound() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");

        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("사용자 정보 업데이트 성공")
    void updateUser_Success() {
        // given
        UserDto.UpdateRequest request = UserDto.UpdateRequest.builder()
                .name("수정된 이름")
                .phone("010-9999-9999")
                .profileImageUrl("https://example.com/new_profile.jpg")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testWorker));

        // when
        UserDto.Response result = userService.updateUser(1L, request);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("사용자 정보 업데이트 실패 - 존재하지 않는 사용자")
    void updateUser_NotFound() {
        // given
        UserDto.UpdateRequest request = UserDto.UpdateRequest.builder()
                .name("수정된 이름")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUser(999L, request))
                .isInstanceOf(NotFoundException.class);
    }
}
