package com.example.paycheck.domain.notice.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.common.exception.UnauthorizedException;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.notification.enums.NotificationActionType;
import com.example.paycheck.domain.notification.enums.NotificationType;
import com.example.paycheck.domain.notification.event.NotificationEvent;
import com.example.paycheck.domain.notice.dto.NoticeDto;
import com.example.paycheck.domain.notice.entity.Notice;
import com.example.paycheck.domain.notice.enums.NoticeCategory;
import com.example.paycheck.domain.notice.repository.NoticeRepository;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeService 테스트")
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private WorkerContractRepository contractRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NoticeService noticeService;

    private User authorUser;
    private User otherUser;
    private Worker otherWorker;
    private WorkerContract activeContract;
    private Employer employer;
    private Workplace workplace;
    private Notice notice;
    private LocalDateTime futureExpiry;

    @BeforeEach
    void setUp() {
        authorUser = User.builder()
                .id(1L)
                .kakaoId("kakao-author")
                .name("작성자")
                .userType(UserType.EMPLOYER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .kakaoId("kakao-other")
                .name("다른 사용자")
                .userType(UserType.WORKER)
                .build();

        otherWorker = Worker.builder()
                .id(10L)
                .user(otherUser)
                .workerCode("ABC123")
                .build();

        employer = Employer.builder()
                .id(1L)
                .user(authorUser)
                .build();

        workplace = Workplace.builder()
                .id(1L)
                .employer(employer)
                .businessNumber("123-45-67890")
                .businessName("테스트 사업체")
                .name("테스트 사업장")
                .isActive(true)
                .build();

        futureExpiry = LocalDateTime.now().plusDays(7);

        notice = Notice.builder()
                .id(1L)
                .workplace(workplace)
                .author(authorUser)
                .category(NoticeCategory.URGENT)
                .title("긴급 공지")
                .content("위생사항 엄수")
                .expiresAt(futureExpiry)
                .build();

        activeContract = WorkerContract.builder()
                .id(100L)
                .workplace(workplace)
                .worker(otherWorker)
                .isActive(true)
                .build();
    }

    // ==================== createNotice ====================

    @Test
    @DisplayName("공지사항 작성 성공")
    void createNotice_Success() {
        // given
        NoticeDto.CreateRequest request = NoticeDto.CreateRequest.builder()
                .category(NoticeCategory.URGENT)
                .title("긴급 공지")
                .content("위생사항 엄수")
                .expiresAt(futureExpiry)
                .build();

        when(workplaceRepository.findById(1L)).thenReturn(Optional.of(workplace));
        when(noticeRepository.save(any())).thenReturn(notice);
        when(contractRepository.findByWorkplaceIdAndIsActive(1L, true))
                .thenReturn(List.of(activeContract));

        // when
        NoticeDto.Response result = noticeService.createNotice(1L, authorUser, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("긴급 공지");
        assertThat(result.getCategory()).isEqualTo(NoticeCategory.URGENT);
        assertThat(result.getAuthorName()).isEqualTo("작성자");
        verify(workplaceRepository).findById(1L);
        verify(noticeRepository).save(any());
        verify(contractRepository).findByWorkplaceIdAndIsActive(1L, true);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        NotificationEvent event = eventCaptor.getValue();
        assertThat(event.getUser().getId()).isEqualTo(otherUser.getId());
        assertThat(event.getType()).isEqualTo(NotificationType.NOTICE_CREATED);
        assertThat(event.getActionType()).isEqualTo(NotificationActionType.VIEW_NOTICE);
        assertThat(event.getActionData()).contains("\"noticeId\":1");
        assertThat(event.getActionData()).contains("\"workplaceId\":1");
    }

    @Test
    @DisplayName("공지사항 작성 실패 - 존재하지 않는 사업장")
    void createNotice_Fail_WorkplaceNotFound() {
        // given
        NoticeDto.CreateRequest request = NoticeDto.CreateRequest.builder()
                .category(NoticeCategory.ETC)
                .title("공지")
                .content("내용")
                .expiresAt(futureExpiry)
                .build();

        when(workplaceRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.createNotice(999L, authorUser, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사업장을 찾을 수 없습니다");

        verify(noticeRepository, never()).save(any());
    }

    @Test
    @DisplayName("공지사항 작성 성공 - 알림 발행 실패 시에도 롤백되지 않음")
    void createNotice_Success_WhenNotificationPublishFails() {
        // given
        NoticeDto.CreateRequest request = NoticeDto.CreateRequest.builder()
                .category(NoticeCategory.URGENT)
                .title("긴급 공지")
                .content("위생사항 엄수")
                .expiresAt(futureExpiry)
                .build();

        when(workplaceRepository.findById(1L)).thenReturn(Optional.of(workplace));
        when(noticeRepository.save(any())).thenReturn(notice);
        when(contractRepository.findByWorkplaceIdAndIsActive(1L, true))
                .thenReturn(List.of(activeContract));
        doThrow(new RuntimeException("알림 발행 실패"))
                .when(eventPublisher).publishEvent(any(NotificationEvent.class));

        // when
        NoticeDto.Response result = noticeService.createNotice(1L, authorUser, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(workplaceRepository).findById(1L);
        verify(noticeRepository).save(any());
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("공지사항 작성 성공 - 사업장 고용주가 null이어도 근로자 알림 발행")
    void createNotice_Success_WhenEmployerIsNull() {
        // given
        Workplace workplaceWithoutEmployer = Workplace.builder()
                .id(1L)
                .employer(null)
                .businessNumber("123-45-67890")
                .businessName("테스트 사업체")
                .name("테스트 사업장")
                .isActive(true)
                .build();

        Notice noticeWithoutEmployer = Notice.builder()
                .id(1L)
                .workplace(workplaceWithoutEmployer)
                .author(authorUser)
                .category(NoticeCategory.URGENT)
                .title("긴급 공지")
                .content("위생사항 엄수")
                .expiresAt(futureExpiry)
                .build();

        WorkerContract contractInWorkplaceWithoutEmployer = WorkerContract.builder()
                .id(101L)
                .workplace(workplaceWithoutEmployer)
                .worker(otherWorker)
                .isActive(true)
                .build();

        NoticeDto.CreateRequest request = NoticeDto.CreateRequest.builder()
                .category(NoticeCategory.URGENT)
                .title("긴급 공지")
                .content("위생사항 엄수")
                .expiresAt(futureExpiry)
                .build();

        when(workplaceRepository.findById(1L)).thenReturn(Optional.of(workplaceWithoutEmployer));
        when(noticeRepository.save(any())).thenReturn(noticeWithoutEmployer);
        when(contractRepository.findByWorkplaceIdAndIsActive(1L, true))
                .thenReturn(List.of(contractInWorkplaceWithoutEmployer));

        // when
        NoticeDto.Response result = noticeService.createNotice(1L, authorUser, request);

        // then
        assertThat(result).isNotNull();
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUser().getId()).isEqualTo(otherUser.getId());
        assertThat(eventCaptor.getValue().getType()).isEqualTo(NotificationType.NOTICE_CREATED);
    }

    // ==================== getNotices ====================

    @Test
    @DisplayName("공지사항 목록 조회 성공 - 만료되지 않은 공지만 반환")
    void getNotices_Success() {
        // given
        when(workplaceRepository.findById(1L)).thenReturn(Optional.of(workplace));
        when(noticeRepository.findActiveNoticesByWorkplaceId(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(notice));

        // when
        List<NoticeDto.ListResponse> result = noticeService.getNotices(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("긴급 공지");
        assertThat(result.get(0).getAuthorName()).isEqualTo("작성자");
        assertThat(result.get(0).getCategory()).isEqualTo(NoticeCategory.URGENT);
    }

    @Test
    @DisplayName("공지사항 목록 조회 실패 - 존재하지 않는 사업장")
    void getNotices_Fail_WorkplaceNotFound() {
        // given
        when(workplaceRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.getNotices(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사업장을 찾을 수 없습니다");
    }

    // ==================== getNotice ====================

    @Test
    @DisplayName("공지사항 단건 조회 성공")
    void getNotice_Success() {
        // given
        when(noticeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(notice));

        // when
        NoticeDto.Response result = noticeService.getNotice(1L);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("긴급 공지");
        assertThat(result.getContent()).isEqualTo("위생사항 엄수");
    }

    @Test
    @DisplayName("공지사항 단건 조회 실패 - 존재하지 않거나 삭제된 공지")
    void getNotice_Fail_NotFound() {
        // given
        when(noticeRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.getNotice(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("공지사항을 찾을 수 없습니다");
    }

    // ==================== updateNotice ====================

    @Test
    @DisplayName("공지사항 수정 성공")
    void updateNotice_Success() {
        // given
        NoticeDto.UpdateRequest request = NoticeDto.UpdateRequest.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .expiresAt(futureExpiry.plusDays(3))
                .build();

        when(noticeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(notice));

        // when
        NoticeDto.Response result = noticeService.updateNotice(1L, authorUser, request);

        // then
        assertThat(result).isNotNull();
        verify(noticeRepository).findByIdAndIsDeletedFalse(1L);
    }

    @Test
    @DisplayName("공지사항 수정 실패 - 작성자가 아닌 경우")
    void updateNotice_Fail_NotAuthor() {
        // given
        NoticeDto.UpdateRequest request = NoticeDto.UpdateRequest.builder()
                .title("수정 시도")
                .expiresAt(futureExpiry)
                .build();

        when(noticeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(notice));

        // when & then
        assertThatThrownBy(() -> noticeService.updateNotice(1L, otherUser, request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("작성자만 수정/삭제할 수 있습니다");
    }

    // ==================== deleteNotice ====================

    @Test
    @DisplayName("공지사항 삭제 성공 - 소프트 삭제")
    void deleteNotice_Success() {
        // given
        when(noticeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(notice));

        // when
        noticeService.deleteNotice(1L, authorUser);

        // then
        assertThat(notice.getIsDeleted()).isTrue();
        verify(noticeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("공지사항 삭제 실패 - 작성자가 아닌 경우")
    void deleteNotice_Fail_NotAuthor() {
        // given
        when(noticeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(notice));

        // when & then
        assertThatThrownBy(() -> noticeService.deleteNotice(1L, otherUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("작성자만 수정/삭제할 수 있습니다");

        assertThat(notice.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("공지사항 삭제 실패 - 존재하지 않는 공지")
    void deleteNotice_Fail_NotFound() {
        // given
        when(noticeRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.deleteNotice(999L, authorUser))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("공지사항을 찾을 수 없습니다");
    }
}
