package com.example.wagemanager.domain.notification.repository;

import com.example.wagemanager.domain.notification.entity.Notification;
import com.example.wagemanager.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.user u " +
            "WHERE u = :user " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.user u " +
            "WHERE u = :user AND n.isRead = false " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUser(@Param("user") User user);

    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.user u " +
            "WHERE u = :user")
    Page<Notification> findByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.user u " +
            "WHERE u = :user AND n.isRead = :isRead")
    Page<Notification> findByUserAndIsRead(@Param("user") User user, @Param("isRead") Boolean isRead, Pageable pageable);

    long countByUserAndIsReadFalse(User user);

    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.user u " +
            "WHERE n.id = :id AND u = :user")
    Optional<Notification> findByIdAndUser(@Param("id") Long id, @Param("user") User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadByUser(@Param("user") User user, @Param("readAt") LocalDateTime readAt);
}
