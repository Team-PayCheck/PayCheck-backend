package com.example.wagemanager.domain.notification.enums;

public enum NotificationType {
    SCHEDULE_CHANGE,            // 근무시간/일정 변경
    CORRECTION_RESPONSE,        // 근무기록 정정 요청 승인/거절
    PAYMENT_DUE,                // 월급일/급여 지급 예정
    PAYMENT_SUCCESS,            // 급여 입금 완료
    PAYMENT_FAILED,             // 급여 미입금/송금 실패
    INVITATION,                 // 근무지 초대
    RESIGNATION,                // 퇴사 처리
    UNREAD_CORRECTION_REQUEST   // 읽지 않은 정정 요청
}
