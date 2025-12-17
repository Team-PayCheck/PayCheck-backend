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
                .kakaoId("dev_9999") // devLogin과 동일한 kakaoId 사용
                .name("테스트 고용주")
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
                .kakaoId("dev_8888")
                .name("테스트 근로자")
                .phone("010-9876-5432")
                .userType(UserType.WORKER)
                .profileImageUrl("")
                .build();
        worker1User = userRepository.save(worker1User);
        log.info("테스트 근로자1 User 생성: {}", worker1User.getName());

        Worker worker1 = Worker.builder()
                .user(worker1User)
                .workerCode("DEV888") // 고유 코드
                .kakaoPayLink("https://qr.kakaopay.com/dev_test_worker1")
                .build();
        workerRepository.save(worker1);
        log.info("테스트 Worker1 생성 완료 (코드: {})", worker1.getWorkerCode());

        // 근로자 2
        User worker2User = User.builder()
                .kakaoId("dev_7777")
                .name("김철수")
                .phone("010-1111-2222")
                .userType(UserType.WORKER)
                .profileImageUrl("")
                .build();
        worker2User = userRepository.save(worker2User);
        log.info("테스트 근로자2 User 생성: {}", worker2User.getName());

        Worker worker2 = Worker.builder()
                .user(worker2User)
                .workerCode("DEV777")
                .kakaoPayLink("https://qr.kakaopay.com/dev_test_worker2")
                .build();
        workerRepository.save(worker2);
        log.info("테스트 Worker2 생성 완료 (코드: {})", worker2.getWorkerCode());

        // 근로자 3
        User worker3User = User.builder()
                .kakaoId("dev_6666")
                .name("이영희")
                .phone("010-3333-4444")
                .userType(UserType.WORKER)
                .profileImageUrl("")
                .build();
        worker3User = userRepository.save(worker3User);
        log.info("테스트 근로자3 User 생성: {}", worker3User.getName());

        Worker worker3 = Worker.builder()
                .user(worker3User)
                .workerCode("DEV666")
                .kakaoPayLink("https://qr.kakaopay.com/dev_test_worker3")
                .build();
        workerRepository.save(worker3);
        log.info("테스트 Worker3 생성 완료 (코드: {})", worker3.getWorkerCode());
    }
}
