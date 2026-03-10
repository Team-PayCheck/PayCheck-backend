package com.example.paycheck.domain.salary.repository;

import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.salary.entity.Salary;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.workplace.entity.Workplace;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({EncryptionConfig.class, AccountNumberEncryptor.class})
@DisplayName("SalaryRepository 통합 테스트")
class SalaryRepositoryTest {

    @Autowired
    private SalaryRepository salaryRepository;

    @Autowired
    private EntityManager entityManager;

    private Workplace workplace;
    private WorkerContract contract1;
    private WorkerContract contract2;

    @BeforeEach
    void setUp() {
        // 고용주 User
        User employerUser = User.builder()
                .kakaoId("employer-kakao-sal-001")
                .name("사장님")
                .userType(UserType.EMPLOYER)
                .build();
        entityManager.persist(employerUser);

        // 근로자 User 1
        User workerUser1 = User.builder()
                .kakaoId("worker-kakao-sal-001")
                .name("알바생1")
                .userType(UserType.WORKER)
                .build();
        entityManager.persist(workerUser1);

        // 근로자 User 2
        User workerUser2 = User.builder()
                .kakaoId("worker-kakao-sal-002")
                .name("알바생2")
                .userType(UserType.WORKER)
                .build();
        entityManager.persist(workerUser2);

        // Employer
        Employer employer = Employer.builder()
                .user(employerUser)
                .phone("010-0000-0000")
                .build();
        entityManager.persist(employer);

        // Worker 1
        Worker worker1 = Worker.builder()
                .user(workerUser1)
                .workerCode("WRK001")
                .build();
        entityManager.persist(worker1);

        // Worker 2
        Worker worker2 = Worker.builder()
                .user(workerUser2)
                .workerCode("WRK002")
                .build();
        entityManager.persist(worker2);

        // Workplace
        workplace = Workplace.builder()
                .employer(employer)
                .businessNumber("999-88-77777")
                .name("급여 테스트 사업장")
                .address("서울시 종로구")
                .build();
        entityManager.persist(workplace);

        // Contract 1
        contract1 = WorkerContract.builder()
                .workplace(workplace)
                .worker(worker1)
                .hourlyWage(BigDecimal.valueOf(10000))
                .workSchedules("[{\"dayOfWeek\":1,\"startTime\":\"09:00\",\"endTime\":\"18:00\"}]")
                .contractStartDate(LocalDate.of(2026, 1, 1))
                .paymentDay(15)
                .build();
        entityManager.persist(contract1);

        // Contract 2
        contract2 = WorkerContract.builder()
                .workplace(workplace)
                .worker(worker2)
                .hourlyWage(BigDecimal.valueOf(12000))
                .workSchedules("[{\"dayOfWeek\":2,\"startTime\":\"10:00\",\"endTime\":\"19:00\"}]")
                .contractStartDate(LocalDate.of(2026, 1, 1))
                .paymentDay(15)
                .build();
        entityManager.persist(contract2);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByWorkplaceId")
    class FindByWorkplaceId {

        @Test
        @DisplayName("사업장 ID로 급여 목록을 조인된 엔티티와 함께 반환한다")
        void returnsSalariesWithJoinedEntities() {
            // given
            Salary salary1 = Salary.builder()
                    .contract(contract1)
                    .year(2026)
                    .month(3)
                    .totalWorkHours(BigDecimal.valueOf(160))
                    .basePay(BigDecimal.valueOf(1600000))
                    .overtimePay(BigDecimal.ZERO)
                    .nightPay(BigDecimal.ZERO)
                    .holidayPay(BigDecimal.ZERO)
                    .weeklyPaidLeaveAmount(BigDecimal.ZERO)
                    .totalGrossPay(BigDecimal.valueOf(1600000))
                    .totalDeduction(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(1600000))
                    .paymentDueDate(LocalDate.of(2026, 3, 15))
                    .build();

            Salary salary2 = Salary.builder()
                    .contract(contract2)
                    .year(2026)
                    .month(3)
                    .totalWorkHours(BigDecimal.valueOf(120))
                    .basePay(BigDecimal.valueOf(1440000))
                    .overtimePay(BigDecimal.ZERO)
                    .nightPay(BigDecimal.ZERO)
                    .holidayPay(BigDecimal.ZERO)
                    .weeklyPaidLeaveAmount(BigDecimal.ZERO)
                    .totalGrossPay(BigDecimal.valueOf(1440000))
                    .totalDeduction(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(1440000))
                    .paymentDueDate(LocalDate.of(2026, 3, 15))
                    .build();

            entityManager.persist(salary1);
            entityManager.persist(salary2);
            entityManager.flush();
            entityManager.clear();

            // when
            List<Salary> results = salaryRepository.findByWorkplaceId(workplace.getId());

            // then
            assertThat(results).hasSize(2);
            // JOIN FETCH로 연관 엔티티 접근 가능 확인
            for (Salary salary : results) {
                assertThat(salary.getContract()).isNotNull();
                assertThat(salary.getContract().getWorkplace()).isNotNull();
                assertThat(salary.getContract().getWorker()).isNotNull();
                assertThat(salary.getContract().getWorker().getUser()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("findByWorkplaceIdAndYearAndMonth")
    class FindByWorkplaceIdAndYearAndMonth {

        @Test
        @DisplayName("사업장 ID, 연도, 월로 급여를 필터링하여 반환한다")
        void filtersCorrectly() {
            // given
            Salary marchSalary = Salary.builder()
                    .contract(contract1)
                    .year(2026)
                    .month(3)
                    .totalWorkHours(BigDecimal.valueOf(160))
                    .basePay(BigDecimal.valueOf(1600000))
                    .overtimePay(BigDecimal.ZERO)
                    .nightPay(BigDecimal.ZERO)
                    .holidayPay(BigDecimal.ZERO)
                    .weeklyPaidLeaveAmount(BigDecimal.ZERO)
                    .totalGrossPay(BigDecimal.valueOf(1600000))
                    .totalDeduction(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(1600000))
                    .paymentDueDate(LocalDate.of(2026, 3, 15))
                    .build();

            Salary aprilSalary = Salary.builder()
                    .contract(contract1)
                    .year(2026)
                    .month(4)
                    .totalWorkHours(BigDecimal.valueOf(160))
                    .basePay(BigDecimal.valueOf(1600000))
                    .overtimePay(BigDecimal.ZERO)
                    .nightPay(BigDecimal.ZERO)
                    .holidayPay(BigDecimal.ZERO)
                    .weeklyPaidLeaveAmount(BigDecimal.ZERO)
                    .totalGrossPay(BigDecimal.valueOf(1600000))
                    .totalDeduction(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(1600000))
                    .paymentDueDate(LocalDate.of(2026, 4, 15))
                    .build();

            entityManager.persist(marchSalary);
            entityManager.persist(aprilSalary);
            entityManager.flush();
            entityManager.clear();

            // when
            List<Salary> results = salaryRepository.findByWorkplaceIdAndYearAndMonth(
                    workplace.getId(), 2026, 3);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMonth()).isEqualTo(3);
            assertThat(results.get(0).getYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("일치하는 급여가 없으면 빈 리스트를 반환한다")
        void returnsEmptyWhenNoMatch() {
            // when
            List<Salary> results = salaryRepository.findByWorkplaceIdAndYearAndMonth(
                    workplace.getId(), 2026, 12);

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByContractIdAndYearAndMonth")
    class FindByContractIdAndYearAndMonth {

        @Test
        @DisplayName("계약 ID, 연도, 월로 일치하는 급여를 반환한다")
        void returnsMatchingSalary() {
            // given
            Salary salary = Salary.builder()
                    .contract(contract1)
                    .year(2026)
                    .month(2)
                    .totalWorkHours(BigDecimal.valueOf(140))
                    .basePay(BigDecimal.valueOf(1400000))
                    .overtimePay(BigDecimal.ZERO)
                    .nightPay(BigDecimal.ZERO)
                    .holidayPay(BigDecimal.ZERO)
                    .weeklyPaidLeaveAmount(BigDecimal.ZERO)
                    .totalGrossPay(BigDecimal.valueOf(1400000))
                    .totalDeduction(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(1400000))
                    .paymentDueDate(LocalDate.of(2026, 2, 15))
                    .build();
            entityManager.persist(salary);
            entityManager.flush();
            entityManager.clear();

            // when
            List<Salary> results = salaryRepository.findByContractIdAndYearAndMonth(
                    contract1.getId(), 2026, 2);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getContract().getId()).isEqualTo(contract1.getId());
            assertThat(results.get(0).getYear()).isEqualTo(2026);
            assertThat(results.get(0).getMonth()).isEqualTo(2);
        }

        @Test
        @DisplayName("다른 계약의 급여는 반환하지 않는다")
        void doesNotReturnOtherContractSalaries() {
            // given
            Salary salary1 = Salary.builder()
                    .contract(contract1)
                    .year(2026)
                    .month(3)
                    .totalWorkHours(BigDecimal.valueOf(160))
                    .basePay(BigDecimal.valueOf(1600000))
                    .overtimePay(BigDecimal.ZERO)
                    .nightPay(BigDecimal.ZERO)
                    .holidayPay(BigDecimal.ZERO)
                    .weeklyPaidLeaveAmount(BigDecimal.ZERO)
                    .totalGrossPay(BigDecimal.valueOf(1600000))
                    .totalDeduction(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(1600000))
                    .paymentDueDate(LocalDate.of(2026, 3, 15))
                    .build();

            Salary salary2 = Salary.builder()
                    .contract(contract2)
                    .year(2026)
                    .month(3)
                    .totalWorkHours(BigDecimal.valueOf(120))
                    .basePay(BigDecimal.valueOf(1440000))
                    .overtimePay(BigDecimal.ZERO)
                    .nightPay(BigDecimal.ZERO)
                    .holidayPay(BigDecimal.ZERO)
                    .weeklyPaidLeaveAmount(BigDecimal.ZERO)
                    .totalGrossPay(BigDecimal.valueOf(1440000))
                    .totalDeduction(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(1440000))
                    .paymentDueDate(LocalDate.of(2026, 3, 15))
                    .build();

            entityManager.persist(salary1);
            entityManager.persist(salary2);
            entityManager.flush();
            entityManager.clear();

            // when
            List<Salary> results = salaryRepository.findByContractIdAndYearAndMonth(
                    contract1.getId(), 2026, 3);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getContract().getId()).isEqualTo(contract1.getId());
        }
    }
}
