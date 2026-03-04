package com.example.paycheck.domain.fcm.repository;

import com.example.paycheck.domain.fcm.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findByUserId(Long userId);

    Optional<FcmToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUserId(Long userId);
}
