package com.example.paycheck.domain.correction.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.common.exception.UnauthorizedException;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.correction.dto.CorrectionRequestDto;
import com.example.paycheck.domain.correction.entity.CorrectionRequest;
import com.example.paycheck.domain.correction.enums.CorrectionStatus;
import com.example.paycheck.domain.correction.enums.RequestType;
import com.example.paycheck.domain.correction.repository.CorrectionRequestRepository;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.notification.event.NotificationEvent;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.workrecord.dto.WorkRecordDto;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.example.paycheck.domain.workrecord.service.WorkRecordCommandService;
import com.example.paycheck.domain.workrecord.service.WorkRecordCoordinatorService;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorrectionRequestService {

    private final CorrectionRequestRepository correctionRequestRepository;
    private final WorkRecordRepository workRecordRepository;
    private final WorkerContractRepository workerContractRepository;
    private final WorkRecordCommandService workRecordCommandService;
    private final WorkRecordCoordinatorService coordinatorService;
    private final ApplicationEventPublisher eventPublisher;

    // ===== 근로자용 API =====

    /**
     * 정정요청 생성 (CREATE/UPDATE/DELETE 타입 지원)
     */
    @Transactional
    public CorrectionRequestDto.Response createCorrectionRequest(User requester, CorrectionRequestDto.CreateRequest request) {
        // 타입별 분기 처리
        if (request.getType() == RequestType.CREATE) {
            return createCreateRequest(requester, request);
        } else if (request.getType() == RequestType.UPDATE) {
            return createUpdateRequest(requester, request);
        } else if (request.getType() == RequestType.DELETE) {
            return createDeleteRequest(requester, request);
        }

        throw new BadRequestException(ErrorCode.INVALID_REQUEST_TYPE, "지원하지 않는 요청 타입입니다.");
    }

    private CorrectionRequestDto.Response createCreateRequest(User requester, CorrectionRequestDto.CreateRequest request) {
        // CREATE 타입에서는 contractId가 필수
        if (request.getContractId() == null) {
            throw new BadRequestException(ErrorCode.CONTRACT_ID_REQUIRED, "계약 ID는 필수입니다.");
        }

        // 계약 조회 및 권한 확인
        WorkerContract contract = workerContractRepository.findById(request.getContractId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        if (!contract.getWorker().getUser().getId().equals(requester.getId())) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ACCESS, "본인의 계약에 대해서만 요청할 수 있습니다.");
        }

        // 중복 CREATE 요청 확인 - 동일한 시간대에 요청이 있는지 확인
        if (correctionRequestRepository.existsPendingCreateRequestWithTimeOverlap(
                request.getContractId(),
                request.getRequestedWorkDate(),
                request.getRequestedStartTime(),
                request.getRequestedEndTime(),
                RequestType.CREATE,
                CorrectionStatus.PENDING)) {
            throw new BadRequestException(ErrorCode.DUPLICATE_CORRECTION_REQUEST, "해당 시간대에 이미 생성 요청이 있습니다.");
        }

        // 중복 근무 기록 확인 - 이미 등록된 근무 기록이 있는지 확인
        if (workRecordRepository.existsOverlappingWorkRecord(
                request.getContractId(),
                request.getRequestedWorkDate(),
                request.getRequestedStartTime(),
                request.getRequestedEndTime(),
                WorkRecordStatus.DELETED)) {
            throw new BadRequestException(ErrorCode.DUPLICATE_WORK_RECORD, "해당 시간대에 이미 등록된 근무 기록이 있습니다.");
        }

        CorrectionRequest correctionRequest = CorrectionRequest.builder()
                .type(RequestType.CREATE)
                .workRecord(null)
                .contract(contract)
                .requester(requester)
                .originalWorkDate(null)
                .originalStartTime(null)
                .originalEndTime(null)
                .requestedWorkDate(request.getRequestedWorkDate())
                .requestedStartTime(request.getRequestedStartTime())
                .requestedEndTime(request.getRequestedEndTime())
                .requestedBreakMinutes(request.getRequestedBreakMinutes())
                .requestedMemo(request.getRequestedMemo())
                .status(CorrectionStatus.PENDING)
                .build();

        CorrectionRequest savedRequest = correctionRequestRepository.save(correctionRequest);
        publishCorrectionRequestCreatedEvent(savedRequest);
        return CorrectionRequestDto.Response.from(savedRequest);
    }

    private CorrectionRequestDto.Response createUpdateRequest(User requester, CorrectionRequestDto.CreateRequest request) {
        // UPDATE 타입에서는 workRecordId가 필수
        if (request.getWorkRecordId() == null) {
            throw new BadRequestException(ErrorCode.WORK_RECORD_ID_REQUIRED, "근무 기록 ID는 필수입니다.");
        }

        WorkRecord workRecord = workRecordRepository.findById(request.getWorkRecordId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));

        // 이미 삭제된 근무 기록인지 확인
        if (workRecord.getStatus() == com.example.paycheck.domain.workrecord.enums.WorkRecordStatus.DELETED) {
            throw new BadRequestException(ErrorCode.INVALID_WORK_RECORD_STATUS, "삭제된 근무 기록은 수정할 수 없습니다.");
        }

        // 중복 UPDATE 요청 확인
        if (correctionRequestRepository.existsByWorkRecordIdAndStatus(request.getWorkRecordId(), CorrectionStatus.PENDING)) {
            throw new BadRequestException(ErrorCode.DUPLICATE_CORRECTION_REQUEST, "해당 근무 기록에 이미 대기중인 정정요청이 있습니다.");
        }

        // 본인의 근무기록인지 확인
        if (!workRecord.getContract().getWorker().getUser().getId().equals(requester.getId())) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ACCESS, "본인의 근무 기록에 대해서만 정정요청을 할 수 있습니다.");
        }

        CorrectionRequest correctionRequest = CorrectionRequest.builder()
                .type(RequestType.UPDATE)
                .workRecord(workRecord)
                .contract(null)
                .requester(requester)
                .originalWorkDate(workRecord.getWorkDate())
                .originalStartTime(workRecord.getStartTime())
                .originalEndTime(workRecord.getEndTime())
                .requestedWorkDate(request.getRequestedWorkDate())
                .requestedStartTime(request.getRequestedStartTime())
                .requestedEndTime(request.getRequestedEndTime())
                .requestedBreakMinutes(request.getRequestedBreakMinutes())
                .requestedMemo(request.getRequestedMemo())
                .status(CorrectionStatus.PENDING)
                .build();

        CorrectionRequest savedRequest = correctionRequestRepository.save(correctionRequest);
        publishCorrectionRequestCreatedEvent(savedRequest);
        return CorrectionRequestDto.Response.from(savedRequest);
    }

    private CorrectionRequestDto.Response createDeleteRequest(User requester, CorrectionRequestDto.CreateRequest request) {
        // DELETE 타입에서는 workRecordId가 필수
        if (request.getWorkRecordId() == null) {
            throw new BadRequestException(ErrorCode.WORK_RECORD_ID_REQUIRED, "근무 기록 ID는 필수입니다.");
        }

        WorkRecord workRecord = workRecordRepository.findById(request.getWorkRecordId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));

        // 이미 삭제된 근무 기록인지 확인
        if (workRecord.getStatus() == com.example.paycheck.domain.workrecord.enums.WorkRecordStatus.DELETED) {
            throw new BadRequestException(ErrorCode.INVALID_WORK_RECORD_STATUS, "이미 삭제된 근무 기록입니다.");
        }

        // 중복 DELETE 요청 확인
        if (correctionRequestRepository.existsByWorkRecordIdAndStatus(request.getWorkRecordId(), CorrectionStatus.PENDING)) {
            throw new BadRequestException(ErrorCode.DUPLICATE_CORRECTION_REQUEST, "해당 근무 기록에 이미 대기중인 삭제 요청이 있습니다.");
        }

        // 본인의 근무기록인지 확인
        if (!workRecord.getContract().getWorker().getUser().getId().equals(requester.getId())) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ACCESS, "본인의 근무 기록에 대해서만 삭제요청을 할 수 있습니다.");
        }

        CorrectionRequest correctionRequest = CorrectionRequest.builder()
                .type(RequestType.DELETE)
                .workRecord(workRecord)
                .contract(null)
                .requester(requester)
                .originalWorkDate(workRecord.getWorkDate())
                .originalStartTime(workRecord.getStartTime())
                .originalEndTime(workRecord.getEndTime())
                .requestedWorkDate(workRecord.getWorkDate())
                .requestedStartTime(workRecord.getStartTime())
                .requestedEndTime(workRecord.getEndTime())
                .requestedBreakMinutes(workRecord.getBreakMinutes())
                .requestedMemo(request.getRequestedMemo())
                .status(CorrectionStatus.PENDING)
                .build();

        CorrectionRequest savedRequest = correctionRequestRepository.save(correctionRequest);
        publishCorrectionRequestCreatedEvent(savedRequest);
        return CorrectionRequestDto.Response.from(savedRequest);
    }

    /**
     * 내 정정요청 목록 조회
     */
    public List<CorrectionRequestDto.ListResponse> getMyCorrectionRequests(User requester, CorrectionStatus status) {
        List<CorrectionRequest> requests;
        if (status != null) {
            requests = correctionRequestRepository.findByRequesterIdAndStatus(requester.getId(), status);
        } else {
            requests = correctionRequestRepository.findByRequesterId(requester.getId());
        }

        return requests.stream()
                .map(CorrectionRequestDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 내 정정요청 상세 조회
     */
    public CorrectionRequestDto.Response getMyCorrectionRequest(User requester, Long correctionRequestId) {
        CorrectionRequest correctionRequest = correctionRequestRepository.findByIdWithDetails(correctionRequestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CORRECTION_REQUEST_NOT_FOUND, "정정요청을 찾을 수 없습니다."));

        // 본인의 정정요청인지 확인
        if (!correctionRequest.getRequester().getId().equals(requester.getId())) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ACCESS, "본인의 정정요청만 조회할 수 있습니다.");
        }

        return CorrectionRequestDto.Response.from(correctionRequest);
    }

    /**
     * 정정요청 취소 (PENDING 상태만 가능)
     */
    @Transactional
    public void cancelCorrectionRequest(User requester, Long correctionRequestId) {
        CorrectionRequest correctionRequest = correctionRequestRepository.findById(correctionRequestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CORRECTION_REQUEST_NOT_FOUND, "정정요청을 찾을 수 없습니다."));

        // 본인의 정정요청인지 확인
        if (!correctionRequest.getRequester().getId().equals(requester.getId())) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ACCESS, "본인의 정정요청만 취소할 수 있습니다.");
        }

        // PENDING 상태만 취소 가능
        if (correctionRequest.getStatus() != CorrectionStatus.PENDING) {
            throw new BadRequestException(ErrorCode.INVALID_CORRECTION_STATUS, "대기중인 정정요청만 취소할 수 있습니다.");
        }

        correctionRequestRepository.delete(correctionRequest);
    }

    // ===== 고용주용 API =====

    /**
     * 사업장별 정정요청 목록 조회 (페이징)
     */
    public Page<CorrectionRequestDto.ListResponse> getCorrectionRequestsByWorkplace(
            Long workplaceId, CorrectionStatus status, Pageable pageable) {
        Page<CorrectionRequest> requests;
        if (status != null) {
            requests = correctionRequestRepository.findByWorkplaceIdAndStatus(workplaceId, status, pageable);
        } else {
            requests = correctionRequestRepository.findByWorkplaceId(workplaceId, pageable);
        }

        return requests.map(CorrectionRequestDto.ListResponse::from);
    }

    /**
     * 정정요청 상세 조회 (고용주용)
     */
    public CorrectionRequestDto.Response getCorrectionRequestDetail(Long correctionRequestId) {
        CorrectionRequest correctionRequest = correctionRequestRepository.findByIdWithDetails(correctionRequestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CORRECTION_REQUEST_NOT_FOUND, "정정요청을 찾을 수 없습니다."));

        return CorrectionRequestDto.Response.from(correctionRequest);
    }

    /**
     * 정정요청 승인 (CREATE/UPDATE/DELETE 타입별 처리)
     */
    @Transactional
    public CorrectionRequestDto.Response approveCorrectionRequest(Long correctionRequestId) {
        CorrectionRequest correctionRequest = correctionRequestRepository.findByIdWithDetails(correctionRequestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CORRECTION_REQUEST_NOT_FOUND, "정정요청을 찾을 수 없습니다."));

        // PENDING 상태만 승인 가능
        if (correctionRequest.getStatus() != CorrectionStatus.PENDING) {
            throw new BadRequestException(ErrorCode.INVALID_CORRECTION_STATUS, "대기중인 정정요청만 승인할 수 있습니다.");
        }

        // 타입별 승인 로직 실행
        if (correctionRequest.isCreateType()) {
            approveCreateRequest(correctionRequest);
        } else if (correctionRequest.isUpdateType()) {
            approveUpdateRequest(correctionRequest);
        } else if (correctionRequest.isDeleteType()) {
            approveDeleteRequest(correctionRequest);
        }

        // 상태 업데이트
        correctionRequest.approve();

        publishCorrectionResponseEvent(correctionRequest, true);
        return CorrectionRequestDto.Response.from(correctionRequest);
    }

    private void approveCreateRequest(CorrectionRequest correctionRequest) {
        // WorkRecord 생성
        WorkRecordDto.CreateRequest createRequest = WorkRecordDto.CreateRequest.builder()
                .contractId(correctionRequest.getContract().getId())
                .workDate(correctionRequest.getRequestedWorkDate())
                .startTime(correctionRequest.getRequestedStartTime())
                .endTime(correctionRequest.getRequestedEndTime())
                .breakMinutes(correctionRequest.getRequestedBreakMinutes() != null ? correctionRequest.getRequestedBreakMinutes() : 0)
                .build();

        workRecordCommandService.createWorkRecordByEmployer(createRequest);
    }

    private void approveUpdateRequest(CorrectionRequest correctionRequest) {
        WorkRecord workRecord = correctionRequest.getWorkRecord();

        // 기존 WeeklyAllowance 저장 (재계산용)
        WeeklyAllowance oldWeeklyAllowance = workRecord.getWeeklyAllowance();

        // WorkRecord 업데이트
        workRecord.updateWorkTime(
                correctionRequest.getRequestedStartTime(),
                correctionRequest.getRequestedEndTime(),
                correctionRequest.getRequestedMemo());

        // WeeklyAllowance 및 Salary 재계산
        coordinatorService.handleWorkRecordUpdate(workRecord, oldWeeklyAllowance, workRecord.getWeeklyAllowance());
    }

    private void approveDeleteRequest(CorrectionRequest correctionRequest) {
        WorkRecord workRecord = correctionRequest.getWorkRecord();

        // 소프트 삭제
        workRecord.markAsDeleted();

        // WeeklyAllowance 및 Salary 재계산 처리
        if (workRecord.getWeeklyAllowance() != null) {
            coordinatorService.handleWorkRecordDeletion(
                    workRecord.getWeeklyAllowance(),
                    workRecord,
                    workRecord.getStatus());
        }
    }

    /**
     * 정정요청 거절
     */
    @Transactional
    public CorrectionRequestDto.Response rejectCorrectionRequest(Long correctionRequestId) {
        CorrectionRequest correctionRequest = correctionRequestRepository.findByIdWithDetails(correctionRequestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CORRECTION_REQUEST_NOT_FOUND, "정정요청을 찾을 수 없습니다."));

        // PENDING 상태만 거절 가능
        if (correctionRequest.getStatus() != CorrectionStatus.PENDING) {
            throw new BadRequestException(ErrorCode.INVALID_CORRECTION_STATUS, "대기중인 정정요청만 거절할 수 있습니다.");
        }

        correctionRequest.reject();

        publishCorrectionResponseEvent(correctionRequest, false);
        return CorrectionRequestDto.Response.from(correctionRequest);
    }

    private void publishCorrectionRequestCreatedEvent(CorrectionRequest correctionRequest) {
        WorkerContract contract = getContractForCorrectionRequest(correctionRequest);
        User employerUser = contract.getWorkplace().getEmployer().getUser();
        User requester = correctionRequest.getRequester();
        String title = String.format("%s님의 정정 요청이 도착했습니다.", requester.getName());

        NotificationEvent event = NotificationEvent.builder()
                .user(employerUser)
                .type(NotificationType.UNREAD_CORRECTION_REQUEST)
                .title(title)
                .actionType(NotificationActionType.VIEW_CORRECTION_REQUEST)
                .actionData(buildActionData(correctionRequest.getId()))
                .build();

        eventPublisher.publishEvent(event);
    }

    private void publishCorrectionResponseEvent(CorrectionRequest correctionRequest, boolean approved) {
        User worker = correctionRequest.getRequester();
        String title = String.format("%s 정정 요청이 %s되었습니다.", getTargetWorkDate(correctionRequest), approved ? "승인" : "거절");

        NotificationEvent event = NotificationEvent.builder()
                .user(worker)
                .type(NotificationType.CORRECTION_RESPONSE)
                .title(title)
                .actionType(NotificationActionType.VIEW_CORRECTION_REQUEST)
                .actionData(buildActionData(correctionRequest.getId()))
                .build();

        eventPublisher.publishEvent(event);
    }

    private WorkerContract getContractForCorrectionRequest(CorrectionRequest correctionRequest) {
        if (correctionRequest.getContract() != null) {
            return correctionRequest.getContract();
        }
        return correctionRequest.getWorkRecord().getContract();
    }

    private LocalDate getTargetWorkDate(CorrectionRequest correctionRequest) {
        if (correctionRequest.getRequestedWorkDate() != null) {
            return correctionRequest.getRequestedWorkDate();
        }
        return correctionRequest.getOriginalWorkDate();
    }

    private String buildActionData(Long correctionRequestId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = new HashMap<>();
            data.put("correctionRequestId", correctionRequestId);
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("알림 액션 데이터 생성 실패: correctionRequestId={}", correctionRequestId, e);
            return null;
        }
    }
}
