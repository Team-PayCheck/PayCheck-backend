package com.example.paycheck.global.security.permission;

import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.correction.repository.CorrectionRequestRepository;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.payment.repository.PaymentRepository;
import com.example.paycheck.domain.salary.repository.SalaryRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomPermissionEvaluator 테스트")
class CustomPermissionEvaluatorTest {

    @Mock
    private WorkerContractRepository contractRepository;
    @Mock
    private WorkplaceRepository workplaceRepository;
    @Mock
    private WorkRecordRepository workRecordRepository;
    @Mock
    private SalaryRepository salaryRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CorrectionRequestRepository correctionRequestRepository;
    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private CustomPermissionEvaluator permissionEvaluator;

    private User employerUser;
    private User workerUser;

    @BeforeEach
    void setUp() {
        employerUser = User.builder()
                .id(1L)
                .kakaoId("employer123")
                .name("고용주")
                .userType(UserType.EMPLOYER)
                .build();

        workerUser = User.builder()
                .id(2L)
                .kakaoId("worker123")
                .name("근로자")
                .userType(UserType.WORKER)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));
    }

    @Nested
    @DisplayName("역할 검증")
    class RoleCheckTest {

        @Test
        @DisplayName("isEmployer - 고용주면 true 반환")
        void isEmployer_EmployerUser_ReturnsTrue() {
            setSecurityContext(employerUser);
            assertThat(permissionEvaluator.isEmployer()).isTrue();
        }

        @Test
        @DisplayName("isEmployer - 근로자면 false 반환")
        void isEmployer_WorkerUser_ReturnsFalse() {
            setSecurityContext(workerUser);
            assertThat(permissionEvaluator.isEmployer()).isFalse();
        }

        @Test
        @DisplayName("isWorker - 근로자면 true 반환")
        void isWorker_WorkerUser_ReturnsTrue() {
            setSecurityContext(workerUser);
            assertThat(permissionEvaluator.isWorker()).isTrue();
        }

        @Test
        @DisplayName("isWorker - 고용주면 false 반환")
        void isWorker_EmployerUser_ReturnsFalse() {
            setSecurityContext(employerUser);
            assertThat(permissionEvaluator.isWorker()).isFalse();
        }

        @Test
        @DisplayName("인증 정보가 없으면 false 반환")
        void noAuthentication_ReturnsFalse() {
            SecurityContextHolder.clearContext();
            assertThat(permissionEvaluator.isEmployer()).isFalse();
            assertThat(permissionEvaluator.isWorker()).isFalse();
        }
    }

    @Nested
    @DisplayName("USER 권한 검증")
    class UserPermissionTest {

        @Test
        @DisplayName("본인 ID로 접근하면 true 반환")
        void canAccessUser_SameUser_ReturnsTrue() {
            setSecurityContext(employerUser);
            assertThat(permissionEvaluator.canAccessUser(1L)).isTrue();
        }

        @Test
        @DisplayName("다른 사용자 ID로 접근하면 false 반환")
        void canAccessUser_DifferentUser_ReturnsFalse() {
            setSecurityContext(employerUser);
            assertThat(permissionEvaluator.canAccessUser(999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("WORKPLACE 권한 검증")
    class WorkplacePermissionTest {

        @Test
        @DisplayName("사업장 소유자가 접근하면 true 반환")
        void canAccessWorkplace_Owner_ReturnsTrue() {
            setSecurityContext(employerUser);
            Employer employer = Employer.builder().id(1L).user(employerUser).build();
            Workplace workplace = Workplace.builder().id(1L).employer(employer).build();

            when(workplaceRepository.findById(1L)).thenReturn(Optional.of(workplace));

            assertThat(permissionEvaluator.canAccessWorkplace(1L)).isTrue();
        }

        @Test
        @DisplayName("사업장 비소유자가 접근하면 false 반환")
        void canAccessWorkplace_NotOwner_ReturnsFalse() {
            setSecurityContext(employerUser);
            User otherUser = User.builder().id(999L).build();
            Employer employer = Employer.builder().id(1L).user(otherUser).build();
            Workplace workplace = Workplace.builder().id(1L).employer(employer).build();

            when(workplaceRepository.findById(1L)).thenReturn(Optional.of(workplace));

            assertThat(permissionEvaluator.canAccessWorkplace(1L)).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 사업장에 접근하면 false 반환")
        void canAccessWorkplace_NotFound_ReturnsFalse() {
            setSecurityContext(employerUser);
            when(workplaceRepository.findById(999L)).thenReturn(Optional.empty());

            assertThat(permissionEvaluator.canAccessWorkplace(999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("CONTRACT 권한 검증")
    class ContractPermissionTest {

        @Test
        @DisplayName("고용주가 자신의 계약에 접근하면 true 반환")
        void canAccessContractAsEmployer_Owner_ReturnsTrue() {
            setSecurityContext(employerUser);
            Employer employer = Employer.builder().id(1L).user(employerUser).build();
            Workplace workplace = Workplace.builder().id(1L).employer(employer).build();
            WorkerContract contract = WorkerContract.builder().id(1L).workplace(workplace).build();

            when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

            assertThat(permissionEvaluator.canAccessContractAsEmployer(1L)).isTrue();
        }

        @Test
        @DisplayName("근로자가 자신의 계약에 접근하면 true 반환")
        void canAccessContractAsWorker_Owner_ReturnsTrue() {
            setSecurityContext(workerUser);
            Worker worker = Worker.builder().id(1L).user(workerUser).build();
            Employer employer = Employer.builder().id(1L).user(employerUser).build();
            Workplace workplace = Workplace.builder().id(1L).employer(employer).build();
            WorkerContract contract = WorkerContract.builder().id(1L).workplace(workplace).worker(worker).build();

            when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

            assertThat(permissionEvaluator.canAccessContractAsWorker(1L)).isTrue();
        }
    }

    @Nested
    @DisplayName("WORK_RECORD 권한 검증")
    class WorkRecordPermissionTest {

        @Test
        @DisplayName("고용주가 자신의 근무기록에 접근하면 true 반환")
        void canAccessWorkRecordAsEmployer_Owner_ReturnsTrue() {
            setSecurityContext(employerUser);
            Employer employer = Employer.builder().id(1L).user(employerUser).build();
            Workplace workplace = Workplace.builder().id(1L).employer(employer).build();
            WorkerContract contract = WorkerContract.builder().id(1L).workplace(workplace).build();
            WorkRecord workRecord = WorkRecord.builder().id(1L).contract(contract).build();

            when(workRecordRepository.findById(1L)).thenReturn(Optional.of(workRecord));

            assertThat(permissionEvaluator.canAccessWorkRecordAsEmployer(1L)).isTrue();
        }

        @Test
        @DisplayName("근로자가 자신의 근무기록에 접근하면 true 반환")
        void canAccessWorkRecordAsWorker_Owner_ReturnsTrue() {
            setSecurityContext(workerUser);
            Worker worker = Worker.builder().id(1L).user(workerUser).build();
            Employer employer = Employer.builder().id(1L).user(employerUser).build();
            Workplace workplace = Workplace.builder().id(1L).employer(employer).build();
            WorkerContract contract = WorkerContract.builder().id(1L).workplace(workplace).worker(worker).build();
            WorkRecord workRecord = WorkRecord.builder().id(1L).contract(contract).build();

            when(workRecordRepository.findById(1L)).thenReturn(Optional.of(workRecord));

            assertThat(permissionEvaluator.canAccessWorkRecordAsWorker(1L)).isTrue();
        }
    }

    @Nested
    @DisplayName("WORKER 권한 검증")
    class WorkerPermissionTest {

        @Test
        @DisplayName("본인 근로자 정보에 접근하면 true 반환")
        void canAccessWorker_Owner_ReturnsTrue() {
            setSecurityContext(workerUser);
            Worker worker = Worker.builder().id(1L).user(workerUser).build();

            when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

            assertThat(permissionEvaluator.canAccessWorker(1L)).isTrue();
        }

        @Test
        @DisplayName("타인 근로자 정보에 접근하면 false 반환")
        void canAccessWorker_NotOwner_ReturnsFalse() {
            setSecurityContext(workerUser);
            User otherUser = User.builder().id(999L).build();
            Worker worker = Worker.builder().id(1L).user(otherUser).build();

            when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));

            assertThat(permissionEvaluator.canAccessWorker(1L)).isFalse();
        }
    }
}
