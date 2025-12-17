package com.example.wagemanager.domain.workrecord.deletion.enums;

public enum WorkRecordDeletionRequestStatus {
    PENDING,    // 근로자가 삭제 요청 제출
    APPROVED,   // 고용주가 삭제 처리 완료
    REJECTED,   // 고용주가 삭제 요청 거절
    CANCELLED   // 근로자가 요청 취소
}
