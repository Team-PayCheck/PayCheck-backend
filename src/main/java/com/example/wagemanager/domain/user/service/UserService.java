package com.example.wagemanager.domain.user.service;

import com.example.wagemanager.common.exception.ErrorCode;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.employer.service.EmployerService;
import com.example.wagemanager.domain.settings.service.UserSettingsService;
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
    private final UserSettingsService userSettingsService;

    public UserDto.Response getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return UserDto.Response.from(user);
    }

    @Transactional
    public UserDto.Response updateUser(Long userId, UserDto.UpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.updateProfile(request.getName(), request.getPhone(), request.getProfileImageUrl());

        return UserDto.Response.from(user);
    }

    @Transactional
    public UserDto.RegisterResponse register(UserDto.RegisterRequest request) {
        // User 생성
        User user = User.builder()
                .kakaoId(request.getKakaoId())
                .name(request.getName())
                .phone(request.getPhone())
                .userType(request.getUserType())
                .profileImageUrl(request.getProfileImageUrl())
                .build();

        User savedUser = userRepository.save(user);

        // UserSettings 자동 생성
        userSettingsService.createDefaultSettings(savedUser.getId());

        String workerCode = null;

        // UserType에 따라 Worker 또는 Employer 생성
        if (request.getUserType() == UserType.WORKER) {
            Worker worker = workerService.createWorker(savedUser, request.getBankName(), request.getAccountNumber());
            workerCode = worker.getWorkerCode();
        } else if (request.getUserType() == UserType.EMPLOYER) {
            employerService.createEmployer(savedUser, request.getPhone());
        }

        return UserDto.RegisterResponse.from(savedUser, workerCode);
    }
}
