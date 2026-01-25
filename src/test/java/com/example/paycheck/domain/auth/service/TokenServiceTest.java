package com.example.paycheck.domain.auth.service;

import com.example.paycheck.domain.auth.repository.RefreshTokenRepository;
import com.example.paycheck.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("Refresh Token 생성 및 저장 성공 - DB 레벨 Upsert")
    void generateAndSaveRefreshToken_Success() {
        // given
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh_token");
        when(jwtTokenProvider.getRefreshExpirationTime()).thenReturn(2592000000L);
        doNothing().when(refreshTokenRepository).upsertRefreshToken(eq(1L), eq("refresh_token"), any(LocalDateTime.class));

        // when
        String result = tokenService.generateAndSaveRefreshToken(1L);

        // then
        assertThat(result).isEqualTo("refresh_token");
        verify(refreshTokenRepository).upsertRefreshToken(eq(1L), eq("refresh_token"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("토큰 쌍 생성 성공")
    void generateTokenPair_Success() {
        // given
        when(jwtTokenProvider.generateToken(1L)).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh_token");
        when(jwtTokenProvider.getRefreshExpirationTime()).thenReturn(2592000000L);
        doNothing().when(refreshTokenRepository).upsertRefreshToken(eq(1L), eq("refresh_token"), any(LocalDateTime.class));

        // when
        TokenService.TokenPair result = tokenService.generateTokenPair(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access_token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh_token");
        verify(jwtTokenProvider).generateToken(1L);
        verify(jwtTokenProvider).generateRefreshToken(1L);
        verify(refreshTokenRepository).upsertRefreshToken(eq(1L), eq("refresh_token"), any(LocalDateTime.class));
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
