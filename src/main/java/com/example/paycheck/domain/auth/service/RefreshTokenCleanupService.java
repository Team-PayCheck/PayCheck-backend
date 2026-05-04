package com.example.paycheck.domain.auth.service;

import com.example.paycheck.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh Token 정리 서비스
 * 별도 트랜잭션에서 실행되어야 하는 토큰 삭제 작업을 담당
 * (Self-Invocation 문제 방지를 위해 별도 클래스로 분리)
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 만료된 토큰 삭제 (별도 트랜잭션에서 실행)
     * 예외 발생 시에도 삭제가 커밋되어야 하므로 REQUIRES_NEW 사용
     *
     * @param tokenString 삭제할 토큰 문자열
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteExpiredToken(String tokenString) {
        refreshTokenRepository.findByToken(tokenString)
                .ifPresent(refreshTokenRepository::delete);
    }
}
