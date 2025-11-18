package com.example.wagemanager.domain.notification.event;

import com.example.wagemanager.domain.notification.enums.NotificationActionType;
import com.example.wagemanager.domain.notification.enums.NotificationType;
import com.example.wagemanager.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.lang.Nullable;

@Getter
@Builder
@AllArgsConstructor
public class NotificationEvent {
    @NonNull
    private final User user;
    @NonNull
    private final NotificationType type;
    @NonNull
    private final String title;
    @NonNull
    private final NotificationActionType actionType;
    @Nullable
    private final String actionData;
}
