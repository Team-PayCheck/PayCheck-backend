package com.example.wagemanager.domain.workrecord.dto;

import com.example.wagemanager.domain.correction.entity.CorrectionRequest;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

public class PendingApprovalDto {

    // 필터 타입
    public enum FilterType {
        ALL,            // 전체 (기본값)
        CORRECTION,     // 정정 요청만
        CREATION        // 생성 요청만
    }

    // 통합 응답 (CorrectionRequest 목록 + WorkRecord 목록)
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PendingApprovalResponse")
    public static class Response {
        private List<CorrectionRequestInfo> correctionRequests;  // 수정 요청 목록
        private List<WorkRecordCreationInfo> workRecordCreations; // 생성 요청 목록
    }

    // 수정 요청 정보
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CorrectionRequestInfo")
    public static class CorrectionRequestInfo {
        private Long id;                    // CorrectionRequest ID
        private Long workRecordId;          // WorkRecord ID
        private RequesterInfo requester;    // 요청자 정보
        private String workplaceName;       // 사업장 이름

        // 근무 정보
        private LocalDate originalWorkDate;    // 원본 근무 날짜
        private LocalTime originalStartTime;   // 원본 시작 시간
        private LocalTime originalEndTime;     // 원본 종료 시간
        private LocalDate requestedWorkDate;   // 요청 근무 날짜
        private LocalTime requestedStartTime;  // 요청 시작 시간
        private LocalTime requestedEndTime;    // 요청 종료 시간

        private String status;              // 상태
        private LocalDateTime createdAt;    // 요청 생성 시간

        // CorrectionRequest로부터 생성
        public static CorrectionRequestInfo from(CorrectionRequest request) {
            return CorrectionRequestInfo.builder()
                    .id(request.getId())
                    .workRecordId(request.getWorkRecord().getId())
                    .requester(RequesterInfo.from(request.getRequester()))
                    .workplaceName(request.getWorkRecord().getContract().getWorkplace().getName())
                    .originalWorkDate(request.getOriginalWorkDate())
                    .originalStartTime(request.getOriginalStartTime())
                    .originalEndTime(request.getOriginalEndTime())
                    .requestedWorkDate(request.getRequestedWorkDate())
                    .requestedStartTime(request.getRequestedStartTime())
                    .requestedEndTime(request.getRequestedEndTime())
                    .status(request.getStatus().name())
                    .createdAt(request.getCreatedAt())
                    .build();
        }
    }

    // 생성 요청 정보
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WorkRecordCreationInfo")
    public static class WorkRecordCreationInfo {
        private Long id;                    // WorkRecord ID
        private RequesterInfo requester;    // 요청자 정보
        private String workplaceName;       // 사업장 이름

        // 근무 정보
        private LocalDate requestedWorkDate;   // 요청 근무 날짜
        private LocalTime requestedStartTime;  // 요청 시작 시간
        private LocalTime requestedEndTime;    // 요청 종료 시간

        private String status;              // 상태
        private LocalDateTime createdAt;    // 요청 생성 시간

        // WorkRecord(PENDING_APPROVAL)로부터 생성
        public static WorkRecordCreationInfo from(WorkRecord workRecord) {
            return WorkRecordCreationInfo.builder()
                    .id(workRecord.getId())
                    .requester(RequesterInfo.from(workRecord.getContract().getWorker().getUser()))
                    .workplaceName(workRecord.getContract().getWorkplace().getName())
                    .requestedWorkDate(workRecord.getWorkDate())
                    .requestedStartTime(workRecord.getStartTime())
                    .requestedEndTime(workRecord.getEndTime())
                    .status(workRecord.getStatus().name())
                    .createdAt(workRecord.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RequesterInfo")
    public static class RequesterInfo {
        private Long id;
        private String name;

        public static RequesterInfo from(User user) {
            return RequesterInfo.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .build();
        }
    }
}
