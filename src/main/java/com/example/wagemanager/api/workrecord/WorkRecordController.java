package com.example.wagemanager.api.workrecord;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.workrecord.dto.WorkRecordDto;
import com.example.wagemanager.domain.workrecord.service.WorkRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-records")
@RequiredArgsConstructor
public class WorkRecordController {

    private final WorkRecordService workRecordService;

    @GetMapping("/contract/{contractId}")
    public ApiResponse<List<WorkRecordDto.Response>> getWorkRecordsByContract(@PathVariable Long contractId) {
        return ApiResponse.success(workRecordService.getWorkRecordsByContract(contractId));
    }

    @GetMapping("/{workRecordId}")
    public ApiResponse<WorkRecordDto.Response> getWorkRecordById(@PathVariable Long workRecordId) {
        return ApiResponse.success(workRecordService.getWorkRecordById(workRecordId));
    }

    @PostMapping
    public ApiResponse<WorkRecordDto.Response> createWorkRecord(@Valid @RequestBody WorkRecordDto.CreateRequest request) {
        return ApiResponse.success(workRecordService.createWorkRecord(request));
    }

    @PutMapping("/{workRecordId}")
    public ApiResponse<WorkRecordDto.Response> updateWorkRecord(
            @PathVariable Long workRecordId,
            @RequestBody WorkRecordDto.UpdateRequest request) {
        return ApiResponse.success(workRecordService.updateWorkRecord(workRecordId, request));
    }
}
