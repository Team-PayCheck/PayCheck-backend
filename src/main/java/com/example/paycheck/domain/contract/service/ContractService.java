package com.example.paycheck.domain.contract.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.contract.dto.ContractDto;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.notification.event.NotificationEvent;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import com.example.paycheck.domain.workrecord.service.WorkRecordGenerationService;
import com.example.paycheck.domain.workrecord.service.WorkRecordCommandService;
import com.example.paycheck.domain.contract.dto.WorkScheduleDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final WorkerContractRepository contractRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkerRepository workerRepository;
    private final WorkRecordGenerationService workRecordGenerationService;
    private final WorkRecordCommandService workRecordCommandService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ContractDto.Response addWorkerToWorkplace(Long workplaceId, ContractDto.CreateRequest request) {
        // 사업장 조회
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKPLACE_NOT_FOUND, "사업장을 찾을 수 없습니다."));

        // Worker 코드로 근로자 조회
        Worker worker = workerRepository.findByWorkerCode(request.getWorkerCode())
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKER_NOT_FOUND, "근로자 코드를 찾을 수 없습니다: " + request.getWorkerCode()));

        // 이미 계약이 존재하는지 확인 (활성 상태인 계약)
        List<WorkerContract> existingContracts = contractRepository.findByWorkplaceIdAndIsActive(workplaceId, true);
        boolean alreadyContracted = existingContracts.stream()
                .anyMatch(contract -> contract.getWorker().getId().equals(worker.getId()));

        if (alreadyContracted) {
            throw new BadRequestException(ErrorCode.DUPLICATE_CONTRACT, "이미 해당 사업장에 계약이 존재하는 근로자입니다.");
        }

        // 계약 생성
        WorkerContract contract = WorkerContract.builder()
                .workplace(workplace)
                .worker(worker)
                .hourlyWage(request.getHourlyWage())
                .workSchedules(convertWorkSchedulesToJson(request.getWorkSchedules()))
                .contractStartDate(request.getContractStartDate())
                .contractEndDate(request.getContractEndDate())
                .paymentDay(request.getPaymentDay())
                .payrollDeductionType(request.getPayrollDeductionType())
                .isActive(true)
                .build();

        WorkerContract savedContract = contractRepository.save(contract);

        // 2개월치 WorkRecord 자동 생성
        workRecordGenerationService.generateInitialWorkRecords(savedContract);

        publishInvitationEvent(savedContract);

        return ContractDto.Response.from(savedContract);
    }

    public List<ContractDto.ListResponse> getContractsByWorkplaceId(Long workplaceId) {
        List<WorkerContract> contracts = contractRepository.findByWorkplaceIdAndIsActive(workplaceId, true);
        return contracts.stream()
                .map(ContractDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    public List<ContractDto.ListResponse> getContractsByUserId(Long userId) {
        Worker worker = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKER_NOT_FOUND, "근로자 정보를 찾을 수 없습니다."));

        List<WorkerContract> contracts = contractRepository.findByWorkerId(worker.getId());
        return contracts.stream()
                .filter(contract -> contract.getIsActive())
                .map(ContractDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    public ContractDto.Response getContractById(Long contractId) {
        WorkerContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));
        return ContractDto.Response.from(contract);
    }

    @Transactional
    public ContractDto.Response updateContract(Long contractId, ContractDto.UpdateRequest request) {
        WorkerContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        String workSchedulesJson = request.getWorkSchedules() != null
                ? convertWorkSchedulesToJson(request.getWorkSchedules())
                : null;

        // 근무 스케줄 변경 여부 확인
        boolean workScheduleChanged = workSchedulesJson != null
                && !workSchedulesJson.equals(contract.getWorkSchedules());

        contract.update(
                request.getHourlyWage(),
                workSchedulesJson,
                request.getContractEndDate(),
                request.getPaymentDay(),
                request.getPayrollDeductionType()
        );

        // 근무 스케줄이 변경된 경우 미래 WorkRecord 재생성
        if (workScheduleChanged) {
            workRecordCommandService.regenerateFutureWorkRecords(contractId);
        }

        return ContractDto.Response.from(contract);
    }

    @Transactional
    public void terminateContract(Long contractId) {
        WorkerContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        contract.terminate();

        // 미래 예정된 근무 기록 일괄 soft-delete
        workRecordCommandService.deleteFutureWorkRecords(contractId);

        publishResignationEvent(contract);
    }

    private String convertWorkSchedulesToJson(List<WorkScheduleDto> workSchedules) {
        try {
            return objectMapper.writeValueAsString(workSchedules);
        } catch (JsonProcessingException e) {
            throw new BadRequestException(ErrorCode.WORK_DAY_CONVERSION_ERROR, "근무 스케줄 변환 중 오류가 발생했습니다.");
        }
    }

    private void publishInvitationEvent(WorkerContract contract) {
        User workerUser = contract.getWorker().getUser();
        String title = String.format("%s 사업장 초대가 도착했습니다.", contract.getWorkplace().getName());

        NotificationEvent event = NotificationEvent.builder()
                .user(workerUser)
                .type(NotificationType.INVITATION)
                .title(title)
                .actionType(NotificationActionType.VIEW_WORKPLACE_INVITATION)
                .actionData(buildActionData(contract.getId(), contract.getWorkplace().getId()))
                .build();

        eventPublisher.publishEvent(event);
    }

    private void publishResignationEvent(WorkerContract contract) {
        User workerUser = contract.getWorker().getUser();
        User employerUser = contract.getWorkplace().getEmployer().getUser();
        String title = String.format("%s 근무가 종료되었습니다.", contract.getWorkplace().getName());

        NotificationEvent workerEvent = NotificationEvent.builder()
                .user(workerUser)
                .type(NotificationType.RESIGNATION)
                .title(title)
                .actionType(NotificationActionType.NONE)
                .actionData(buildActionData(contract.getId(), contract.getWorkplace().getId()))
                .build();

        NotificationEvent employerEvent = NotificationEvent.builder()
                .user(employerUser)
                .type(NotificationType.RESIGNATION)
                .title(title)
                .actionType(NotificationActionType.NONE)
                .actionData(buildActionData(contract.getId(), contract.getWorkplace().getId()))
                .build();

        eventPublisher.publishEvent(workerEvent);
        eventPublisher.publishEvent(employerEvent);
    }

    private String buildActionData(Long contractId, Long workplaceId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("contractId", contractId);
            data.put("workplaceId", workplaceId);
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("알림 액션 데이터 생성 실패: contractId={}, workplaceId={}", contractId, workplaceId, e);
            return null;
        }
    }
}
