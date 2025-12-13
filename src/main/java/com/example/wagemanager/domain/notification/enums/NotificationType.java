package com.example.wagemanager.domain.notification.enums;

public enum NotificationType {
    SCHEDULE_CHANGE,            // 근무시간/일정 변경
    SCHEDULE_CREATED,           // 근무 일정 생성 (고용주가 생성 시)
    SCHEDULE_APPROVAL_REQUEST,  // 근무 일정 승인 요청 (근로자가 생성 시)
    SCHEDULE_APPROVED,          // 근무 일정 승인됨 (고용주가 승인 시)
    SCHEDULE_REJECTED,          // 근무 일정 거절됨 (고용주가 거절 시)
    SCHEDULE_DELETED,           // 근무 일정 삭제됨 (고용주가 삭제 시)
    CORRECTION_RESPONSE,        // 근무기록 정정 요청 승인/거절
    PAYMENT_DUE,                // 월급일/급여 지급 예정
    PAYMENT_SUCCESS,            // 급여 입금 완료
    PAYMENT_FAILED,             // 급여 미입금/송금 실패
    INVITATION,                 // 근무지 초대
    RESIGNATION,                // 퇴사 처리
    UNREAD_CORRECTION_REQUEST   // 읽지 않은 정정 요청
}
