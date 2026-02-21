package com.example.paycheck.api.notice;

import com.example.paycheck.common.dto.ApiResponse;
import com.example.paycheck.domain.notice.dto.NoticeDto;
import com.example.paycheck.domain.notice.service.NoticeService;
import com.example.paycheck.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "공지사항", description = "사업장 공지사항 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 작성", description = "사업장에 새로운 공지사항을 작성합니다.")
    @PreAuthorize("@permissionEvaluator.canAccessWorkplaceAsMember(#workplaceId)")
    @PostMapping("/workplaces/{workplaceId}/notices")
    public ResponseEntity<ApiResponse<NoticeDto.Response>> createNotice(
            @Parameter(description = "사업장 ID", required = true) @PathVariable Long workplaceId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NoticeDto.CreateRequest request) {
        NoticeDto.Response response = noticeService.createNotice(workplaceId, user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "공지사항 목록 조회", description = "사업장의 공지사항 목록을 조회합니다. 만료된 공지사항은 제외됩니다.")
    @PreAuthorize("@permissionEvaluator.canAccessWorkplaceAsMember(#workplaceId)")
    @GetMapping("/workplaces/{workplaceId}/notices")
    public ApiResponse<List<NoticeDto.ListResponse>> getNotices(
            @Parameter(description = "사업장 ID", required = true) @PathVariable Long workplaceId) {
        return ApiResponse.success(noticeService.getNotices(workplaceId));
    }

    @Operation(summary = "공지사항 단건 조회", description = "특정 공지사항의 상세 정보를 조회합니다.")
    @PreAuthorize("@permissionEvaluator.canAccessNotice(#noticeId)")
    @GetMapping("/notices/{noticeId}")
    public ApiResponse<NoticeDto.Response> getNotice(
            @Parameter(description = "공지사항 ID", required = true) @PathVariable Long noticeId) {
        return ApiResponse.success(noticeService.getNotice(noticeId));
    }

    @Operation(summary = "공지사항 수정", description = "공지사항을 수정합니다. 작성자만 수정할 수 있습니다.")
    @PreAuthorize("@permissionEvaluator.canAccessNotice(#noticeId)")
    @PutMapping("/notices/{noticeId}")
    public ApiResponse<NoticeDto.Response> updateNotice(
            @Parameter(description = "공지사항 ID", required = true) @PathVariable Long noticeId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NoticeDto.UpdateRequest request) {
        return ApiResponse.success(noticeService.updateNotice(noticeId, user, request));
    }

    @Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다. 작성자만 삭제할 수 있습니다.")
    @PreAuthorize("@permissionEvaluator.canAccessNotice(#noticeId)")
    @DeleteMapping("/notices/{noticeId}")
    public ApiResponse<Void> deleteNotice(
            @Parameter(description = "공지사항 ID", required = true) @PathVariable Long noticeId,
            @AuthenticationPrincipal User user) {
        noticeService.deleteNotice(noticeId, user);
        return ApiResponse.success(null);
    }
}
