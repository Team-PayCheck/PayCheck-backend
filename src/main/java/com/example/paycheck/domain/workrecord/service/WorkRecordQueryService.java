package com.example.paycheck.domain.workrecord.service;

import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.correction.dto.CorrectionRequestDto;
import com.example.paycheck.domain.correction.entity.CorrectionRequest;
import com.example.paycheck.domain.correction.enums.CorrectionStatus;
import com.example.paycheck.domain.correction.enums.RequestType;
import com.example.paycheck.domain.correction.repository.CorrectionRequestRepository;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.workrecord.dto.WorkRecordDto;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordCurrentStatus;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final Clock clock;

    public List<WorkRecordDto.Response> getWorkRecordsByContract(Long contractId) {
        return workRecordRepository.findByContractId(contractId, WorkRecordStatus.DELETED).stream()
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
        LocalDate queryStartDate = startDate.minusDays(1);
        List<WorkRecord> records = workRecordRepository.findByWorkplaceAndDateRange(
                workplaceId, queryStartDate, endDate, WorkRecordStatus.DELETED);

        return records.stream()
                .flatMap(record -> getCalendarDisplayDates(record, startDate, endDate).stream()
                        .map(displayDate -> WorkRecordDto.CalendarResponse.from(record, displayDate)))
                .sorted(Comparator.comparing(WorkRecordDto.CalendarResponse::getWorkDate)
                        .thenComparing(WorkRecordDto.CalendarResponse::getStartTime)
                        .thenComparing(WorkRecordDto.CalendarResponse::getId))
                .collect(Collectors.toList());
    }

    // 근로자용: 내 근무 기록 조회
    public List<WorkRecordDto.DetailedResponse> getWorkRecordsByWorkerAndDateRange(
            Long userId, LocalDate startDate, LocalDate endDate) {
        Worker worker = workerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKER_NOT_FOUND, "근로자 정보를 찾을 수 없습니다."));

        List<WorkRecord> records = workRecordRepository.findByWorkerAndDateRange(worker.getId(), startDate, endDate, WorkRecordStatus.DELETED);
        LocalDateTime now = LocalDateTime.now(clock);

        return records.stream()
                .sorted(buildWorkerRecordComparator(now))
                .map(record -> WorkRecordDto.DetailedResponse.from(record, calculateCurrentStatus(record, now)))
                .collect(Collectors.toList());
    }

    // 고용주용: 승인 대기중인 모든 요청 조회 (통합)
    public List<CorrectionRequestDto.ListResponse> getAllPendingApprovalsByWorkplace(
            Long workplaceId, RequestType filterType) {
        List<CorrectionRequest> correctionRequests;

        // 필터 타입에 따라 조회
        if (filterType == null) {
            // 전체 조회 (모든 타입)
            correctionRequests = correctionRequestRepository.findByWorkplaceIdAndStatus(
                    workplaceId, CorrectionStatus.PENDING, Pageable.unpaged()).getContent();
        } else {
            // 특정 타입만 조회
            correctionRequests = correctionRequestRepository.findByWorkplaceIdAndStatusAndType(
                    workplaceId, CorrectionStatus.PENDING, filterType);
        }

        // CorrectionRequestDto.ListResponse로 변환하여 반환
        return correctionRequests.stream()
                .map(CorrectionRequestDto.ListResponse::from)
                .sorted(Comparator.comparing(CorrectionRequestDto.ListResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    private Comparator<WorkRecord> buildWorkerRecordComparator(LocalDateTime now) {
        return Comparator
                .comparing((WorkRecord record) -> calculateCurrentStatus(record, now).getSortOrder())
                .thenComparing((left, right) -> compareWithinSameCurrentStatus(left, right, now))
                .thenComparing(WorkRecord::getId);
    }

    private int compareWithinSameCurrentStatus(WorkRecord left, WorkRecord right, LocalDateTime now) {
        WorkRecordCurrentStatus leftStatus = calculateCurrentStatus(left, now);
        WorkRecordCurrentStatus rightStatus = calculateCurrentStatus(right, now);

        if (leftStatus != rightStatus) {
            return 0;
        }

        return switch (leftStatus) {
            case IN_PROGRESS, UPCOMING -> getStartDateTime(left).compareTo(getStartDateTime(right));
            case COMPLETED -> getEndDateTime(right).compareTo(getEndDateTime(left));
        };
    }

    private WorkRecordCurrentStatus calculateCurrentStatus(WorkRecord workRecord, LocalDateTime now) {
        if (workRecord.getStatus() == WorkRecordStatus.COMPLETED) {
            return WorkRecordCurrentStatus.COMPLETED;
        }

        LocalDateTime startDateTime = getStartDateTime(workRecord);
        LocalDateTime endDateTime = getEndDateTime(workRecord);

        if (!now.isBefore(endDateTime)) {
            return WorkRecordCurrentStatus.COMPLETED;
        }

        if (!now.isBefore(startDateTime)) {
            return WorkRecordCurrentStatus.IN_PROGRESS;
        }

        return WorkRecordCurrentStatus.UPCOMING;
    }

    private LocalDateTime getStartDateTime(WorkRecord workRecord) {
        return workRecord.getWorkDate().atTime(workRecord.getStartTime());
    }

    private LocalDateTime getEndDateTime(WorkRecord workRecord) {
        LocalDate endDate = workRecord.getEndTime().isAfter(workRecord.getStartTime())
                ? workRecord.getWorkDate()
                : workRecord.getWorkDate().plusDays(1);
        return endDate.atTime(workRecord.getEndTime());
    private List<LocalDate> getCalendarDisplayDates(WorkRecord workRecord, LocalDate startDate, LocalDate endDate) {
        List<LocalDate> displayDates = new ArrayList<>();
        LocalDate workDate = workRecord.getWorkDate();

        if (isWithinDateRange(workDate, startDate, endDate)) {
            displayDates.add(workDate);
        }

        if (isOvernight(workRecord)) {
            LocalDate nextDate = workDate.plusDays(1);
            if (isWithinDateRange(nextDate, startDate, endDate)) {
                displayDates.add(nextDate);
            }
        }

        return displayDates;
    }

    private boolean isWithinDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private boolean isOvernight(WorkRecord workRecord) {
        return !workRecord.getEndTime().isAfter(workRecord.getStartTime());
    }
}
