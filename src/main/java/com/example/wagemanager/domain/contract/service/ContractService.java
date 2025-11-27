package com.example.wagemanager.domain.contract.service;

import com.example.wagemanager.domain.contract.dto.ContractDto;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.contract.repository.WorkerContractRepository;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.worker.repository.WorkerRepository;
import com.example.wagemanager.domain.workplace.entity.Workplace;
import com.example.wagemanager.domain.workplace.repository.WorkplaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final WorkerContractRepository contractRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkerRepository workerRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ContractDto.Response addWorkerToWorkplace(Long workplaceId, ContractDto.CreateRequest request) {
        // 사업장 조회
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(() -> new IllegalArgumentException("사업장을 찾을 수 없습니다."));

        // Worker 코드로 근로자 조회
        Worker worker = workerRepository.findByWorkerCode(request.getWorkerCode())
                .orElseThrow(() -> new IllegalArgumentException("근로자 코드를 찾을 수 없습니다: " + request.getWorkerCode()));

        // 이미 계약이 존재하는지 확인 (활성 상태인 계약)
        List<WorkerContract> existingContracts = contractRepository.findByWorkplaceIdAndIsActive(workplaceId, true);
        boolean alreadyContracted = existingContracts.stream()
                .anyMatch(contract -> contract.getWorker().getId().equals(worker.getId()));

        if (alreadyContracted) {
            throw new IllegalArgumentException("이미 해당 사업장에 계약이 존재하는 근로자입니다.");
        }

        // 계약 생성
        WorkerContract contract = WorkerContract.builder()
                .workplace(workplace)
                .worker(worker)
                .hourlyWage(request.getHourlyWage())
                .workDays(convertWorkDaysToJson(request.getWorkDays()))
                .contractStartDate(request.getContractStartDate())
                .contractEndDate(request.getContractEndDate())
                .paymentDay(request.getPaymentDay())
                .isActive(true)
                .build();

        return ContractDto.Response.from(contractRepository.save(contract));
    }

    public List<ContractDto.ListResponse> getContractsByWorkplaceId(Long workplaceId) {
        List<WorkerContract> contracts = contractRepository.findByWorkplaceIdAndIsActive(workplaceId, true);
        return contracts.stream()
                .map(ContractDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    public ContractDto.Response getContractById(Long contractId) {
        WorkerContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        return ContractDto.Response.from(contract);
    }

    @Transactional
    public ContractDto.Response updateContract(Long contractId, ContractDto.UpdateRequest request) {
        WorkerContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));

        String workDaysJson = request.getWorkDays() != null
                ? convertWorkDaysToJson(request.getWorkDays())
                : null;

        contract.update(
                request.getHourlyWage(),
                workDaysJson,
                request.getContractEndDate(),
                request.getPaymentDay()
        );

        return ContractDto.Response.from(contract);
    }

    @Transactional
    public void terminateContract(Long contractId) {
        WorkerContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));

        contract.terminate();
    }

    private String convertWorkDaysToJson(List<Integer> workDays) {
        try {
            return objectMapper.writeValueAsString(workDays);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("근무 요일 변환 중 오류가 발생했습니다.", e);
        }
    }
}
