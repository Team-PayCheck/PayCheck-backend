package com.example.wagemanager.domain.workrecord.service;

import com.example.wagemanager.common.exception.BadRequestException;
import com.example.wagemanager.common.exception.ErrorCode;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.allowance.entity.WeeklyAllowance;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.contract.repository.WorkerContractRepository;
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
    private final WorkRecordCoordinatorService coordinatorService;
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

    /**
     * 근로자가 근무 일정 생성 요청 (승인 필요)
     * PENDING_APPROVAL 상태로 생성
     */
    public WorkRecordDto.Response createWorkRecordByWorker(User worker, WorkRecordDto.CreateRequest request) {
        WorkerContract contract = workerContractRepository.findById(request.getContractId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        int totalMinutes = calculateWorkMinutes(
                LocalDateTime.of(request.getWorkDate(), request.getStartTime()),
                LocalDateTime.of(request.getWorkDate(), request.getEndTime()),
                request.getBreakMinutes() != null ? request.getBreakMinutes() : 0
        );

        // WorkRecord가 생성된 주에 WeeklyAllowance 자동 생성/조회
        WeeklyAllowance weeklyAllowance = coordinatorService.getOrCreateWeeklyAllowance(
                contract.getId(), request.getWorkDate());

        // 근로자가 생성하는 경우 항상 PENDING_APPROVAL 상태
        WorkRecord workRecord = WorkRecord.builder()
                .contract(contract)
                .workDate(request.getWorkDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .breakMinutes(request.getBreakMinutes() != null ? request.getBreakMinutes() : 0)
                .totalWorkMinutes(totalMinutes)
                .status(WorkRecordStatus.PENDING_APPROVAL)
                .memo(request.getMemo())
                .weeklyAllowance(weeklyAllowance)
                .build();

        WorkRecord savedRecord = workRecordRepository.save(workRecord);

        // 도메인 간 협력 처리 (PENDING_APPROVAL은 주휴수당 재계산 제외)
        coordinatorService.handleWorkRecordCreation(savedRecord);

        // 고용주에게 승인 요청 알림 전송
        User employer = savedRecord.getContract().getWorkplace().getEmployer().getUser();
        String title = String.format("%s님이 %s 근무 일정 승인을 요청했습니다.",
                worker.getName(), request.getWorkDate().toString());

        NotificationEvent event = NotificationEvent.builder()
                .user(employer)
                .type(NotificationType.SCHEDULE_APPROVAL_REQUEST)
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

    /**
     * 근무 일정 승인 (PENDING_APPROVAL -> SCHEDULED)
     */
    public WorkRecordDto.Response approveWorkRecord(Long workRecordId) {
        WorkRecord workRecord = workRecordRepository.findById(workRecordId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));

        // PENDING_APPROVAL 상태만 승인 가능
        if (workRecord.getStatus() != WorkRecordStatus.PENDING_APPROVAL) {
            throw new BadRequestException(ErrorCode.INVALID_WORK_RECORD_STATUS, "승인 대기 중인 근무 일정만 승인할 수 있습니다.");
        }

        // 승인: PENDING_APPROVAL -> SCHEDULED
        workRecord.approve();

        // 주휴수당 재계산 (이제 계산에 포함됨)
        coordinatorService.recalculateAllowanceAfterApproval(workRecord);

        // 근로자에게 승인 알림 전송
        User worker = workRecord.getContract().getWorker().getUser();
        String title = String.format("%s 근무 일정이 승인되었습니다.", workRecord.getWorkDate().toString());

        NotificationEvent event = NotificationEvent.builder()
                .user(worker)
                .type(NotificationType.SCHEDULE_APPROVED)
                .title(title)
                .actionType(NotificationActionType.VIEW_WORK_RECORD)
                .actionData(buildActionData(workRecordId))
                .build();

        eventPublisher.publishEvent(event);

        return WorkRecordDto.Response.from(workRecord);
    }

    /**
     * 근무 일정 거절 (PENDING_APPROVAL 삭제)
     */
    public void rejectWorkRecord(Long workRecordId) {
        WorkRecord workRecord = workRecordRepository.findById(workRecordId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));

        // PENDING_APPROVAL 상태만 거절 가능
        if (workRecord.getStatus() != WorkRecordStatus.PENDING_APPROVAL) {
            throw new BadRequestException(ErrorCode.INVALID_WORK_RECORD_STATUS, "승인 대기 중인 근무 일정만 거절할 수 있습니다.");
        }

        // 알림 전송을 위해 데이터 저장
        User worker = workRecord.getContract().getWorker().getUser();
        LocalDate workDate = workRecord.getWorkDate();
        WeeklyAllowance weeklyAllowance = workRecord.getWeeklyAllowance();
        WorkRecordStatus status = workRecord.getStatus();

        // 양방향 관계 해제
        workRecord.removeFromWeeklyAllowance();

        // WorkRecord 삭제
        workRecordRepository.delete(workRecord);

        // 도메인 간 협력 처리 (WeeklyAllowance 정리)
        coordinatorService.handleWorkRecordDeletion(weeklyAllowance, workRecord, status);

        // 근로자에게 거절 알림 전송
        String title = String.format("%s 근무 일정 요청이 거절되었습니다.", workDate.toString());

        NotificationEvent event = NotificationEvent.builder()
                .user(worker)
                .type(NotificationType.SCHEDULE_REJECTED)
                .title(title)
                .actionType(null)
                .actionData(null)
                .build();

        eventPublisher.publishEvent(event);
    }

    public void completeWorkRecord(Long workRecordId) {
        WorkRecord workRecord = workRecordRepository.findById(workRecordId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));
        workRecord.complete();

        // 근무 완료 시 급여 재계산 (COMPLETED 상태가 되어야 급여에 포함됨)
        coordinatorService.handleWorkRecordCompletion(workRecord);
    }

    public void deleteWorkRecord(Long workRecordId) {
        WorkRecord workRecord = workRecordRepository.findById(workRecordId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));

        // 알림 전송을 위해 데이터 저장
        User worker = workRecord.getContract().getWorker().getUser();
        LocalDate workDate = workRecord.getWorkDate();

        // SCHEDULED, COMPLETED 모두 삭제 가능
        WorkRecordStatus status = workRecord.getStatus();
        WeeklyAllowance weeklyAllowance = workRecord.getWeeklyAllowance();

        // 양방향 관계 해제
        workRecord.removeFromWeeklyAllowance();

        workRecordRepository.delete(workRecord);

        // 도메인 간 협력 처리
        coordinatorService.handleWorkRecordDeletion(weeklyAllowance, workRecord, status);

        // 근로자에게 삭제 알림 전송
        String title = String.format("%s 근무 일정이 삭제되었습니다.", workDate.toString());

        NotificationEvent event = NotificationEvent.builder()
                .user(worker)
                .type(NotificationType.SCHEDULE_DELETED)
                .title(title)
                .actionType(null)  // 삭제된 경우 액션 없음
                .actionData(null)
                .build();

        eventPublisher.publishEvent(event);
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
