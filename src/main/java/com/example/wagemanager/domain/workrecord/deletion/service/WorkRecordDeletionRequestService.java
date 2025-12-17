package com.example.wagemanager.domain.workrecord.deletion.service;

import com.example.wagemanager.common.exception.BadRequestException;
import com.example.wagemanager.common.exception.ErrorCode;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.common.exception.UnauthorizedException;
import com.example.wagemanager.domain.notification.enums.NotificationActionType;
import com.example.wagemanager.domain.notification.enums.NotificationType;
import com.example.wagemanager.domain.notification.event.NotificationEvent;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workrecord.deletion.dto.WorkRecordDeletionRequestDto;
import com.example.wagemanager.domain.workrecord.deletion.entity.WorkRecordDeletionRequest;
import com.example.wagemanager.domain.workrecord.deletion.enums.WorkRecordDeletionRequestStatus;
import com.example.wagemanager.domain.workrecord.deletion.repository.WorkRecordDeletionRequestRepository;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.repository.WorkRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkRecordDeletionRequestService {

    private final WorkRecordRepository workRecordRepository;
    private final WorkRecordDeletionRequestRepository deletionRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WorkRecordDeletionRequestDto.Response requestDeletion(
            User requester,
            Long workRecordId,
            WorkRecordDeletionRequestDto.CreateRequest request
    ) {
        WorkRecord workRecord = workRecordRepository.findById(workRecordId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));

        Long workerId = workRecord.getContract().getWorker().getUser().getId();
        if (!workerId.equals(requester.getId())) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ACCESS, "본인의 근무 일정만 삭제 요청할 수 있습니다.");
        }

        boolean pendingExists = deletionRequestRepository.existsByWorkRecordIdAndStatus(
                workRecordId, WorkRecordDeletionRequestStatus.PENDING);
        if (pendingExists) {
            throw new BadRequestException(ErrorCode.DUPLICATE_DELETION_REQUEST, "이미 처리 대기 중인 삭제 요청이 있습니다.");
        }

        WorkRecordDeletionRequest deletionRequest = WorkRecordDeletionRequest.builder()
                .workRecord(workRecord)
                .requester(requester)
                .reason(request != null ? request.getReason() : null)
                .status(WorkRecordDeletionRequestStatus.PENDING)
                .build();

        WorkRecordDeletionRequest savedRequest = deletionRequestRepository.save(deletionRequest);

        // 고용주에게 알림 전송
        User employer = workRecord.getContract().getWorkplace().getEmployer().getUser();
        String title = String.format("%s님이 %s 근무 일정 삭제를 요청했습니다.",
                requester.getName(), workRecord.getWorkDate());

        NotificationEvent event = NotificationEvent.builder()
                .user(employer)
                .type(NotificationType.SCHEDULE_DELETION_REQUEST)
                .title(title)
                .actionType(NotificationActionType.VIEW_WORK_RECORD)
                .actionData(buildActionData(workRecord.getId()))
                .build();

        eventPublisher.publishEvent(event);

        return WorkRecordDeletionRequestDto.Response.from(savedRequest);
    }

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
