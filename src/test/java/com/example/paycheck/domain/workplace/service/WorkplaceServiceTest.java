package com.example.paycheck.domain.workplace.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.employer.service.EmployerService;
import com.example.paycheck.domain.workplace.dto.WorkplaceDto;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkplaceService 테스트")
class WorkplaceServiceTest {

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private WorkerContractRepository workerContractRepository;

    @Mock
    private EmployerService employerService;

    @Mock
    private BusinessNumberVerificationService businessNumberVerificationService;

    @InjectMocks
    private WorkplaceService workplaceService;

    private Employer testEmployer;
    private Workplace testWorkplace;

    @BeforeEach
    void setUp() {
        testEmployer = Employer.builder()
                .id(1L)
                .phone("010-1234-5678")
                .build();

        testWorkplace = Workplace.builder()
                .id(1L)
                .employer(testEmployer)
                .businessNumber("123-45-67890")
                .businessName("테스트 사업체")
                .name("테스트 사업장")
                .address("서울시 강남구")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("사업장 생성 성공")
    void createWorkplace_Success() {
        // given
        WorkplaceDto.CreateRequest request = WorkplaceDto.CreateRequest.builder()
                .businessNumber("123-45-67890")
                .businessName("테스트 사업체")
                .name("테스트 사업장")
                .address("서울시 강남구")
                .colorCode("#FF0000")
                .build();

        when(employerService.getEmployerByUserId(1L)).thenReturn(testEmployer);
        when(workplaceRepository.save(any(Workplace.class))).thenReturn(testWorkplace);

        // when
        WorkplaceDto.Response result = workplaceService.createWorkplace(1L, request);

        // then
        assertThat(result).isNotNull();
        verify(employerService).getEmployerByUserId(1L);
        verify(businessNumberVerificationService).verifyBusinessNumber("123-45-67890");
        verify(workplaceRepository).save(any(Workplace.class));
    }

    @Test
    @DisplayName("사업장 생성 실패 - 사업자번호 검증 실패")
    void createWorkplace_Fail_InvalidBusinessNumber() {
        // given
        WorkplaceDto.CreateRequest request = WorkplaceDto.CreateRequest.builder()
                .businessNumber("123-45-67890")
                .businessName("테스트 사업체")
                .name("테스트 사업장")
                .address("서울시 강남구")
                .colorCode("#FF0000")
                .build();

        when(employerService.getEmployerByUserId(1L)).thenReturn(testEmployer);
        doThrow(new BadRequestException(ErrorCode.INVALID_BUSINESS_NUMBER, "유효하지 않은 사업자 등록번호입니다."))
                .when(businessNumberVerificationService)
                .verifyBusinessNumber("123-45-67890");

        // when & then
        assertThatThrownBy(() -> workplaceService.createWorkplace(1L, request))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_BUSINESS_NUMBER);

        verify(workplaceRepository, never()).save(any(Workplace.class));
    }

    @Test
    @DisplayName("사업장 ID로 조회 성공")
    void getWorkplaceById_Success() {
        // given
        when(workplaceRepository.findById(1L)).thenReturn(Optional.of(testWorkplace));

        // when
        WorkplaceDto.Response result = workplaceService.getWorkplaceById(1L);

        // then
        assertThat(result).isNotNull();
        verify(workplaceRepository).findById(1L);
    }

    @Test
    @DisplayName("사업장 ID로 조회 실패")
    void getWorkplaceById_NotFound() {
        // given
        when(workplaceRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workplaceService.getWorkplaceById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사업장을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("사용자 ID로 사업장 목록 조회 성공")
    void getWorkplacesByUserId_Success() {
        // given
        when(employerService.getEmployerByUserId(1L)).thenReturn(testEmployer);
        when(workplaceRepository.findByEmployerIdAndIsActive(1L, true))
                .thenReturn(Collections.singletonList(testWorkplace));
        when(workerContractRepository.countByWorkplaceIdAndIsActive(1L, true)).thenReturn(5);

        // when
        List<WorkplaceDto.ListResponse> result = workplaceService.getWorkplacesByUserId(1L);

        // then
        assertThat(result).isNotEmpty();
        verify(employerService).getEmployerByUserId(1L);
        verify(workplaceRepository).findByEmployerIdAndIsActive(1L, true);
    }

    @Test
    @DisplayName("사업장 정보 업데이트 성공")
    void updateWorkplace_Success() {
        // given
        WorkplaceDto.UpdateRequest request = WorkplaceDto.UpdateRequest.builder()
                .businessName("수정된 사업체")
                .name("수정된 사업장")
                .address("서울시 서초구")
                .colorCode("#00FF00")
                .isLessThanFiveEmployees(true)
                .build();

        when(workplaceRepository.findById(1L)).thenReturn(Optional.of(testWorkplace));

        // when
        WorkplaceDto.Response result = workplaceService.updateWorkplace(1L, request);

        // then
        assertThat(result).isNotNull();
        verify(workplaceRepository).findById(1L);
    }
}
