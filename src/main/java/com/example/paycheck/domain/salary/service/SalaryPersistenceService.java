package com.example.paycheck.domain.salary.service;

import com.example.paycheck.domain.salary.entity.Salary;
import com.example.paycheck.domain.salary.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 급여 저장 전용 서비스
 * REQUIRES_NEW 전파 속성으로 독립된 트랜잭션에서 저장을 시도하여
 * 중복 키 예외 발생 시 메인 트랜잭션에 영향을 주지 않도록 함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryPersistenceService {

    private final SalaryRepository salaryRepository;

    /**
     * 새로운 트랜잭션에서 급여 저장 시도
     * 중복 키 예외 발생 시 예외를 던짐 (호출 측에서 처리, 메인 트랜잭션은 영향받지 않음)
     *
     * @param salary 저장할 급여 엔티티
     * @return 저장된 급여 엔티티
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Salary trySave(Salary salary) {
        return salaryRepository.save(salary);
    }
}
