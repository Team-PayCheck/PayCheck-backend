package com.example.paycheck.domain.notice.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.common.exception.UnauthorizedException;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.notification.event.NotificationEvent;
import com.example.paycheck.domain.notice.dto.NoticeDto;
import com.example.paycheck.domain.notice.entity.Notice;
import com.example.paycheck.domain.notice.repository.NoticeRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkerContractRepository contractRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public NoticeDto.Response createNotice(Long workplaceId, User author, NoticeDto.CreateRequest request) {
        if (!request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "만료 일시는 현재 시간 이후여야 합니다.");
        }

        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKPLACE_NOT_FOUND, "사업장을 찾을 수 없습니다."));

        Notice notice = Notice.builder()
                .workplace(workplace)
                .author(author)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .expiresAt(request.getExpiresAt())
                .build();

        Notice saved = noticeRepository.save(notice);
        try {
            publishNoticeCreatedNotification(saved);
        } catch (Exception e) {
            log.error("공지사항 생성은 성공했지만 알림 발행에 실패했습니다. noticeId={}, workplaceId={}",
                    saved.getId(), workplaceId, e);
        }
        return NoticeDto.Response.from(saved);
    }

    public List<NoticeDto.ListResponse> getNotices(Long workplaceId) {
        workplaceRepository.findById(workplaceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WORKPLACE_NOT_FOUND, "사업장을 찾을 수 없습니다."));

        return noticeRepository.findActiveNoticesByWorkplaceId(workplaceId, LocalDateTime.now())
                .stream()
                .map(NoticeDto.ListResponse::from)
                .toList();
    }

    public NoticeDto.Response getNotice(Long noticeId) {
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(noticeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOTICE_NOT_FOUND, "공지사항을 찾을 수 없습니다."));

        return NoticeDto.Response.from(notice);
    }

    @Transactional
    public NoticeDto.Response updateNotice(Long noticeId, User user, NoticeDto.UpdateRequest request) {
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "만료 일시는 현재 시간 이후여야 합니다.");
        }

        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(noticeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOTICE_NOT_FOUND, "공지사항을 찾을 수 없습니다."));

        validateAuthor(notice, user);

        notice.update(request.getCategory(), request.getTitle(), request.getContent(), request.getExpiresAt());
        return NoticeDto.Response.from(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId, User user) {
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(noticeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOTICE_NOT_FOUND, "공지사항을 찾을 수 없습니다."));

        validateAuthor(notice, user);

        notice.delete();
    }

    private void validateAuthor(Notice notice, User user) {
        if (!notice.getAuthor().getId().equals(user.getId())) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ACCESS, "공지사항 작성자만 수정/삭제할 수 있습니다.");
        }
    }

    private void publishNoticeCreatedNotification(Notice notice) {
        String title = String.format("[%s] 새로운 공지사항이 등록되었습니다: %s",
                notice.getWorkplace().getName(), notice.getTitle());
        String actionData = buildActionData(notice.getId(), notice.getWorkplace().getId());
        Long authorId = notice.getAuthor().getId();

        Set<Long> notifiedUserIds = new HashSet<>();
        User employerUser = null;
        if (notice.getWorkplace() != null && notice.getWorkplace().getEmployer() != null) {
            employerUser = notice.getWorkplace().getEmployer().getUser();
        }

        publishNoticeNotificationIfNeeded(
                employerUser,
                authorId,
                title,
                actionData,
                notifiedUserIds
        );

        List<WorkerContract> activeContracts = contractRepository
                .findByWorkplaceIdAndIsActive(notice.getWorkplace().getId(), true);

        for (WorkerContract contract : activeContracts) {
            User workerUser = contract.getWorker() != null ? contract.getWorker().getUser() : null;
            publishNoticeNotificationIfNeeded(
                    workerUser,
                    authorId,
                    title,
                    actionData,
                    notifiedUserIds
            );
        }
    }

    private void publishNoticeNotificationIfNeeded(
            User recipient,
            Long authorId,
            String title,
            String actionData,
            Set<Long> notifiedUserIds
    ) {
        if (recipient == null || recipient.getId() == null) {
            return;
        }
        if (recipient.getId().equals(authorId) || !notifiedUserIds.add(recipient.getId())) {
            return;
        }

        NotificationEvent event = NotificationEvent.builder()
                .user(recipient)
                .type(NotificationType.NOTICE_CREATED)
                .title(title)
                .actionType(NotificationActionType.VIEW_NOTICE)
                .actionData(actionData)
                .build();

        eventPublisher.publishEvent(event);
    }

    private String buildActionData(Long noticeId, Long workplaceId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("noticeId", noticeId);
            data.put("workplaceId", workplaceId);
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("공지사항 알림 actionData 생성 실패: noticeId={}, workplaceId={}", noticeId, workplaceId, e);
            return "{}";
        }
    }
}
