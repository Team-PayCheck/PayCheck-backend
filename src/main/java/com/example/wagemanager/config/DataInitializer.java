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

        Worker worker1 = Worker.builder()
                .user(worker1User)
                .workerCode("WK001")
                .bankName("카카오뱅크")
                .accountNumber("333311110001")
                .tossDeepLink("toss://send?bank=088&account=000000000001&amount=0")
                .build();
        workerRepository.save(worker1);
        log.info("테스트 Worker1 생성 완료 (코드: {})", worker1.getWorkerCode());

        // 근로자 2
        User worker2User = User.builder()
                .kakaoId("dev_3")
                .name("이서연")
                .phone("010-2222-2222")
                .userType(UserType.WORKER)
                .profileImageUrl("")
                .build();
        worker2User = userRepository.save(worker2User);
        log.info("테스트 근로자2 User 생성: {}", worker2User.getName());

        Worker worker2 = Worker.builder()
                .user(worker2User)
                .workerCode("WK002")
                .bankName("카카오뱅크")
                .accountNumber("333311110002")
                .tossDeepLink("toss://send?bank=088&account=000000000002&amount=0")
                .build();
        workerRepository.save(worker2);
        log.info("테스트 Worker2 생성 완료 (코드: {})", worker2.getWorkerCode());

        // 근로자 3
        User worker3User = User.builder()
                .kakaoId("dev_4")
                .name("박지훈")
                .phone("010-3333-3333")
                .userType(UserType.WORKER)
                .profileImageUrl("")
                .build();
        worker3User = userRepository.save(worker3User);
        log.info("테스트 근로자3 User 생성: {}", worker3User.getName());

        Worker worker3 = Worker.builder()
                .user(worker3User)
                .workerCode("WK003")
                .bankName("카카오뱅크")
                .accountNumber("333311110003")
                .tossDeepLink("toss://send?bank=088&account=000000000003&amount=0")
                .build();
        workerRepository.save(worker3);
        log.info("테스트 Worker3 생성 완료 (코드: {})", worker3.getWorkerCode());

        // 근로자 4
        User worker4User = User.builder()
                .kakaoId("dev_5")
                .name("정수빈")
                .phone("010-4444-4444")
                .userType(UserType.WORKER)
                .profileImageUrl("")
                .build();
        worker4User = userRepository.save(worker4User);
        log.info("테스트 근로자4 User 생성: {}", worker4User.getName());

        Worker worker4 = Worker.builder()
                .user(worker4User)
                .workerCode("WK004")
                .bankName("카카오뱅크")
                .accountNumber("333311110004")
                .tossDeepLink("toss://send?bank=088&account=000000000004&amount=0")
                .build();
        workerRepository.save(worker4);
        log.info("테스트 Worker4 생성 완료 (코드: {})", worker4.getWorkerCode());

        // 근로자 5
        User worker5User = User.builder()
                .kakaoId("dev_6")
                .name("최유진")
                .phone("010-5555-5555")
                .userType(UserType.WORKER)
                .profileImageUrl("")
                .build();
        worker5User = userRepository.save(worker5User);
        log.info("테스트 근로자5 User 생성: {}", worker5User.getName());

        Worker worker5 = Worker.builder()
                .user(worker5User)
                .workerCode("WK005")
                .bankName("카카오뱅크")
                .accountNumber("333311110005")
                .tossDeepLink("toss://send?bank=088&account=000000000005&amount=0")
                .build();
        workerRepository.save(worker5);
        log.info("테스트 Worker5 생성 완료 (코드: {})", worker5.getWorkerCode());
    }
}
