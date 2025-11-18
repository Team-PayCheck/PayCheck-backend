package com.example.wagemanager.domain.notification.service;

import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.example.wagemanager.domain.notification.dto.NotificationResponse;
import com.example.wagemanager.domain.notification.dto.NotificationPageResponse;
import com.example.wagemanager.domain.notification.repository.NotificationRepository;
import com.example.wagemanager.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotificationDtos(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(User user, Boolean isRead, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by("createdAt").descending());
        Page<Notification> p;
        if (isRead == null) {
            p = notificationRepository.findByUser(user, pageable);
        } else {
            p = notificationRepository.findByUserAndIsRead(user, isRead, pageable);
        }

        List<NotificationResponse> dtoList = p.getContent().stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        long unreadCount = notificationRepository.countByUserAndIsReadFalse(user);

        return NotificationPageResponse.builder()
                .notifications(dtoList)
                .page(page)
                .size(size)
                .totalPages(p.getTotalPages())
                .totalElements(p.getTotalElements())
                .unreadCount(unreadCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findUnreadByUser(user);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotificationDtos(User user) {
        return notificationRepository.findUnreadByUser(user).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Notification notification) {
        notification.markAsRead();
        // JPA dirty checking으로 자동 저장으로 save 호출 불필요
    }

    @Transactional
    public void markAsReadById(User user, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."));
        n.markAsRead();
        // JPA dirty checking으로 자동 저장으로 save 호출 불필요
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadByUser(user, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(User user, Long notificationId) {
        notificationRepository.findByIdAndUser(notificationId, user)
                .ifPresentOrElse(
                        notificationRepository::delete,
                        () -> { throw new NotFoundException("NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."); }
                );
    }
}
