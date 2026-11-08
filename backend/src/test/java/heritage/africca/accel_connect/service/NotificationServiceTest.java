package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.NotificationDto;
import heritage.africca.accel_connect.entity.Notification;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.repository.NotificationRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User recipient;
    private Notification unreadNotification;

    @BeforeEach
    void setUp() {
        recipient = new User();
        recipient.setId(1L);

        unreadNotification = Notification.builder()
                .id(10L)
                .recipient(recipient)
                .title("New Request")
                .message("You have a new leave request")
                .link("/leave/1")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    class CreateForUser {

        @Test
        void savesNotificationWhenRecipientExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(recipient));

            notificationService.createForUser(1L, "Title", "Message", "/link");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());

            Notification saved = captor.getValue();
            assertThat(saved.getRecipient()).isEqualTo(recipient);
            assertThat(saved.getTitle()).isEqualTo("Title");
            assertThat(saved.getMessage()).isEqualTo("Message");
            assertThat(saved.getLink()).isEqualTo("/link");
            assertThat(saved.isRead()).isFalse();
        }

        @Test
        void doesNothingWhenRecipientNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            notificationService.createForUser(99L, "Title", "Message", "/link");

            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    class GetUnread {

        @Test
        void returnsUnreadNotificationsAsDtos() {
            when(notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(unreadNotification));

            List<NotificationDto> result = notificationService.getUnread(1L);

            assertThat(result).hasSize(1);
            NotificationDto dto = result.get(0);
            assertThat(dto.getId()).isEqualTo(10L);
            assertThat(dto.getTitle()).isEqualTo("New Request");
            assertThat(dto.getMessage()).isEqualTo("You have a new leave request");
            assertThat(dto.getLink()).isEqualTo("/leave/1");
            assertThat(dto.isRead()).isFalse();
            assertThat(dto.getCreatedAt()).isEqualTo(unreadNotification.getCreatedAt());
        }

        @Test
        void returnsEmptyListWhenNoneUnread() {
            when(notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            List<NotificationDto> result = notificationService.getUnread(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class MarkRead {

        @Test
        void marksReadWhenOwnerMatches() {
            when(notificationRepository.findById(10L)).thenReturn(Optional.of(unreadNotification));

            notificationService.markRead(10L, 1L);

            assertThat(unreadNotification.isRead()).isTrue();
            verify(notificationRepository).save(unreadNotification);
        }

        @Test
        void doesNotMarkReadWhenOwnerMismatch() {
            when(notificationRepository.findById(10L)).thenReturn(Optional.of(unreadNotification));

            notificationService.markRead(10L, 99L);

            assertThat(unreadNotification.isRead()).isFalse();
            verify(notificationRepository, never()).save(any());
        }

        @Test
        void doesNothingWhenNotificationNotFound() {
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

            notificationService.markRead(999L, 1L);

            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    class MarkAllRead {

        @Test
        @DisplayName("delegates to repository bulk update")
        void delegatesToRepositoryBulkUpdate() {
            notificationService.markAllRead(1L);

            verify(notificationRepository).markAllReadByRecipient(1L);
        }
    }
}
