package com.example.wagemanager.domain.user.service;

import com.example.wagemanager.domain.employer.service.EmployerService;
import com.example.wagemanager.domain.user.dto.UserDto;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final WorkerService workerService;
    private final EmployerService employerService;

    public UserDto.Response getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserDto.Response.from(user);
    }

    public UserDto.Response getUserByKakaoId(String kakaoId) {
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserDto.Response.from(user);
    }

    @Transactional
    public UserDto.Response updateUser(Long userId, UserDto.UpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateProfile(request.getName(), request.getPhone(), request.getProfileImageUrl());

        return UserDto.Response.from(user);
    }

    @Transactional
    public UserDto.RegisterResponse register(UserDto.RegisterRequest request) {
        // 카카오 ID 중복 체크
        if (userRepository.findByKakaoId(request.getKakaoId()).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 카카오 계정입니다.");
        }

        // User 생성
        User user = User.builder()
                .kakaoId(request.getKakaoId())
                .name(request.getName())
                .phone(request.getPhone())
                .userType(request.getUserType())
                .profileImageUrl(request.getProfileImageUrl())
                .build();

        User savedUser = userRepository.save(user);

        String workerCode = null;

        // UserType에 따라 Worker 또는 Employer 생성
        if (request.getUserType() == UserType.WORKER) {
            Worker worker = workerService.createWorker(savedUser, request.getKakaoPayLink());
            workerCode = worker.getWorkerCode();
        } else if (request.getUserType() == UserType.EMPLOYER) {
            employerService.createEmployer(savedUser, request.getPhone());
        }

        return UserDto.RegisterResponse.from(savedUser, workerCode);
    }
}
