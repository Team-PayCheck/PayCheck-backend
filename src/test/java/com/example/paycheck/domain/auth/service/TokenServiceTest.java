package com.example.paycheck.domain.auth.service;

import com.example.paycheck.domain.auth.entity.RefreshToken;
import com.example.paycheck.domain.auth.repository.RefreshTokenRepository;
import com.example.paycheck.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 테스트")
class TokenServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private TokenService tokenService;

    @Test
    @DisplayName("Access Token 생성 성공")
    void generateAccessToken_Success() {
        // given
        when(jwtTokenProvider.generateToken(1L)).thenReturn("access_token");

        // when
        String result = tokenService.generateAccessToken(1L);

        // then
        assertThat(result).isEqualTo("access_token");
        verify(jwtTokenProvider).generateToken(1L);
    }

    @Test
    @DisplayName("Refresh Token 생성 및 저장 성공 - 신규 사용자")
    void generateAndSaveRefreshToken_Success_NewUser() {
        // given
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh_token");
        when(jwtTokenProvider.getRefreshExpirationTime()).thenReturn(2592000000L);
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(null);

        // when
        String result = tokenService.generateAndSaveRefreshToken(1L);

        // then
        assertThat(result).isEqualTo("refresh_token");
        verify(refreshTokenRepository).findByUserId(1L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh Token 생성 및 저장 성공 - 기존 사용자 (Upsert)")
    void generateAndSaveRefreshToken_Success_ExistingUser() {
        // given
        RefreshToken existingToken = RefreshToken.builder()
                .userId(1L)
                .token("old_refresh_token")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("new_refresh_token");
        when(jwtTokenProvider.getRefreshExpirationTime()).thenReturn(2592000000L);
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(null);

        // when
        String result = tokenService.generateAndSaveRefreshToken(1L);

        // then
        assertThat(result).isEqualTo("new_refresh_token");
        verify(refreshTokenRepository).findByUserId(1L);
        verify(refreshTokenRepository).save(existingToken);
    }

    @Test
    @DisplayName("토큰 쌍 생성 성공")
    void generateTokenPair_Success() {
        // given
        when(jwtTokenProvider.generateToken(1L)).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh_token");
        when(jwtTokenProvider.getRefreshExpirationTime()).thenReturn(2592000000L);
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(null);

        // when
        TokenService.TokenPair result = tokenService.generateTokenPair(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access_token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh_token");
        verify(jwtTokenProvider).generateToken(1L);
        verify(jwtTokenProvider).generateRefreshToken(1L);
    }

    @Test
    @DisplayName("Refresh Token 폐기 성공")
    void revokeRefreshToken_Success() {
        // given
        doNothing().when(refreshTokenRepository).deleteByUserId(1L);

        // when
        tokenService.revokeRefreshToken(1L);

        // then
        verify(refreshTokenRepository).deleteByUserId(1L);
    }
}
