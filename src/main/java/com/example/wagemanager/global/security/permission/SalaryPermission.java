package com.example.wagemanager.global.security.permission;

import com.example.wagemanager.domain.salary.entity.Salary;
import com.example.wagemanager.domain.salary.repository.SalaryRepository;
import com.example.wagemanager.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("salaryPermission")
@RequiredArgsConstructor
public class SalaryPermission {

    private final SalaryRepository salaryRepository;
    private final WorkplacePermission workplacePermission;
    private final ContractPermission contractPermission;

    public boolean canAccess(Long salaryId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return false;
        }

        User currentUser = (User) authentication.getPrincipal();
        Salary salary = salaryRepository.findById(salaryId).orElse(null);

        if (salary == null) {
            return false;
        }

        return salary.getContract().getWorkplace().getEmployer().getUser().getId().equals(currentUser.getId());
    }

    public boolean canAccessAsWorker(Long salaryId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return false;
        }

        User currentUser = (User) authentication.getPrincipal();
        Salary salary = salaryRepository.findById(salaryId).orElse(null);

        if (salary == null) {
            return false;
        }

        return salary.getContract().getWorker().getUser().getId().equals(currentUser.getId());
    }

    public boolean canAccessWorkplaceSalaries(Long workplaceId) {
        return workplacePermission.canAccess(workplaceId);
    }

    public boolean canCalculateForContract(Long contractId) {
        return contractPermission.canAccessAsEmployer(contractId) || contractPermission.canAccessAsWorker(contractId);
    }
}
