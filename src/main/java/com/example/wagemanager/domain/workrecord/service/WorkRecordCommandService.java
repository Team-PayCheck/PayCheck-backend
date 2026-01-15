package com.example.wagemanager.domain.workrecord.service;

import com.example.wagemanager.common.exception.BadRequestException;
import com.example.wagemanager.common.exception.ErrorCode;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.allowance.entity.WeeklyAllowance;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.contract.repository.WorkerContractRepository;
import com.example.wagemanager.domain.correction.repository.CorrectionRequestRepository;
import com.example.wagemanager.domain.notification.enums.NotificationActionType;
import com.example.wagemanager.domain.notification.enums.NotificationType;
import com.example.wagemanager.domain.notification.event.NotificationEvent;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workrecord.dto.WorkRecordDto;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import com.example.wagemanager.domain.workrecord.repository.WorkRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkRecordCommandService {

    private final WorkRecordRepository workRecordRepository;
    private final WorkerContractRepository workerContractRepository;
    private final CorrectionRequestRepository correctionRequestRepository;
    private final WorkRecordCoordinatorService coordinatorService;
    private final WorkRecordGenerationService workRecordGenerationService;
    private final WorkRecordCalculationService calculationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 고용주가 근무 일정 생성 (승인 불필요)
     * SCHEDULED 또는 COMPLETED 상태로 생성
     */
    public WorkRecordDto.Response createWorkRecordByEmployer(WorkRecordDto.CreateRequest request) {
        WorkerContract contract = workerContractRepository.findById(request.getContractId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        int totalMinutes = calculateWorkMinutes(
                LocalDateTime.of(request.getWorkDate(), request.getStartTime()),
                LocalDateTime.of(request.getWorkDate(), request.getEndTime()),
                request.getBreakMinutes() != null ? request.getBreakMinutes() : 0
        );

        // 근무 날짜와 현재 날짜를 비교하여 상태 결정
        // 과거 날짜면 COMPLETED, 미래 날짜면 SCHEDULED
        WorkRecordStatus status = request.getWorkDate().isBefore(LocalDate.now())
                ? WorkRecordStatus.COMPLETED
                : WorkRecordStatus.SCHEDULED;

        // WorkRecord가 생성된 주에 WeeklyAllowance 자동 생성/조회
        WeeklyAllowance weeklyAllowance = coordinatorService.getOrCreateWeeklyAllowance(
                contract.getId(), request.getWorkDate());

        WorkRecord workRecord = WorkRecord.builder()
                .contract(contract)
                .workDate(request.getWorkDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .breakMinutes(request.getBreakMinutes() != null ? request.getBreakMinutes() : 0)
                .totalWorkMinutes(totalMinutes)
                .status(status)
                .memo(request.getMemo())
                .weeklyAllowance(weeklyAllowance)
                .build();

        WorkRecord savedRecord = workRecordRepository.save(workRecord);

        // COMPLETED 상태면 정확한 휴일 정보와 사업장 규모를 반영하여 재계산
        if (status == WorkRecordStatus.COMPLETED) {
            calculationService.calculateWorkRecordDetails(savedRecord);
            workRecordRepository.save(savedRecord);
        }

        // 도메인 간 협력 처리
        if (status == WorkRecordStatus.COMPLETED) {
            // COMPLETED로 생성된 경우 급여 재계산 포함
            coordinatorService.handleWorkRecordCreation(savedRecord);
            coordinatorService.handleWorkRecordCompletion(savedRecord);
        } else {
            // SCHEDULED로 생성된 경우 WeeklyAllowance만 재계산
            coordinatorService.handleWorkRecordCreation(savedRecord);
        }

        // 근로자에게 일정 생성 알림 전송
        User worker = savedRecord.getContract().getWorker().getUser();
        String title = String.format("%s 근무 일정이 등록되었습니다.",
                request.getWorkDate().toString());

        NotificationEvent event = NotificationEvent.builder()
                .user(worker)
                .type(NotificationType.SCHEDULE_CREATED)
                .title(title)
                .actionType(NotificationActionType.VIEW_WORK_RECORD)
                .actionData(buildActionData(savedRecord.getId()))
                .build();

        eventPublisher.publishEvent(event);

        return WorkRecordDto.Response.from(savedRecord);
    }


    public WorkRecordDto.Response updateWorkRecord(Long workRecordId, WorkRecordDto.UpdateRequest request) {
        WorkRecord workRecord = workRecordRepository.findById(workRecordId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));

        // 알림 전송을 위해 원본 날짜 저장
        LocalDate originalWorkDate = workRecord.getWorkDate();

        if (request.getStartTime() != null || request.getEndTime() != null || request.getBreakMinutes() != null) {
            int totalMinutes = calculateWorkMinutes(
                    LocalDateTime.of(workRecord.getWorkDate(),
                            request.getStartTime() != null ? request.getStartTime() : workRecord.getStartTime()),
                    LocalDateTime.of(workRecord.getWorkDate(),
                            request.getEndTime() != null ? request.getEndTime() : workRecord.getEndTime()),
                    request.getBreakMinutes() != null ? request.getBreakMinutes() : workRecord.getBreakMinutes()
            );

            // 기존 WeeklyAllowance 저장 (나중에 재계산용)
            WeeklyAllowance oldWeeklyAllowance = workRecord.getWeeklyAllowance();

            // 기존 WeeklyAllowance에서 제거 (양방향 관계 해제)
            if (oldWeeklyAllowance != null) {
                workRecord.removeFromWeeklyAllowance();
            }

            // 현재 주에 맞는 WeeklyAllowance 조회/생성
            WeeklyAllowance newWeeklyAllowance = coordinatorService.getOrCreateWeeklyAllowance(
                    workRecord.getContract().getId(), workRecord.getWorkDate());

            // 새로운 WeeklyAllowance에 할당 (양방향 관계 설정)
            workRecord.assignToWeeklyAllowance(newWeeklyAllowance);
            workRecord.addToWeeklyAllowance();

            // WorkRecord 업데이트
            workRecord.updateWorkRecord(
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getBreakMinutes(),
                    totalMinutes,
                    request.getMemo()
            );

            workRecordRepository.save(workRecord);

            // COMPLETED 상태면 정확한 휴일 정보와 사업장 규모를 반영하여 재계산
            if (workRecord.getStatus() == WorkRecordStatus.COMPLETED) {
                calculationService.calculateWorkRecordDetails(workRecord);
                workRecordRepository.save(workRecord);
            }

            // 도메인 간 협력 처리
            coordinatorService.handleWorkRecordUpdate(workRecord, oldWeeklyAllowance, newWeeklyAllowance);

            // 근로자에게 변경 알림 전송
            User worker = workRecord.getContract().getWorker().getUser();
            String title = String.format("%s 근무 일정이 수정되었습니다.", originalWorkDate.toString());

            NotificationEvent event = NotificationEvent.builder()
                    .user(worker)
                    .type(NotificationType.SCHEDULE_CHANGE)
                    .title(title)
                    .actionType(NotificationActionType.VIEW_WORK_RECORD)
                    .actionData(buildActionData(workRecordId))
                    .build();

            eventPublisher.publishEvent(event);
        }

        return WorkRecordDto.Response.from(workRecord);
    }


    public void completeWorkRecord(Long workRecordId) {
        WorkRecord workRecord = workRecordRepository.findById(workRecordId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));
        workRecord.complete();
        workRecordRepository.save(workRecord);

        // 정확한 휴일 정보와 사업장 규모를 반영하여 재계산
        calculationService.calculateWorkRecordDetails(workRecord);
        workRecordRepository.save(workRecord);

        // 근무 완료 시 급여 재계산 (COMPLETED 상태가 되어야 급여에 포함됨)
        coordinatorService.handleWorkRecordCompletion(workRecord);
    }

    /**
     * 근무 일정 삭제 (소프트 삭제)
     * 실제로 삭제하지 않고 status를 DELETED로 변경
     */
    public void deleteWorkRecord(Long workRecordId) {
        WorkRecord workRecord = workRecordRepository.findById(workRecordId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));

        // 이미 삭제된 경우 체크
        if (workRecord.getStatus() == WorkRecordStatus.DELETED) {
            throw new BadRequestException(ErrorCode.INVALID_WORK_RECORD_STATUS, "이미 삭제된 근무 기록입니다.");
        }

        // 알림 전송을 위해 데이터 저장
        User worker = workRecord.getContract().getWorker().getUser();
        LocalDate workDate = workRecord.getWorkDate();
        WorkRecordStatus previousStatus = workRecord.getStatus();
        WeeklyAllowance weeklyAllowance = workRecord.getWeeklyAllowance();

        // 소프트 삭제: status를 DELETED로 변경
        workRecord.markAsDeleted();

        // 도메인 간 협력 처리 (WeeklyAllowance 및 Salary 재계산)
        coordinatorService.handleWorkRecordDeletion(weeklyAllowance, workRecord, previousStatus);

        // 근로자에게 삭제 알림 전송
        String title = String.format("%s 근무 일정이 삭제되었습니다.", workDate.toString());

        NotificationEvent event = NotificationEvent.builder()
                .user(worker)
                .type(NotificationType.SCHEDULE_DELETED)
                .title(title)
                .actionType(NotificationActionType.NONE)  // 삭제된 경우 액션 없음
                .actionData(null)
                .build();

        eventPublisher.publishEvent(event);
    }

    /**
     * 고용주가 근무 일정 일괄 생성
     * 여러 날짜에 동일한 시간으로 일정 생성
     */
    public WorkRecordDto.BatchCreateResponse createWorkRecordsBatch(WorkRecordDto.BatchCreateRequest request) {
        WorkerContract contract = workerContractRepository.findById(request.getContractId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        int createdCount = 0;

        for (LocalDate workDate : request.getWorkDates()) {
            // 중복 체크 (이미 해당 날짜에 근무 기록이 있으면 스킵)
            boolean exists = workRecordRepository.existsByContractAndWorkDate(contract, workDate);
            if (exists) {
                continue;
            }

            int totalMinutes = calculateWorkMinutes(
                    LocalDateTime.of(workDate, request.getStartTime()),
                    LocalDateTime.of(workDate, request.getEndTime()),
                    request.getBreakMinutes() != null ? request.getBreakMinutes() : 0
            );

            // 근무 날짜와 현재 날짜를 비교하여 상태 결정
            WorkRecordStatus status = workDate.isBefore(LocalDate.now())
                    ? WorkRecordStatus.COMPLETED
                    : WorkRecordStatus.SCHEDULED;

            // WorkRecord가 생성된 주에 WeeklyAllowance 자동 생성/조회
            WeeklyAllowance weeklyAllowance = coordinatorService.getOrCreateWeeklyAllowance(
                    contract.getId(), workDate);

            WorkRecord workRecord = WorkRecord.builder()
                    .contract(contract)
                    .workDate(workDate)
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .breakMinutes(request.getBreakMinutes() != null ? request.getBreakMinutes() : 0)
                    .totalWorkMinutes(totalMinutes)
                    .status(status)
                    .memo(request.getMemo())
                    .weeklyAllowance(weeklyAllowance)
                    .build();

            WorkRecord savedRecord = workRecordRepository.save(workRecord);
            createdCount++;

            // COMPLETED 상태면 정확한 휴일 정보와 사업장 규모를 반영하여 재계산
            if (status == WorkRecordStatus.COMPLETED) {
                calculationService.calculateWorkRecordDetails(savedRecord);
                workRecordRepository.save(savedRecord);
            }

            // 도메인 간 협력 처리
            if (status == WorkRecordStatus.COMPLETED) {
                coordinatorService.handleWorkRecordCreation(savedRecord);
                coordinatorService.handleWorkRecordCompletion(savedRecord);
            } else {
                coordinatorService.handleWorkRecordCreation(savedRecord);
            }
        }

        // 근로자에게 일괄 생성 알림 전송 (1회만)
        if (createdCount > 0) {
            User worker = contract.getWorker().getUser();
            String title = String.format("%d개의 근무 일정이 등록되었습니다.", createdCount);

            NotificationEvent event = NotificationEvent.builder()
                    .user(worker)
                    .type(NotificationType.SCHEDULE_CREATED)
                    .title(title)
                    .actionType(NotificationActionType.VIEW_WORK_RECORD)
                    .actionData(null)  // 일괄 생성이므로 특정 레코드 ID 없음
                    .build();

            eventPublisher.publishEvent(event);
        }

        // 결과 반환
        return WorkRecordDto.BatchCreateResponse.builder()
                .createdCount(createdCount)
                .skippedCount(request.getWorkDates().size() - createdCount)
                .totalRequested(request.getWorkDates().size())
                .build();
    }

    /**
     * 계약 정보 변경 시 미래 WorkRecord 재생성
     * - 오늘 이후의 SCHEDULED 상태 WorkRecord 삭제
     * - 변경된 근무 스케줄로 새로운 WorkRecord 생성
     */
    public void regenerateFutureWorkRecords(Long contractId) {
        WorkerContract contract = workerContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        // 삭제할 WorkRecord를 참조하는 CorrectionRequest 삭제
        correctionRequestRepository.deleteByWorkRecordContractAndDateAfterAndStatus(
                contractId, LocalDate.now(), WorkRecordStatus.SCHEDULED);

        // 오늘 이후의 SCHEDULED 상태 WorkRecord 삭제
        workRecordRepository.deleteByContractIdAndWorkDateAfterAndStatus(
                contractId, LocalDate.now(), WorkRecordStatus.SCHEDULED);

        // 새로운 WorkRecord 생성 (오늘+1 ~ 2개월 뒤)
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusMonths(2);
        workRecordGenerationService.generateWorkRecordsForPeriod(contract, startDate, endDate);
    }

    private int calculateWorkMinutes(LocalDateTime start, LocalDateTime end, int breakMinutes) {
        long totalMinutes = Duration.between(start, end).toMinutes();
        return (int) (totalMinutes - breakMinutes);
    }

    /**
     * 알림의 액션 데이터 생성 (JSON 형식)
     */
    private String buildActionData(Long workRecordId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = new HashMap<>();
            data.put("workRecordId", workRecordId);
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }
}
