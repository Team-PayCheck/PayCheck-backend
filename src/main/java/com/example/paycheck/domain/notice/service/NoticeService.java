package com.example.paycheck.domain.notice.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.common.exception.UnauthorizedException;
import com.example.paycheck.domain.notice.dto.NoticeDto;
import com.example.paycheck.domain.notice.entity.Notice;
import com.example.paycheck.domain.notice.repository.NoticeRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final WorkplaceRepository workplaceRepository;

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
}
