package com.example.wagemanager.domain.auth.repository;

import com.example.wagemanager.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 문자열로 RefreshToken 조회
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 ID로 RefreshToken 조회
     */
    Optional<RefreshToken> findByUserId(Long userId);

    /**
     * 사용자 ID로 RefreshToken 삭제 (로그아웃 시 사용)
     */
    void deleteByUserId(Long userId);

    /**
     * 만료된 토큰 삭제 (배치 작업용)
     */
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
