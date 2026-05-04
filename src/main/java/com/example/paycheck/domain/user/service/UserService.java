package com.example.paycheck.domain.user.service;

import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.employer.service.EmployerService;
import com.example.paycheck.domain.settings.service.UserSettingsService;
import com.example.paycheck.domain.user.dto.UserDto;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.user.repository.UserRepository;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final WorkerService workerService;
    private final EmployerService employerService;
    private final UserSettingsService userSettingsService;
    private final ProfileImageStorageService profileImageStorageService;
    private final ProfileImageUrlResolver profileImageUrlResolver;

    public UserDto.Response getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (user.getUserType() == UserType.WORKER) {
            return workerRepository.findByUserId(userId)
                    .map(worker -> buildUserResponse(user, worker))
                    .orElse(buildUserResponse(user));
        }
        return buildUserResponse(user);
    }

    @Transactional
    public UserDto.Response updateUser(Long userId, UserDto.UpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.updateProfile(request.getName(), request.getPhone(), request.getProfileImageUrl());

        return getUserById(userId);
    }

    @Transactional
    public UserDto.Response updateProfileImage(Long userId, MultipartFile profileImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        String previousProfileImageUrl = user.getProfileImageUrl();
        String storedProfileImageUrl = profileImageStorageService.store(profileImage);
        user.updateProfile(null, null, storedProfileImageUrl);

        if (previousProfileImageUrl != null && !previousProfileImageUrl.equals(storedProfileImageUrl)) {
            profileImageStorageService.deleteIfStoredLocally(previousProfileImageUrl);
        }

        return getUserById(userId);
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

    private UserDto.Response buildUserResponse(User user) {
        return UserDto.Response.from(user, profileImageUrlResolver.resolve(user.getProfileImageUrl()));
    }

    private UserDto.Response buildUserResponse(User user, Worker worker) {
        return UserDto.Response.from(user, worker, profileImageUrlResolver.resolve(user.getProfileImageUrl()));
    }
}
