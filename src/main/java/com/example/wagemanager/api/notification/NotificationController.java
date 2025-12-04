package com.example.wagemanager.api.notification;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.notification.dto.NotificationPageResponse;
import com.example.wagemanager.domain.notification.service.NotificationService;
import com.example.wagemanager.domain.notification.service.SseEmitterService;
import com.example.wagemanager.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "알림", description = "실시간 알림 및 알림 관리 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @Operation(summary = "내 알림 목록 조회", description = "로그인한 사용자의 알림 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ApiResponse<NotificationPageResponse> getMyNotifications(
        @AuthenticationPrincipal User user,
        @Parameter(description = "읽음 여부 필터 (true: 읽은 알림만, false: 읽지 않은 알림만)") @RequestParam(value = "is_read", required = false) Boolean is_read,
        @Parameter(description = "페이지 번호 (기본값: 1)") @RequestParam(value = "page", required = false) Integer page,
        @Parameter(description = "페이지 크기 (기본값: 20)") @RequestParam(value = "size", required = false) Integer size
    ){
        int p = (page == null) ? 1 : page;
        int s = (size == null) ? 20 : size;
        NotificationPageResponse resp = notificationService.getNotifications(user, is_read, p, s);
        return ApiResponse.success(resp);
    }

    @Operation(summary = "실시간 알림 구독 (SSE)", description = "Server-Sent Events를 통해 실시간 알림을 구독합니다.")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal User user) {
        return sseEmitterService.createEmitter(user.getId());
    }

    @Operation(summary = "읽지 않은 알림 개수 조회", description = "로그인한 사용자의 읽지 않은 알림 개수를 조회합니다.")
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal User user) {
        return ApiResponse.success(notificationService.getUnreadCount(user));
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @Parameter(description = "알림 ID", required = true) @PathVariable("id") Long id) {
        notificationService.markAsReadById(user, id);
        sseEmitterService.sendUnreadCount(user.getId(), notificationService.getUnreadCount(user));
        return ApiResponse.success(null);
    }

    @Operation(summary = "모든 알림 읽음 처리", description = "로그인한 사용자의 모든 알림을 읽음 상태로 변경합니다.")
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user);
        sseEmitterService.sendUnreadCount(user.getId(), 0L);
        return ApiResponse.success(null);
    }

    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(
            @AuthenticationPrincipal User user,
            @Parameter(description = "알림 ID", required = true) @PathVariable("id") Long id) {
        notificationService.deleteNotification(user, id);
        sseEmitterService.sendUnreadCount(user.getId(), notificationService.getUnreadCount(user));
        return ApiResponse.success(null);
    }
}
