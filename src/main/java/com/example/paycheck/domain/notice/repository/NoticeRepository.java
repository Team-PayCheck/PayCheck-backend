package com.example.paycheck.domain.notice.repository;

import com.example.paycheck.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT n FROM Notice n " +
           "JOIN FETCH n.workplace " +
           "JOIN FETCH n.author " +
           "WHERE n.workplace.id = :workplaceId " +
           "AND n.isDeleted = false " +
           "AND n.expiresAt > :now " +
           "ORDER BY n.createdAt DESC")
    List<Notice> findActiveNoticesByWorkplaceId(
            @Param("workplaceId") Long workplaceId,
            @Param("now") LocalDateTime now);

    @Query("SELECT n FROM Notice n " +
           "JOIN FETCH n.workplace " +
           "JOIN FETCH n.author " +
           "WHERE n.id = :noticeId " +
           "AND n.isDeleted = false")
    Optional<Notice> findByIdAndIsDeletedFalse(@Param("noticeId") Long noticeId);
}
