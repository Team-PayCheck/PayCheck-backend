package com.example.wagemanager.domain.user.repository;

import com.example.wagemanager.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.kakaoId = :kakaoId")
    Optional<User> findByKakaoId(@Param("kakaoId") String kakaoId);

    boolean existsByKakaoId(String kakaoId);
}
