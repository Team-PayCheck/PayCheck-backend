package com.example.paycheck.domain.auth.repository;

import com.example.paycheck.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Refresh Token Upsert (원자적 INSERT 또는 UPDATE)
     * userId가 존재하면 UPDATE, 없으면 INSERT
     * clearAutomatically: 쿼리 실행 후 영속성 컨텍스트 초기화
     * flushAutomatically: 쿼리 실행 전 영속성 컨텍스트 flush
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO refresh_tokens (user_id, token, expires_at, created_at, updated_at) " +
                   "VALUES (:userId, :token, :expiresAt, NOW(), NOW()) " +
                   "ON DUPLICATE KEY UPDATE token = :token, expires_at = :expiresAt, updated_at = NOW()",
           nativeQuery = true)
    void upsertRefreshToken(@Param("userId") Long userId,
                            @Param("token") String token,
                            @Param("expiresAt") LocalDateTime expiresAt);

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
