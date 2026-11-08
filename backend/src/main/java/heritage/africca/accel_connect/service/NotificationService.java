package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.NotificationDto;
import heritage.africca.accel_connect.entity.Notification;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.repository.NotificationRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void createForUser(Long recipientId, String title, String message, String link) {
        userRepository.findById(recipientId).ifPresent(recipient ->
            notificationRepository.save(Notification.builder()
                    .recipient(recipient)
                    .title(title)
                    .message(message)
                    .link(link)
                    .build())
        );
    }

    /**
     * Broadcast to recipients the caller already holds. Unlike a loop over
     * {@link #createForUser}, this does not re-read each user by id and saves all notifications in a single batch.
     */
    @Transactional
    public void createForUsers(Collection<User> recipients, String title, String message, String link) {
        List<Notification> batch = recipients.stream()
                .map(recipient -> Notification.builder()
                        .recipient(recipient)
                        .title(title)
                        .message(message)
                        .link(link)
                        .build())
                .toList();
        notificationRepository.saveAll(batch);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUnread(Long userId) {
        return notificationRepository
                .findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipient().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByRecipient(userId);
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .link(n.getLink())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
