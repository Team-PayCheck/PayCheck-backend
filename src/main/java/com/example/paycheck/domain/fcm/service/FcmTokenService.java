package com.example.paycheck.domain.fcm.service;

import com.example.paycheck.domain.fcm.dto.FcmTokenDto;
import com.example.paycheck.domain.fcm.entity.FcmToken;
import com.example.paycheck.domain.fcm.repository.FcmTokenRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerToken(Long userId, FcmTokenDto.RegisterRequest request) {
        Optional<FcmToken> existingToken = fcmTokenRepository.findByToken(request.getToken());
        if (existingToken.isPresent()) {
            FcmToken fcmToken = existingToken.get();
            if (!fcmToken.getUser().getId().equals(userId)) {
                User userRef = userRepository.getReferenceById(userId);
                fcmToken.turnOverTo(userRef, request.getDeviceInfo());
            }
            return;
        }

        User userRef = userRepository.getReferenceById(userId);
        saveNewToken(userRef, request);
        log.info("FCM 토큰 등록: userId={}", userId);
    }

    @Transactional
    public void deleteToken(String token) {
        fcmTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteAllTokensByUserId(Long userId) {
        fcmTokenRepository.deleteByUserId(userId);
    }

    public List<String> getTokensByUserId(Long userId) {
        return fcmTokenRepository.findByUserId(userId)
                .stream()
                .map(FcmToken::getToken)
                .toList();
    }

    private void saveNewToken(User user, FcmTokenDto.RegisterRequest request) {
        FcmToken fcmToken = FcmToken.builder()
                .user(user)
                .token(request.getToken())
                .deviceInfo(request.getDeviceInfo())
                .build();
        fcmTokenRepository.save(fcmToken);
    }
}
