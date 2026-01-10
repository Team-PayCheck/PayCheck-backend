package com.example.wagemanager.common.exception;

import com.example.wagemanager.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("DataIntegrityViolationException - 기본 메시지")
    void handleDataIntegrityViolationException_DefaultMessage() {
        // given
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Duplicate entry");

        // when
        ApiResponse<Void> response = exceptionHandler.handleDataIntegrityViolationException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo(ErrorCode.DATA_INTEGRITY_VIOLATION);
        assertThat(response.getError().getMessage()).isEqualTo("중복된 데이터가 이미 존재합니다.");
    }

    @Test
    @DisplayName("DataIntegrityViolationException - 카카오 ID 중복")
    void handleDataIntegrityViolationException_DuplicateKakaoId() {
        // given
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate entry '12345' for key 'kakao_id'"
        );

        // when
        ApiResponse<Void> response = exceptionHandler.handleDataIntegrityViolationException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo(ErrorCode.DUPLICATE_KAKAO_ID);
        assertThat(response.getError().getMessage()).isEqualTo("이미 가입된 카카오 계정입니다.");
    }

    @Test
    @DisplayName("DataIntegrityViolationException - 근로자 코드 중복")
    void handleDataIntegrityViolationException_DuplicateWorkerCode() {
        // given
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate entry 'ABC123' for key 'worker_code'"
        );

        // when
        ApiResponse<Void> response = exceptionHandler.handleDataIntegrityViolationException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo(ErrorCode.DUPLICATE_WORKER_CODE);
        assertThat(response.getError().getMessage()).isEqualTo("이미 사용 중인 근로자 코드입니다.");
    }

    @Test
    @DisplayName("DataIntegrityViolationException - 사업자등록번호 중복")
    void handleDataIntegrityViolationException_DuplicateBusinessNumber() {
        // given
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate entry '123-45-67890' for key 'business_number'"
        );

        // when
        ApiResponse<Void> response = exceptionHandler.handleDataIntegrityViolationException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        assertThat(response.getError().getMessage()).isEqualTo("이미 등록된 사업자등록번호입니다.");
    }

    @Test
    @DisplayName("SQLIntegrityConstraintViolationException 처리")
    void handleSQLIntegrityConstraintViolationException() {
        // given
        SQLIntegrityConstraintViolationException exception = new SQLIntegrityConstraintViolationException(
                "Duplicate entry '12345' for key 'kakao_id'"
        );

        // when
        ApiResponse<Void> response = exceptionHandler.handleDataIntegrityViolationException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo(ErrorCode.DUPLICATE_KAKAO_ID);
        assertThat(response.getError().getMessage()).isEqualTo("이미 가입된 카카오 계정입니다.");
    }

    @Test
    @DisplayName("NotFoundException 처리")
    void handleNotFoundException() {
        // given
        NotFoundException exception = new NotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");

        // when
        ApiResponse<Void> response = exceptionHandler.handleNotFoundException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(response.getError().getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("UnauthorizedException 처리")
    void handleUnauthorizedException() {
        // given
        UnauthorizedException exception = new UnauthorizedException(ErrorCode.LOGIN_REQUIRED, "로그인이 필요합니다.");

        // when
        ApiResponse<Void> response = exceptionHandler.handleUnauthorizedException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo(ErrorCode.LOGIN_REQUIRED);
        assertThat(response.getError().getMessage()).isEqualTo("로그인이 필요합니다.");
    }

    @Test
    @DisplayName("BadRequestException 처리")
    void handleBadRequestException() {
        // given
        BadRequestException exception = new BadRequestException(ErrorCode.INVALID_USER_TYPE, "잘못된 사용자 타입입니다.");

        // when
        ApiResponse<Void> response = exceptionHandler.handleBadRequestException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo(ErrorCode.INVALID_USER_TYPE);
        assertThat(response.getError().getMessage()).isEqualTo("잘못된 사용자 타입입니다.");
    }

    @Test
    @DisplayName("IllegalArgumentException 처리")
    void handleIllegalArgumentException() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("잘못된 인자입니다.");

        // when
        ApiResponse<Void> response = exceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo("BAD_REQUEST");
        assertThat(response.getError().getMessage()).isEqualTo("잘못된 인자입니다.");
    }

    @Test
    @DisplayName("일반 Exception 처리")
    void handleException() {
        // given
        Exception exception = new Exception("예상치 못한 오류");

        // when
        ApiResponse<Void> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getError().getMessage()).isEqualTo("서버 오류가 발생했습니다.");
    }
}
