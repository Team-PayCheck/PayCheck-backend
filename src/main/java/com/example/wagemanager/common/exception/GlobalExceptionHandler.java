package com.example.wagemanager.common.exception;

import com.example.wagemanager.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFoundException(NotFoundException e) {
        log.error("NotFoundException: {}", e.getErrorMessage());
        return ApiResponse.error(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleUnauthorizedException(UnauthorizedException e) {
        log.error("UnauthorizedException: {}", e.getErrorMessage());
        return ApiResponse.error(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBadRequestException(BadRequestException e) {
        log.error("BadRequestException: {}", e.getErrorMessage());
        return ApiResponse.error(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException: {}", e.getMessage());
        return ApiResponse.error("BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, SQLIntegrityConstraintViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDataIntegrityViolationException(Exception e) {
        log.error("DataIntegrityViolationException: {}", e.getMessage());

        String errorCode = ErrorCode.DATA_INTEGRITY_VIOLATION;
        String errorMessage = "데이터 무결성 제약 조건 위반: 중복된 데이터가 이미 존재합니다.";

        // 구체적인 중복 키 파싱
        String exceptionMessage = e.getMessage();
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("kakao_id") || exceptionMessage.contains("KAKAO_ID")) {
                errorCode = ErrorCode.DUPLICATE_KAKAO_ID;
                errorMessage = "이미 가입된 카카오 계정입니다.";
            } else if (exceptionMessage.contains("worker_code") || exceptionMessage.contains("WORKER_CODE")) {
                errorCode = ErrorCode.DUPLICATE_WORKER_CODE;
                errorMessage = "이미 사용 중인 근로자 코드입니다.";
            } else if (exceptionMessage.contains("business_number") || exceptionMessage.contains("BUSINESS_NUMBER")) {
                errorCode = ErrorCode.DUPLICATE_BUSINESS_NUMBER;
                errorMessage = "이미 등록된 사업자등록번호입니다.";
            } else if (exceptionMessage.contains("Duplicate entry")) {
                errorMessage = "중복된 데이터가 이미 존재합니다.";
            }
        }

        return ApiResponse.error(errorCode, errorMessage);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");
    }
}
