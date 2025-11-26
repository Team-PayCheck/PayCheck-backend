package com.example.wagemanager.api.employer;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workplace.dto.WorkplaceDto;
import com.example.wagemanager.domain.workplace.service.WorkplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employer/workplaces")
@RequiredArgsConstructor
public class WorkplaceController {

    private final WorkplaceService workplaceService;

    @PostMapping
    public ApiResponse<WorkplaceDto.Response> createWorkplace(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WorkplaceDto.CreateRequest request) {
        return ApiResponse.success(workplaceService.createWorkplace(user.getId(), request));
    }

    @GetMapping
    public ApiResponse<List<WorkplaceDto.ListResponse>> getWorkplaces(
            @AuthenticationPrincipal User user) {
        return ApiResponse.success(workplaceService.getWorkplacesByUserId(user.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkplaceDto.Response> getWorkplace(@PathVariable Long id) {
        return ApiResponse.success(workplaceService.getWorkplaceById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<WorkplaceDto.Response> updateWorkplace(
            @PathVariable Long id,
            @Valid @RequestBody WorkplaceDto.UpdateRequest request) {
        return ApiResponse.success(workplaceService.updateWorkplace(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivateWorkplace(@PathVariable Long id) {
        workplaceService.deactivateWorkplace(id);
        return ApiResponse.success(null);
    }
}
