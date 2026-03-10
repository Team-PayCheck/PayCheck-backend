package com.example.paycheck.domain.workrecord.repository;

import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.global.config.EncryptionConfig;
import com.example.paycheck.global.encryption.AccountNumberEncryptor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({EncryptionConfig.class, AccountNumberEncryptor.class})
@DisplayName("WorkRecordRepository 통합 테스트")
class WorkRecordRepositoryTest {

    @Autowired
    private WorkRecordRepository workRecordRepository;

    @Autowired
    private EntityManager entityManager;

    private WorkerContract contract;
    private Workplace workplace;

    @BeforeEach
    void setUp() {
        // User (고용주)
        User employerUser = User.builder()
                .kakaoId("employer-kakao-001")
                .name("고용주")
                .userType(UserType.EMPLOYER)
                .build();
        entityManager.persist(employerUser);

        // User (근로자)
        User workerUser = User.builder()
                .kakaoId("worker-kakao-001")
                .name("근로자")
                .userType(UserType.WORKER)
                .build();
        entityManager.persist(workerUser);

        // Employer
        Employer employer = Employer.builder()
                .user(employerUser)
                .phone("010-1234-5678")
                .build();
        entityManager.persist(employer);

        // Worker
        Worker worker = Worker.builder()
                .user(workerUser)
                .workerCode("ABC123")
                .build();
        entityManager.persist(worker);

        // Workplace
        workplace = Workplace.builder()
                .employer(employer)
                .businessNumber("123-45-67890")
                .name("테스트 사업장")
                .address("서울시 강남구")
                .build();
        entityManager.persist(workplace);

        // WorkerContract
        contract = WorkerContract.builder()
                .workplace(workplace)
                .worker(worker)
                .hourlyWage(BigDecimal.valueOf(10000))
                .workSchedules("[{\"dayOfWeek\":1,\"startTime\":\"09:00\",\"endTime\":\"18:00\"}]")
                .contractStartDate(LocalDate.of(2026, 1, 1))
                .paymentDay(15)
                .build();
        entityManager.persist(contract);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByContractAndDateRange")
    class FindByContractAndDateRange {

        @Test
        @DisplayName("날짜 범위 내의 DELETED가 아닌 WorkRecord를 반환한다")
        void returnsRecordsInDateRangeExcludingDeleted() {
            // given
            WorkRecord completedRecord = WorkRecord.builder()
                    .contract(contract)
                    .workDate(LocalDate.of(2026, 3, 2))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .status(WorkRecordStatus.COMPLETED)
                    .build();

            WorkRecord scheduledRecord = WorkRecord.builder()
                    .contract(contract)
                    .workDate(LocalDate.of(2026, 3, 5))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .status(WorkRecordStatus.SCHEDULED)
                    .build();

            WorkRecord deletedRecord = WorkRecord.builder()
                    .contract(contract)
                    .workDate(LocalDate.of(2026, 3, 3))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .status(WorkRecordStatus.DELETED)
                    .build();

            // 범위 밖의 레코드
            WorkRecord outOfRangeRecord = WorkRecord.builder()
                    .contract(contract)
                    .workDate(LocalDate.of(2026, 4, 1))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .status(WorkRecordStatus.COMPLETED)
                    .build();

            entityManager.persist(completedRecord);
            entityManager.persist(scheduledRecord);
            entityManager.persist(deletedRecord);
            entityManager.persist(outOfRangeRecord);
            entityManager.flush();
            entityManager.clear();

            // when
            List<WorkRecord> results = workRecordRepository.findByContractAndDateRange(
                    contract.getId(),
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31),
                    WorkRecordStatus.DELETED
            );

            // then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WorkRecord::getWorkDate)
                    .containsExactly(
                            LocalDate.of(2026, 3, 2),
                            LocalDate.of(2026, 3, 5)
                    );
            assertThat(results).noneMatch(r -> r.getStatus() == WorkRecordStatus.DELETED);
        }

        @Test
        @DisplayName("일치하는 레코드가 없으면 빈 리스트를 반환한다")
        void returnsEmptyForNoMatchingRecords() {
            // given - 레코드 없음

            // when
            List<WorkRecord> results = workRecordRepository.findByContractAndDateRange(
                    contract.getId(),
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30),
                    WorkRecordStatus.DELETED
            );

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByContractAndWorkDate")
    class ExistsByContractAndWorkDate {

        @Test
        @DisplayName("해당 날짜에 WorkRecord가 존재하면 true를 반환한다")
        void returnsTrueWhenExists() {
            // given
            WorkRecord record = WorkRecord.builder()
                    .contract(contract)
                    .workDate(LocalDate.of(2026, 3, 9))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .status(WorkRecordStatus.SCHEDULED)
                    .build();
            entityManager.persist(record);
            entityManager.flush();
            entityManager.clear();

            // when
            boolean exists = workRecordRepository.existsByContractAndWorkDate(
                    contract, LocalDate.of(2026, 3, 9));

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("해당 날짜에 WorkRecord가 존재하지 않으면 false를 반환한다")
        void returnsFalseWhenNotExists() {
            // when
            boolean exists = workRecordRepository.existsByContractAndWorkDate(
                    contract, LocalDate.of(2026, 3, 15));

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByWorkplaceAndDateRange")
    class FindByWorkplaceAndDateRange {

        @Test
        @DisplayName("사업장별 날짜 범위 내의 WorkRecord를 날짜 오름차순으로 반환한다")
        void returnsRecordsByWorkplaceOrderedByDate() {
            // given
            WorkRecord record1 = WorkRecord.builder()
                    .contract(contract)
                    .workDate(LocalDate.of(2026, 3, 10))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .status(WorkRecordStatus.COMPLETED)
                    .build();

            WorkRecord record2 = WorkRecord.builder()
                    .contract(contract)
                    .workDate(LocalDate.of(2026, 3, 5))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .status(WorkRecordStatus.COMPLETED)
                    .build();

            entityManager.persist(record1);
            entityManager.persist(record2);
            entityManager.flush();
            entityManager.clear();

            // when
            List<WorkRecord> results = workRecordRepository.findByWorkplaceAndDateRange(
                    workplace.getId(),
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31),
                    WorkRecordStatus.DELETED
            );

            // then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getWorkDate()).isBefore(results.get(1).getWorkDate());
        }
    }
}
