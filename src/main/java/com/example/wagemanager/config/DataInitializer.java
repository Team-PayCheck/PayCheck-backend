package com.example.wagemanager.config;

import com.example.wagemanager.domain.employer.entity.Employer;
import com.example.wagemanager.domain.employer.repository.EmployerRepository;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.user.enums.UserType;
import com.example.wagemanager.domain.user.repository.UserRepository;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.worker.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개발 환경 초기 데이터 생성
 * - 테스트용 고용주, 근로자 등을 자동 생성
 */
@Slf4j
@Component
@Profile({"local", "dev"}) // local, dev 프로파일에서만 실행
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;
    private final WorkerRepository workerRepository;

    @Override
    public void run(String... args) {
        log.info("=== 개발 환경 초기 데이터 생성 시작 ===");

        // 이미 데이터가 있으면 스킵
        if (userRepository.count() > 0) {
            log.info("이미 데이터가 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        createTestEmployer();
        createTestWorkers();

        log.info("=== 개발 환경 초기 데이터 생성 완료 ===");
    }

    /**
     * 테스트 고용주 생성
     */
    private void createTestEmployer() {
        // 고용주 User 생성
        User employerUser = User.builder()
                .kakaoId("dev_1") // devLogin과 동일한 kakaoId 사용
                .name("박지성")
                .phone("010-1234-5678")
                .userType(UserType.EMPLOYER)
                .profileImageUrl("")
                .build();
        employerUser = userRepository.save(employerUser);
        log.info("테스트 고용주 User 생성: {}", employerUser.getName());

        // Employer 엔티티 생성
        Employer employer = Employer.builder()
                .user(employerUser)
                .phone("010-1234-5678")
                .build();
        employerRepository.save(employer);
        log.info("테스트 Employer 생성 완료");
    }

    /**
     * 테스트 근로자 생성
     */
    private void createTestWorkers() {
        // 근로자 1
        User worker1User = User.builder()
                .kakaoId("dev_2")
                .name("김민준")
                .phone("010-1111-1111")
                .userType(UserType.WORKER)
                .profileImageUrl("")
                .build();
        worker1User = userRepository.save(worker1User);
        log.info("테스트 근로자1 User 생성: {}", worker1User.getName());

        createTestWorker("dev_2", "김민준", "010-1111-1111", "WK001", "333311110001");
        createTestWorker("dev_3", "이서연", "010-2222-2222", "WK002", "333311110002");
        createTestWorker("dev_4", "박지훈", "010-3333-3333", "WK003", "333311110003");
        createTestWorker("dev_5", "정수빈", "010-4444-4444", "WK004", "333311110004");
        createTestWorker("dev_6", "최유진", "010-5555-5555", "WK005", "333311110005");
    }

    private void createTestWorker(String kakaoId, String name, String phone, String workerCode, String accountNumber) {
        User workerUser = User.builder()
                .kakaoId(kakaoId)
                .name(name)
                .phone(phone)
                .userType(UserType.WORKER)
                .profileImageUrl("")
                .build();
        workerUser = userRepository.save(workerUser);
        log.info("테스트 근로자 User 생성: {}", workerUser.getName());

        Worker worker = Worker.builder()
                .user(workerUser)
                .workerCode(workerCode)
                .bankName("카카오뱅크")
                .accountNumber(accountNumber)
                .build();
        workerRepository.save(worker);
        log.info("테스트 Worker 생성 완료 (코드: {})", worker.getWorkerCode());
    }
}
