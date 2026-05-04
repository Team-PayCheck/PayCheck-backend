package com.example.paycheck.domain.notice.repository;

import com.example.paycheck.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * 영구 삭제용: 특정 작성자가 작성한 모든 공지 일괄 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notice n WHERE n.author.id = :userId")
    void deleteAllByAuthorId(@Param("userId") Long userId);

    /**
     * 영구 삭제용: 여러 사업장에 속한 모든 공지 일괄 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notice n WHERE n.workplace.id IN :workplaceIds")
    void deleteAllByWorkplaceIdIn(@Param("workplaceIds") List<Long> workplaceIds);
}
