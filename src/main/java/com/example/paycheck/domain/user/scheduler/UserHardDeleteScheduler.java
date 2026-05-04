package com.example.paycheck.domain.user.scheduler;

import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.repository.UserRepository;
import com.example.paycheck.domain.user.service.UserHardDeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탈퇴 후 30일이 경과한 사용자 데이터를 영구 삭제하는 스케줄러.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserHardDeleteScheduler {

    private static final int RETENTION_DAYS = 30;

    private final UserRepository userRepository;
    private final UserHardDeleteService userHardDeleteService;

    /**
     * 매일 새벽 4시에 30일 경과 탈퇴 사용자 영구 삭제
     * cron: 초 분 시 일 월 요일
     * "0 0 4 * * *" = 매일 새벽 4시 정각
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void hardDeleteWithdrawnUsers() {
        log.info("===== 탈퇴 사용자 영구 삭제 스케줄러 시작 =====");

        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<User> targets = userRepository.findAllByDeletedAtBefore(threshold);

        if (targets.isEmpty()) {
            log.info("영구 삭제 대상 사용자가 없습니다.");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (User user : targets) {
            try {
                userHardDeleteService.hardDeleteUser(user.getId());
                successCount++;
            } catch (Exception e) {
                log.error("사용자 영구 삭제 실패: userId={}, error={}", user.getId(), e.getMessage(), e);
                failCount++;
            }
        }

        log.info("===== 탈퇴 사용자 영구 삭제 스케줄러 완료 ===== (대상: {}, 성공: {}, 실패: {})",
                targets.size(), successCount, failCount);
    }
}
