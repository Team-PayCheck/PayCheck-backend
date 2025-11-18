package com.example.wagemanager.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class NotificationPageResponse {
    private List<NotificationResponse> notifications;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private long unreadCount;
}
