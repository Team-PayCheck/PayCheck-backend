package com.example.wagemanager.domain.workrecord.enums;

public enum WorkRecordStatus {
    SCHEDULED,        // 예정 (고용주가 생성하거나 승인된 일정)
    PENDING_APPROVAL, // 승인 대기 (근로자가 생성한 일정)
    COMPLETED         // 완료 (근무 후)
}
