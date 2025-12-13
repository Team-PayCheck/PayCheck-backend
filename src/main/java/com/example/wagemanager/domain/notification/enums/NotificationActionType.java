package com.example.wagemanager.domain.notification.enums;

/**
 * 알림 클릭 시 수행할 액션 타입
 */
public enum NotificationActionType {
    /**
     * 정정 요청 상세 조회 및 승인/거절
     * - 고용주: 정정 요청 승인/거절 화면으로 이동
     * - 근로자: 정정 요청 상세 조회 화면으로 이동
     */
    VIEW_CORRECTION_REQUEST,

    /**
     * 근무 기록 상세 조회
     * - 변경된 근무 일정 확인
     */
    VIEW_WORK_RECORD,

    /**
     * 근무 일정 승인/거절
     * - 고용주: 근로자가 요청한 근무 일정 승인/거절 화면으로 이동
     */
    VIEW_PENDING_APPROVAL,

    /**
     * 급여 상세 조회
     * - 급여 명세서 확인 (근로자)
     */
    VIEW_SALARY,

    /**
     * 송금 관리 페이지로 이동
     * - 급여 지급일 알림 시 고용주가 송금 처리할 수 있도록
     */
    VIEW_PAYMENT_MANAGEMENT,

    /**
     * 근무지 초대 확인
     * - 근무지 초대 수락/거절 화면으로 이동
     */
    VIEW_WORKPLACE_INVITATION,

    /**
     * 액션 없음
     * - 단순 정보성 알림
     */
    NONE
}
