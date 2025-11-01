package com.example.wagemanager.api.worker;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.worker.dto.WorkerDto;
import com.example.wagemanager.domain.worker.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @GetMapping("/{workerId}")
    public ApiResponse<WorkerDto.Response> getWorkerById(@PathVariable Long workerId) {
        return ApiResponse.success(workerService.getWorkerById(workerId));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<WorkerDto.Response> getWorkerByUserId(@PathVariable Long userId) {
        return ApiResponse.success(workerService.getWorkerByUserId(userId));
    }

    @GetMapping("/code/{workerCode}")
    public ApiResponse<WorkerDto.Response> getWorkerByCode(@PathVariable String workerCode) {
        return ApiResponse.success(workerService.getWorkerByWorkerCode(workerCode));
    }

    @PutMapping("/{workerId}")
    public ApiResponse<WorkerDto.Response> updateWorker(
            @PathVariable Long workerId,
            @RequestBody WorkerDto.UpdateRequest request) {
        return ApiResponse.success(workerService.updateWorker(workerId, request));
    }
}
