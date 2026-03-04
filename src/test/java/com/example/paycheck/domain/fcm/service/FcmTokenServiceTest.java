package com.example.paycheck.domain.fcm.service;

import com.example.paycheck.domain.fcm.dto.FcmTokenDto;
import com.example.paycheck.domain.fcm.entity.FcmToken;
import com.example.paycheck.domain.fcm.repository.FcmTokenRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmTokenService 테스트")
class FcmTokenServiceTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FcmTokenService fcmTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .kakaoId("test_kakao")
                .name("테스트 사용자")
                .userType(UserType.WORKER)
                .build();
    }

    @Test
    @DisplayName("새 FCM 토큰 등록 성공 - getReferenceById로 SELECT 없이 프록시 사용")
    void registerToken_ShouldSaveNewToken() {
        // given
        FcmTokenDto.RegisterRequest request = FcmTokenDto.RegisterRequest.builder()
                .token("fcm_token_123")
                .deviceInfo("iPhone 15")
                .build();

        when(fcmTokenRepository.findByToken("fcm_token_123")).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);

        // when
        fcmTokenService.registerToken(1L, request);

        // then
        ArgumentCaptor<FcmToken> captor = ArgumentCaptor.forClass(FcmToken.class);
        verify(fcmTokenRepository).save(captor.capture());
        verify(userRepository).getReferenceById(1L);
        verify(userRepository, never()).findById(anyLong());

        FcmToken saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo("fcm_token_123");
        assertThat(saved.getDeviceInfo()).isEqualTo("iPhone 15");
        assertThat(saved.getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("deviceInfo 없이 토큰 등록 성공")
    void registerToken_ShouldSaveNewToken_WithoutDeviceInfo() {
        // given
        FcmTokenDto.RegisterRequest request = FcmTokenDto.RegisterRequest.builder()
                .token("fcm_token_456")
                .build();

        when(fcmTokenRepository.findByToken("fcm_token_456")).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);

        // when
        fcmTokenService.registerToken(1L, request);

        // then
        ArgumentCaptor<FcmToken> captor = ArgumentCaptor.forClass(FcmToken.class);
        verify(fcmTokenRepository).save(captor.capture());

        FcmToken saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo("fcm_token_456");
        assertThat(saved.getDeviceInfo()).isNull();
    }

    @Test
    @DisplayName("동일한 사용자의 동일한 토큰 등록 시 중복 저장하지 않음")
    void registerToken_ShouldNotDuplicate_WhenSameUserSameToken() {
        // given
        FcmTokenDto.RegisterRequest request = FcmTokenDto.RegisterRequest.builder()
                .token("fcm_token_123")
                .build();

        FcmToken existingToken = FcmToken.builder()
                .id(1L)
                .user(testUser)
                .token("fcm_token_123")
                .build();

        when(fcmTokenRepository.findByToken("fcm_token_123")).thenReturn(Optional.of(existingToken));

        // when
        fcmTokenService.registerToken(1L, request);

        // then
        verify(fcmTokenRepository, never()).save(any(FcmToken.class));
        verify(userRepository, never()).getReferenceById(anyLong());
    }

    @Test
    @DisplayName("다른 사용자의 토큰이 이미 존재하면 turnOverTo로 소유자 변경 (Dirty Checking)")
    void registerToken_ShouldTurnOver_WhenTokenBelongsToOtherUser() {
        // given
        User otherUser = User.builder()
                .id(2L)
                .kakaoId("other_kakao")
                .name("다른 사용자")
                .userType(UserType.WORKER)
                .build();

        FcmTokenDto.RegisterRequest request = FcmTokenDto.RegisterRequest.builder()
                .token("fcm_token_123")
                .deviceInfo("Galaxy S24")
                .build();

        FcmToken existingToken = FcmToken.builder()
                .id(1L)
                .user(otherUser)
                .token("fcm_token_123")
                .build();

        when(fcmTokenRepository.findByToken("fcm_token_123")).thenReturn(Optional.of(existingToken));
        when(userRepository.getReferenceById(1L)).thenReturn(testUser);

        // when
        fcmTokenService.registerToken(1L, request);

        // then - delete/save 대신 turnOverTo로 Dirty Checking UPDATE
        verify(fcmTokenRepository, never()).delete(any(FcmToken.class));
        verify(fcmTokenRepository, never()).save(any(FcmToken.class));
        assertThat(existingToken.getUser()).isEqualTo(testUser);
        assertThat(existingToken.getDeviceInfo()).isEqualTo("Galaxy S24");
    }

    @Test
    @DisplayName("사용자의 FCM 토큰 목록 조회")
    void getTokensByUserId_ShouldReturnTokenStrings() {
        // given
        List<FcmToken> tokens = List.of(
                FcmToken.builder().id(1L).user(testUser).token("token_1").build(),
                FcmToken.builder().id(2L).user(testUser).token("token_2").build()
        );

        when(fcmTokenRepository.findByUserId(1L)).thenReturn(tokens);

        // when
        List<String> result = fcmTokenService.getTokensByUserId(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("token_1", "token_2");
    }

    @Test
    @DisplayName("토큰이 없는 사용자 조회 시 빈 리스트 반환")
    void getTokensByUserId_ShouldReturnEmptyList_WhenNoTokens() {
        // given
        when(fcmTokenRepository.findByUserId(999L)).thenReturn(Collections.emptyList());

        // when
        List<String> result = fcmTokenService.getTokensByUserId(999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("FCM 토큰 삭제")
    void deleteToken_ShouldCallRepository() {
        // when
        fcmTokenService.deleteToken("fcm_token_123");

        // then
        verify(fcmTokenRepository).deleteByToken("fcm_token_123");
    }

    @Test
    @DisplayName("사용자의 모든 FCM 토큰 삭제")
    void deleteAllTokensByUserId_ShouldCallRepository() {
        // when
        fcmTokenService.deleteAllTokensByUserId(1L);

        // then
        verify(fcmTokenRepository).deleteByUserId(1L);
    }
}
