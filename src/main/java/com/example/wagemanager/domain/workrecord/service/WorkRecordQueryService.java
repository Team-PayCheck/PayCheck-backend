package com.example.wagemanager.domain.workrecord.service;

import com.example.wagemanager.common.exception.ErrorCode;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.correction.entity.CorrectionRequest;
import com.example.wagemanager.domain.correction.enums.CorrectionStatus;
import com.example.wagemanager.domain.correction.repository.CorrectionRequestRepository;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.worker.repository.WorkerRepository;
import com.example.wagemanager.domain.workrecord.dto.PendingApprovalDto;
import com.example.wagemanager.domain.workrecord.dto.WorkRecordDto;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import com.example.wagemanager.domain.workrecord.repository.WorkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkRecordQueryService {

    private final WorkRecordRepository workRecordRepository;
    private final WorkerRepository workerRepository;
    private final CorrectionRequestRepository correctionRequestRepository;

    public List<WorkRecordDto.Response> getWorkRecordsByContract(Long contractId) {
        return workRecordRepository.findByContractId(contractId).stream()
                .map(WorkRecordDto.Response::from)
                .collect(Collectors.toList());
    }

    public WorkRecordDto.DetailedResponse getWorkRecordById(Long workRecordId) {
        WorkRecord workRecord = workRecordRepository.findById(workRecordId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "근무 기록을 찾을 수 없습니다."));
        return WorkRecordDto.DetailedResponse.from(workRecord);
    }

    // 고용주용: 사업장의 근무 기록 조회 (캘린더)
    public List<WorkRecordDto.CalendarResponse> getWorkRecordsByWorkplaceAndDateRange(
            Long workplaceId, LocalDate startDate, LocalDate endDate) {
        List<WorkRecord> records = workRecordRepository.findByWorkplaceAndDateRange(workplaceId, startDate, endDate);
        return records.stream()
                .map(WorkRecordDto.CalendarResponse::from)
                .collect(Collectors.toList());
    }

    // 근로자용: 내 근무 기록 조회
    public List<WorkRecordDto.DetailedResponse> getWorkRecordsByWorkerAndDateRange(
            Long userId, LocalDate startDate, LocalDate endDate) {
        Worker worker = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKER_NOT_FOUND, "근로자 정보를 찾을 수 없습니다."));

        List<WorkRecord> records = workRecordRepository.findByWorkerAndDateRange(worker.getId(), startDate, endDate);
        return records.stream()
                .map(WorkRecordDto.DetailedResponse::from)
                .collect(Collectors.toList());
    }

    // 고용주용: 승인 대기중인 모든 요청 조회 (CorrectionRequest + WorkRecord 통합)
    public PendingApprovalDto.Response getAllPendingApprovalsByWorkplace(Long workplaceId, PendingApprovalDto.FilterType filterType) {
        List<PendingApprovalDto.CorrectionRequestInfo> correctionRequestInfos = List.of();
        List<PendingApprovalDto.WorkRecordCreationInfo> workRecordCreationInfos = List.of();

        // 필터 타입에 따라 조회
        if (filterType == PendingApprovalDto.FilterType.ALL || filterType == PendingApprovalDto.FilterType.CORRECTION) {
            // 1. CorrectionRequest (수정 요청) - PENDING 상태
            List<CorrectionRequest> correctionRequests = correctionRequestRepository.findByWorkplaceIdAndStatus(
                    workplaceId, CorrectionStatus.PENDING);
            correctionRequestInfos = correctionRequests.stream()
                    .map(PendingApprovalDto.CorrectionRequestInfo::from)
                    .sorted(Comparator.comparing(PendingApprovalDto.CorrectionRequestInfo::getCreatedAt).reversed())
                    .collect(Collectors.toList());
        }

        if (filterType == PendingApprovalDto.FilterType.ALL || filterType == PendingApprovalDto.FilterType.CREATION) {
            // 2. WorkRecord (생성 요청) - PENDING_APPROVAL 상태
            List<WorkRecord> workRecords = workRecordRepository.findByWorkplaceAndStatus(
                    workplaceId, WorkRecordStatus.PENDING_APPROVAL);
            workRecordCreationInfos = workRecords.stream()
                    .map(PendingApprovalDto.WorkRecordCreationInfo::from)
                    .sorted(Comparator.comparing(PendingApprovalDto.WorkRecordCreationInfo::getCreatedAt).reversed())
                    .collect(Collectors.toList());
        }

        // 3. 통합 응답 생성
        return PendingApprovalDto.Response.builder()
                .correctionRequests(correctionRequestInfos)
                .workRecordCreations(workRecordCreationInfos)
                .build();
    }
}
