package com.example.paycheck.domain.user.repository;

import com.example.paycheck.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.kakaoId = :kakaoId")
    Optional<User> findByKakaoId(@Param("kakaoId") String kakaoId);

    boolean existsByKakaoId(String kakaoId);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL AND u.deletedAt < :threshold")
    List<User> findAllByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}
