package com.example.wagemanager.common.exception;

/**
 * 애플리케이션 전역에서 사용하는 에러 코드 상수
 */
public class ErrorCode {

    // User domain
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";

    // Worker domain
    public static final String WORKER_NOT_FOUND = "WORKER_NOT_FOUND";

    // Employer domain
    public static final String EMPLOYER_NOT_FOUND = "EMPLOYER_NOT_FOUND";

    // Contract domain
    public static final String CONTRACT_NOT_FOUND = "CONTRACT_NOT_FOUND";

    // Workplace domain
    public static final String WORKPLACE_NOT_FOUND = "WORKPLACE_NOT_FOUND";

    // WorkRecord domain
    public static final String WORK_RECORD_NOT_FOUND = "WORK_RECORD_NOT_FOUND";

    // WeeklyAllowance domain
    public static final String WEEKLY_ALLOWANCE_NOT_FOUND = "WEEKLY_ALLOWANCE_NOT_FOUND";

    // Salary domain
    public static final String SALARY_NOT_FOUND = "SALARY_NOT_FOUND";

    // Payment domain
    public static final String PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND";

    // CorrectionRequest domain
    public static final String CORRECTION_REQUEST_NOT_FOUND = "CORRECTION_REQUEST_NOT_FOUND";

    // Notification domain
    public static final String NOTIFICATION_NOT_FOUND = "NOTIFICATION_NOT_FOUND";

    // RefreshToken domain
    public static final String REFRESH_TOKEN_NOT_FOUND = "REFRESH_TOKEN_NOT_FOUND";

    // Authentication & Authorization
    public static final String INVALID_ACCESS_TOKEN = "INVALID_ACCESS_TOKEN";
    public static final String INVALID_KAKAO_ID = "INVALID_KAKAO_ID";
    public static final String LOGIN_REQUIRED = "LOGIN_REQUIRED";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    public static final String EXPIRED_REFRESH_TOKEN = "EXPIRED_REFRESH_TOKEN";
    public static final String REFRESH_TOKEN_REQUIRED = "REFRESH_TOKEN_REQUIRED";
    public static final String KAKAO_AUTH_FAILED = "KAKAO_AUTH_FAILED";
    public static final String UNAUTHORIZED_ACCESS = "UNAUTHORIZED_ACCESS";
    public static final String WORKER_ONLY = "WORKER_ONLY";

    // Business Validation
    public static final String DUPLICATE_KAKAO_ACCOUNT = "DUPLICATE_KAKAO_ACCOUNT";
    public static final String WORKER_BANK_INFO_REQUIRED = "WORKER_BANK_INFO_REQUIRED";
    public static final String KAKAO_NAME_NOT_FOUND = "KAKAO_NAME_NOT_FOUND";
    public static final String INVALID_USER_TYPE = "INVALID_USER_TYPE";
    public static final String DUPLICATE_CONTRACT = "DUPLICATE_CONTRACT";
    public static final String WORK_DAY_CONVERSION_ERROR = "WORK_DAY_CONVERSION_ERROR";
    public static final String INVALID_WORK_RECORD_STATUS = "INVALID_WORK_RECORD_STATUS";
    public static final String DUPLICATE_CORRECTION_REQUEST = "DUPLICATE_CORRECTION_REQUEST";
    public static final String INVALID_CORRECTION_STATUS = "INVALID_CORRECTION_STATUS";
    public static final String INVALID_REQUEST_TYPE = "INVALID_REQUEST_TYPE";
    public static final String SALARY_NOT_CALCULATED = "SALARY_NOT_CALCULATED";
    public static final String PAYMENT_ALREADY_COMPLETED = "PAYMENT_ALREADY_COMPLETED";
    public static final String KAKAO_USER_INFO_FAILED = "KAKAO_USER_INFO_FAILED";
    public static final String KAKAO_SERVER_ERROR = "KAKAO_SERVER_ERROR";

    private ErrorCode() {
        // 인스턴스화 방지
    }
}
