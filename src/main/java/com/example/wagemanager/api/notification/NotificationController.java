package com.example.wagemanager.api.notification;

import com.example.wagemanager.common.dto.ApiResponse;
import com.example.wagemanager.domain.notification.dto.NotificationPageResponse;
import com.example.wagemanager.domain.notification.service.NotificationService;
import com.example.wagemanager.domain.notification.service.SseEmitterService;
import com.example.wagemanager.domain.user.entity.User;
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

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @GetMapping
    public ApiResponse<NotificationPageResponse> getMyNotifications(
        @AuthenticationPrincipal User user,
        @RequestParam(value = "is_read", required = false) Boolean is_read,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ){
        int p = (page == null) ? 1 : page;
        int s = (size == null) ? 20 : size;
        NotificationPageResponse resp = notificationService.getNotifications(user, is_read, p, s);
        return ApiResponse.success(resp);
    }

    /**
     * SSE 실시간 알림 구독
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal User user) {
        return sseEmitterService.createEmitter(user.getId());
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal User user) {
        return ApiResponse.success(notificationService.getUnreadCount(user));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        notificationService.markAsReadById(user, id);
        // 읽음 처리 후 읽지 않은 알림 개수 전송
        sseEmitterService.sendUnreadCount(user.getId(), notificationService.getUnreadCount(user));
        return ApiResponse.success(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user);
        // 전체 읽음 처리 후 읽지 않은 알림 개수 전송
        sseEmitterService.sendUnreadCount(user.getId(), 0L);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        notificationService.deleteNotification(user, id);
        // 삭제 후 읽지 않은 알림 개수 전송
        sseEmitterService.sendUnreadCount(user.getId(), notificationService.getUnreadCount(user));
        return ApiResponse.success(null);
    }
}
