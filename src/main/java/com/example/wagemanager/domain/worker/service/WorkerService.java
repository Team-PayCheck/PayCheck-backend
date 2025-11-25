package com.example.wagemanager.domain.worker.service;

import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.worker.dto.WorkerDto;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.worker.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkerService {

    private final WorkerRepository workerRepository;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();

    public WorkerDto.Response getWorkerById(Long workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("근로자를 찾을 수 없습니다."));
        return WorkerDto.Response.from(worker);
    }

    public WorkerDto.Response getWorkerByUserId(Long userId) {
        Worker worker = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("근로자 정보를 찾을 수 없습니다."));
        return WorkerDto.Response.from(worker);
    }

    public WorkerDto.Response getWorkerByWorkerCode(String workerCode) {
        Worker worker = workerRepository.findByWorkerCode(workerCode)
                .orElseThrow(() -> new IllegalArgumentException("근로자를 찾을 수 없습니다."));
        return WorkerDto.Response.from(worker);
    }

    @Transactional
    public WorkerDto.Response updateWorker(Long workerId, WorkerDto.UpdateRequest request) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("근로자를 찾을 수 없습니다."));

        worker.updateKakaoPayLink(request.getKakaoPayLink());

        return WorkerDto.Response.from(worker);
    }

    @Transactional
    public WorkerDto.Response updateWorkerByUserId(Long userId, WorkerDto.UpdateRequest request) {
        Worker worker = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("근로자 정보를 찾을 수 없습니다."));

        worker.updateKakaoPayLink(request.getKakaoPayLink());

        return WorkerDto.Response.from(worker);
    }

    @Transactional
    public Worker createWorker(User user) {
        String workerCode = generateUniqueWorkerCode();

        Worker worker = Worker.builder()
                .user(user)
                .workerCode(workerCode)
                .build();

        return workerRepository.save(worker);
    }

    private String generateUniqueWorkerCode() {
        String code;
        do {
            code = generateRandomCode();
        } while (workerRepository.existsByWorkerCode(code));
        return code;
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}
