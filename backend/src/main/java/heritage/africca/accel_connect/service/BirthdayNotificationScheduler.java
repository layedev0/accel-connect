package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.UpcomingBirthdayDTO;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.Impl.NextcloudTalkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;


@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayNotificationScheduler {

    private static final String HR_MANAGER_ROLE = "HR_MANAGER";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Cap the number of names spelled out in the (500-char capped) in-app message. */
    private static final int MAX_BELL_NAMES = 6;
    private static final int MESSAGE_MAX = 500;

    private final EmployeeDetailsService employeeDetailsService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NextcloudTalkService nextcloudTalkService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.birthday.lead-days:3}")
    private int leadDays;

    /**
     * Every day at 08:00 (global scheduling timezone, Africa/Dakar) — notify HR managers
     * of employee birthdays that are exactly {@code leadDays} days away.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void notifyUpcomingBirthdays() {
        List<UpcomingBirthdayDTO> due = employeeDetailsService.getUpcomingBirthdays(leadDays).stream()
                .filter(b -> b.getDaysUntil() == leadDays)
                .toList();
        if (due.isEmpty()) {
            return;
        }

        List<User> hrManagers = userRepository.findByRolesIn(List.of(HR_MANAGER_ROLE));
        if (hrManagers.isEmpty()) {
            log.info("Birthday notification: {} birthday(s) due in {} day(s) but no HR_MANAGER to notify",
                    due.size(), leadDays);
            return;
        }

        String link = frontendUrl + "/";
        String title = "🎂 Anniversaire(s) à venir";
        String bellMsg = buildBellMessage(due);
        String talkMsg = buildTalkMessage(due, link);

        log.info("Birthday notification: {} birthday(s) due in {} day(s) → notifying {} HR manager(s)",
                due.size(), leadDays, hrManagers.size());

        for (User hr : hrManagers) {
            notificationService.createForUser(hr.getId(), title, bellMsg, link);
            if (hr.getKeycloakId() != null) {
                String talkId = "SSO-" + hr.getKeycloakId();
                CompletableFuture.runAsync(() -> nextcloudTalkService.sendDirectMessage(talkId, talkMsg));
            }
        }
    }

    /** One-line digest for the notification bell, kept within the 500-char column limit. */
    private String buildBellMessage(List<UpcomingBirthdayDTO> due) {
        int shown = Math.min(due.size(), MAX_BELL_NAMES);
        StringJoiner names = new StringJoiner(", ");
        for (int i = 0; i < shown; i++) {
            names.add(nameWithAge(due.get(i)));
        }
        StringBuilder sb = new StringBuilder(leadLabel(leadDays)).append(" : ").append(names);
        int hidden = due.size() - shown;
        if (hidden > 0) {
            sb.append(" … et ").append(hidden).append(" autre(s)");
        }
        sb.append('.');
        String msg = sb.toString();
        return msg.length() <= MESSAGE_MAX ? msg : msg.substring(0, MESSAGE_MAX - 1) + "…";
    }

    /** Multi-line message for the omni chat (Nextcloud Talk). */
    private String buildTalkMessage(List<UpcomingBirthdayDTO> due, String link) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎂 Anniversaire(s) ").append(leadLabel(leadDays).toLowerCase()).append(" :\n");
        for (UpcomingBirthdayDTO b : due) {
            sb.append("  • ").append(fullName(b));
            if (b.getDepartmentName() != null && !b.getDepartmentName().isBlank()) {
                sb.append(" (").append(b.getDepartmentName()).append(")");
            }
            if (b.getTurningAge() != null) {
                sb.append(" — ").append(b.getTurningAge()).append(" ans");
            }
            if (b.getNextBirthday() != null) {
                sb.append(" le ").append(b.getNextBirthday().format(FMT));
            }
            sb.append('\n');
        }
        sb.append("Voir : ").append(link);
        return sb.toString();
    }

    private String nameWithAge(UpcomingBirthdayDTO b) {
        String name = fullName(b);
        return b.getTurningAge() != null ? name + " (" + b.getTurningAge() + " ans)" : name;
    }

    private String fullName(UpcomingBirthdayDTO b) {
        String first = b.getFirstName() != null ? b.getFirstName() : "";
        String last = b.getLastName() != null ? b.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? "—" : name;
    }

    /** "Aujourd'hui" / "Demain" / "Dans N jours", matching the frontend countdown wording. */
    private String leadLabel(int days) {
        if (days <= 0) return "Aujourd'hui";
        if (days == 1) return "Demain";
        return "Dans " + days + " jours";
    }
}
