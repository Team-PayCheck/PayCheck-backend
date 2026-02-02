package com.example.paycheck.domain.workplace.service;

import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.employer.service.EmployerService;
import com.example.paycheck.domain.workplace.dto.WorkplaceDto;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkplaceService {

    private final WorkplaceRepository workplaceRepository;
    private final WorkerContractRepository workerContractRepository;
    private final EmployerService employerService;
    private final BusinessNumberVerificationService businessNumberVerificationService;

    @Transactional
    public WorkplaceDto.Response createWorkplace(Long userId, WorkplaceDto.CreateRequest request) {
        Employer employer = employerService.getEmployerByUserId(userId);

        businessNumberVerificationService.verifyBusinessNumber(request.getBusinessNumber());

        Workplace workplace = Workplace.builder()
                .employer(employer)
                .businessNumber(request.getBusinessNumber())
                .businessName(request.getBusinessName())
                .name(request.getName())
                .address(request.getAddress())
                .colorCode(request.getColorCode())
                .isActive(true)
                .build();

        Workplace saved = workplaceRepository.save(workplace);
        return WorkplaceDto.Response.from(saved);
    }

    public List<WorkplaceDto.ListResponse> getWorkplacesByUserId(Long userId) {
        Employer employer = employerService.getEmployerByUserId(userId);

        List<Workplace> workplaces = workplaceRepository.findByEmployerIdAndIsActive(employer.getId(), true);

        return workplaces.stream()
                .map(workplace -> {
                    Integer workerCount = workerContractRepository.countByWorkplaceIdAndIsActive(workplace.getId(), true);
                    return WorkplaceDto.ListResponse.from(workplace, workerCount);
                })
                .collect(Collectors.toList());
    }

    public WorkplaceDto.Response getWorkplaceById(Long workplaceId) {
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKPLACE_NOT_FOUND, "사업장을 찾을 수 없습니다."));
        return WorkplaceDto.Response.from(workplace);
    }

    @Transactional
    public WorkplaceDto.Response updateWorkplace(Long workplaceId, WorkplaceDto.UpdateRequest request) {
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKPLACE_NOT_FOUND, "사업장을 찾을 수 없습니다."));

        workplace.update(
                request.getBusinessName(),
                request.getName(),
                request.getAddress(),
                request.getColorCode(),
                request.getIsLessThanFiveEmployees()
        );

        return WorkplaceDto.Response.from(workplace);
    }

    @Transactional
    public void deactivateWorkplace(Long workplaceId) {
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKPLACE_NOT_FOUND, "사업장을 찾을 수 없습니다."));

        workplace.deactivate();
    }
}
