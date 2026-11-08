package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.entity.AbsenceRequest;
import heritage.africca.accel_connect.entity.Leave;
import heritage.africca.accel_connect.entity.LeaveRequest;
import heritage.africca.accel_connect.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply-rh@accel-tech.net}")
    private String fromAddress;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Send a leave request notification to every designated approver.
     * Runs asynchronously so it never blocks the HTTP response.
     */
    @Async
    public void notifyApproversOfNewLeaveRequest(LeaveRequest leaveRequest) {
        List<User> approvers = leaveRequest.getApprovers();
        if (approvers == null || approvers.isEmpty()) {
            return;
        }

        User requester = leaveRequest.getUser();
        String requesterName = requester != null
                ? (requester.getFirstName() + " " + requester.getLastName()).trim()
                : "Un employé";
        String leaveTypeName = leaveRequest.getLeaveType() != null
                ? leaveRequest.getLeaveType().getCode()
                : "Congé";

        String startDate = leaveRequest.getLeaves() != null && !leaveRequest.getLeaves().isEmpty()
                ? leaveRequest.getLeaves().stream()
                    .map(l -> l.getDate())
                    .filter(d -> d != null)
                    .min(java.time.LocalDate::compareTo)
                    .map(d -> d.format(DATE_FMT))
                    .orElse("—")
                : "—";

        String endDate = leaveRequest.getLeaves() != null && !leaveRequest.getLeaves().isEmpty()
                ? leaveRequest.getLeaves().stream()
                    .map(l -> l.getDate())
                    .filter(d -> d != null)
                    .max(java.time.LocalDate::compareTo)
                    .map(d -> d.format(DATE_FMT))
                    .orElse("—")
                : "—";

        for (User approver : approvers) {
            if (approver == null || approver.getEmail() == null) continue;
            try {
                sendApproverNotification(approver, requesterName, leaveTypeName,
                        startDate, endDate, leaveRequest.getId());
            } catch (Exception e) {
                log.warn("Failed to send leave notification to {}: {}", approver.getEmail(), e.getMessage());
            }
        }
    }

    @Async
    public void notifyApproversOfNewAbsenceRequest(AbsenceRequest absenceRequest) {
        Set<User> approvers = absenceRequest.getApprovers();
        if (approvers == null || approvers.isEmpty()) return;

        User requester = absenceRequest.getUser();
        String requesterName = requester != null
                ? (requester.getFirstName() + " " + requester.getLastName()).trim()
                : "Un employé";
        String absenceTypeName = absenceRequest.getAbsenceType() != null
                ? absenceRequest.getAbsenceType().name()
                : "Absence";

        String date = absenceRequest.getDate() != null
                ? absenceRequest.getDate().format(DATE_FMT)
                : "—";
        String start = absenceRequest.getStartTime() != null
                ? absenceRequest.getStartTime().toString()
                : "—";
        String end = absenceRequest.getEndTime() != null
                ? absenceRequest.getEndTime().toString()
                : "—";

        for (User approver : approvers) {
            if (approver == null || approver.getEmail() == null) continue;
            try {
                sendAbsenceApproverNotification(approver, requesterName, absenceTypeName,
                        date, start, end, absenceRequest.getReferenceCode());
            } catch (Exception e) {
                log.warn("Failed to send absence notification to {}: {}", approver.getEmail(), e.getMessage());
            }
        }
    }

    @Async
    public void notifyEmployeeOfLeaveApproval(LeaveRequest leaveRequest) {
        User employee = leaveRequest.getUser();
        if (employee == null || employee.getEmail() == null) return;

        String startDate = leaveRequest.getLeaves() != null && !leaveRequest.getLeaves().isEmpty()
                ? leaveRequest.getLeaves().stream().map(l -> l.getDate()).filter(d -> d != null)
                    .min(java.time.LocalDate::compareTo).map(d -> d.format(DATE_FMT)).orElse("—")
                : "—";
        String endDate = leaveRequest.getLeaves() != null && !leaveRequest.getLeaves().isEmpty()
                ? leaveRequest.getLeaves().stream().map(l -> l.getDate()).filter(d -> d != null)
                    .max(java.time.LocalDate::compareTo).map(d -> d.format(DATE_FMT)).orElse("—")
                : "—";
        String leaveTypeName = leaveRequest.getLeaveType() != null ? leaveRequest.getLeaveType().getCode() : "Congé";
        String firstName = employee.getFirstName() != null ? employee.getFirstName() : "Employé";

        try {
            sendSimpleMail(employee.getEmail(),
                    "Votre demande de congé a été approuvée — #" + leaveRequest.getId(),
                    buildEmployeeLeaveDecisionHtml(firstName, leaveTypeName, startDate, endDate,
                            String.valueOf(leaveRequest.getId()), true, frontendUrl + "/rh/conges"));
            log.info("Leave approval email sent to {}", employee.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send leave approval email to {}: {}", employee.getEmail(), e.getMessage());
        }
    }

    @Async
    public void notifyEmployeeOfLeaveRejection(LeaveRequest leaveRequest) {
        User employee = leaveRequest.getUser();
        if (employee == null || employee.getEmail() == null) return;

        String startDate = leaveRequest.getLeaves() != null && !leaveRequest.getLeaves().isEmpty()
                ? leaveRequest.getLeaves().stream().map(l -> l.getDate()).filter(d -> d != null)
                    .min(java.time.LocalDate::compareTo).map(d -> d.format(DATE_FMT)).orElse("—")
                : "—";
        String endDate = leaveRequest.getLeaves() != null && !leaveRequest.getLeaves().isEmpty()
                ? leaveRequest.getLeaves().stream().map(l -> l.getDate()).filter(d -> d != null)
                    .max(java.time.LocalDate::compareTo).map(d -> d.format(DATE_FMT)).orElse("—")
                : "—";
        String leaveTypeName = leaveRequest.getLeaveType() != null ? leaveRequest.getLeaveType().getCode() : "Congé";
        String firstName = employee.getFirstName() != null ? employee.getFirstName() : "Employé";

        try {
            sendSimpleMail(employee.getEmail(),
                    "Votre demande de congé a été rejetée — #" + leaveRequest.getId(),
                    buildEmployeeLeaveDecisionHtml(firstName, leaveTypeName, startDate, endDate,
                            String.valueOf(leaveRequest.getId()), false, frontendUrl + "/rh/conges"));
            log.info("Leave rejection email sent to {}", employee.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send leave rejection email to {}: {}", employee.getEmail(), e.getMessage());
        }
    }

    @Async
    public void notifyEmployeeOfLeaveCancellation(LeaveRequest leaveRequest) {
        User employee = leaveRequest.getUser();
        if (employee == null || employee.getEmail() == null) return;

        String startDate = leaveRequest.getLeaves() != null && !leaveRequest.getLeaves().isEmpty()
                ? leaveRequest.getLeaves().stream().map(l -> l.getDate()).filter(d -> d != null)
                    .min(java.time.LocalDate::compareTo).map(d -> d.format(DATE_FMT)).orElse("—")
                : "—";
        String endDate = leaveRequest.getLeaves() != null && !leaveRequest.getLeaves().isEmpty()
                ? leaveRequest.getLeaves().stream().map(l -> l.getDate()).filter(d -> d != null)
                    .max(java.time.LocalDate::compareTo).map(d -> d.format(DATE_FMT)).orElse("—")
                : "—";
        String leaveTypeName = leaveRequest.getLeaveType() != null ? leaveRequest.getLeaveType().getCode() : "Congé";
        String firstName = employee.getFirstName() != null ? employee.getFirstName() : "Employé";

        try {
            sendSimpleMail(employee.getEmail(),
                    "Votre demande de congé a été annulée — #" + leaveRequest.getId(),
                    buildEmployeeLeaveCancellationHtml(firstName, leaveTypeName, startDate, endDate,
                            String.valueOf(leaveRequest.getId()), frontendUrl + "/rh/conges"));
            log.info("Leave cancellation email sent to {}", employee.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send leave cancellation email to {}: {}", employee.getEmail(), e.getMessage());
        }
    }

    @Async
    public void notifyEmployeeOfAbsenceApproval(AbsenceRequest absenceRequest) {
        User employee = absenceRequest.getUser();
        if (employee == null || employee.getEmail() == null) return;

        String firstName = employee.getFirstName() != null ? employee.getFirstName() : "Employé";
        String typeName = absenceRequest.getAbsenceType() != null ? absenceRequest.getAbsenceType().name() : "Absence";
        String date = absenceRequest.getDate() != null ? absenceRequest.getDate().format(DATE_FMT) : "—";
        String start = absenceRequest.getStartTime() != null ? absenceRequest.getStartTime().toString() : "—";
        String end = absenceRequest.getEndTime() != null ? absenceRequest.getEndTime().toString() : "—";

        try {
            sendSimpleMail(employee.getEmail(),
                    "Votre demande d'absence a été approuvée — " + absenceRequest.getReferenceCode(),
                    buildEmployeeAbsenceDecisionHtml(firstName, typeName, date, start, end,
                            absenceRequest.getReferenceCode(), true, frontendUrl + "/rh/absences"));
            log.info("Absence approval email sent to {}", employee.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send absence approval email to {}: {}", employee.getEmail(), e.getMessage());
        }
    }

    @Async
    public void notifyEmployeeOfAbsenceRejection(AbsenceRequest absenceRequest) {
        User employee = absenceRequest.getUser();
        if (employee == null || employee.getEmail() == null) return;

        String firstName = employee.getFirstName() != null ? employee.getFirstName() : "Employé";
        String typeName = absenceRequest.getAbsenceType() != null ? absenceRequest.getAbsenceType().name() : "Absence";
        String date = absenceRequest.getDate() != null ? absenceRequest.getDate().format(DATE_FMT) : "—";
        String start = absenceRequest.getStartTime() != null ? absenceRequest.getStartTime().toString() : "—";
        String end = absenceRequest.getEndTime() != null ? absenceRequest.getEndTime().toString() : "—";

        try {
            sendSimpleMail(employee.getEmail(),
                    "Votre demande d'absence a été rejetée — " + absenceRequest.getReferenceCode(),
                    buildEmployeeAbsenceDecisionHtml(firstName, typeName, date, start, end,
                            absenceRequest.getReferenceCode(), false, frontendUrl + "/rh/absences"));
            log.info("Absence rejection email sent to {}", employee.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send absence rejection email to {}: {}", employee.getEmail(), e.getMessage());
        }
    }

    @Async
    public void notifyEmployeeOfAbsenceCancellation(AbsenceRequest absenceRequest) {
        User employee = absenceRequest.getUser();
        if (employee == null || employee.getEmail() == null) return;

        String firstName = employee.getFirstName() != null ? employee.getFirstName() : "Employé";
        String typeName = absenceRequest.getAbsenceType() != null ? absenceRequest.getAbsenceType().name() : "Absence";
        String date = absenceRequest.getDate() != null ? absenceRequest.getDate().format(DATE_FMT) : "—";
        String start = absenceRequest.getStartTime() != null ? absenceRequest.getStartTime().toString() : "—";
        String end = absenceRequest.getEndTime() != null ? absenceRequest.getEndTime().toString() : "—";

        try {
            sendSimpleMail(employee.getEmail(),
                    "Votre demande d'absence a été annulée — " + absenceRequest.getReferenceCode(),
                    buildEmployeeAbsenceCancellationHtml(firstName, typeName, date, start, end,
                            absenceRequest.getReferenceCode(), frontendUrl + "/rh/absences"));
            log.info("Absence cancellation email sent to {}", employee.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send absence cancellation email to {}: {}", employee.getEmail(), e.getMessage());
        }
    }

    @Async
    public void notifyEmployeeOfBalanceAdjustment(User employee, String fieldLabel, String changeLabel,
                                                   String oldBalance, String newBalance) {
        if (employee == null || employee.getEmail() == null) return;

        String firstName = employee.getFirstName() != null ? employee.getFirstName() : "Employé";

        try {
            sendSimpleMail(employee.getEmail(),
                    "Votre solde de congés a été mis à jour",
                    buildBalanceAdjustmentHtml(firstName, fieldLabel, changeLabel, oldBalance, newBalance,
                            frontendUrl + "/rh/conges"));
            log.info("Leave balance adjustment email sent to {}", employee.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send leave balance adjustment email to {}: {}", employee.getEmail(), e.getMessage());
        }
    }

    private String buildBalanceAdjustmentHtml(String firstName, String fieldLabel, String changeLabel,
                                                String oldBalance, String newBalance, String myLeavesUrl) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.5px;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">Gestion des congés</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;">
                                Bonjour <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                Votre solde de congés vient d'être ajusté par le service RH.
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="background:#f8fafc;border:1px solid #e5e7eb;
                                            border-radius:8px;margin-bottom:28px;">
                                <tr>
                                  <td style="padding:20px 24px;">
                                    <table width="100%%" cellpadding="6" cellspacing="0">
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;width:140px;">Champ ajusté</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Ajustement</td>
                                        <td style="color:#111827;font-size:14px;font-weight:600;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Ancien solde</td>
                                        <td style="color:#111827;font-size:14px;">%s j</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Nouveau solde</td>
                                        <td style="color:#25509d;font-size:14px;font-weight:700;">%s j</td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                              <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:8px;">
                                <tr>
                                  <td align="center">
                                    <a href="%s"
                                       style="display:inline-block;background:#25509d;color:#ffffff;
                                              font-size:14px;font-weight:600;text-decoration:none;
                                              padding:12px 32px;border-radius:8px;letter-spacing:.3px;">
                                      Voir mon solde de congés
                                    </a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f8fafc;border-top:1px solid #e5e7eb;
                                       padding:16px 32px;text-align:center;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                Ceci est un message automatique — merci de ne pas y répondre.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, fieldLabel, changeLabel, oldBalance, newBalance, myLeavesUrl);
    }

    private void sendSimpleMail(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    private String buildEmployeeLeaveDecisionHtml(String firstName, String leaveTypeName,
                                                   String startDate, String endDate, String reference,
                                                   boolean approved, String myLeavesUrl) {
        String statusColor = approved ? "#16a34a" : "#dc2626";
        String statusLabel = approved ? "Approuvée" : "Rejetée";
        String statusMessage = approved
                ? "Votre demande de congé a été <strong>approuvée</strong>."
                : "Votre demande de congé a été <strong>rejetée</strong>. Pour toute question, veuillez contacter votre responsable RH.";

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.5px;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">Gestion des congés</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;">
                                Bonjour <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">%s</p>
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="background:#f8fafc;border:1px solid #e5e7eb;
                                            border-radius:8px;margin-bottom:28px;">
                                <tr>
                                  <td style="padding:20px 24px;">
                                    <table width="100%%" cellpadding="6" cellspacing="0">
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;width:140px;">Type</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Du</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Au</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Référence</td>
                                        <td style="color:#25509d;font-size:14px;font-weight:600;">#%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Statut</td>
                                        <td style="color:%s;font-size:14px;font-weight:700;">%s</td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                              <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:8px;">
                                <tr>
                                  <td align="center">
                                    <a href="%s"
                                       style="display:inline-block;background:#25509d;color:#ffffff;
                                              font-size:14px;font-weight:600;text-decoration:none;
                                              padding:12px 32px;border-radius:8px;letter-spacing:.3px;">
                                      Voir mes congés
                                    </a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f8fafc;border-top:1px solid #e5e7eb;
                                       padding:16px 32px;text-align:center;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                Ceci est un message automatique — merci de ne pas y répondre.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, statusMessage, leaveTypeName,
                startDate, endDate, reference, statusColor, statusLabel, myLeavesUrl);
    }

    private String buildEmployeeLeaveCancellationHtml(String firstName, String leaveTypeName,
                                                       String startDate, String endDate,
                                                       String reference, String myLeavesUrl) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.5px;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">Gestion des congés</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;">
                                Bonjour <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                Votre demande de congé a été <strong>annulée</strong>. Pour toute question, veuillez contacter votre responsable RH.
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="background:#f8fafc;border:1px solid #e5e7eb;
                                            border-radius:8px;margin-bottom:28px;">
                                <tr>
                                  <td style="padding:20px 24px;">
                                    <table width="100%%" cellpadding="6" cellspacing="0">
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;width:140px;">Type</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Du</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Au</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Référence</td>
                                        <td style="color:#25509d;font-size:14px;font-weight:600;">#%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Statut</td>
                                        <td style="color:#d97706;font-size:14px;font-weight:700;">Annulé</td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                              <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:8px;">
                                <tr>
                                  <td align="center">
                                    <a href="%s"
                                       style="display:inline-block;background:#25509d;color:#ffffff;
                                              font-size:14px;font-weight:600;text-decoration:none;
                                              padding:12px 32px;border-radius:8px;letter-spacing:.3px;">
                                      Voir mes congés
                                    </a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f8fafc;border-top:1px solid #e5e7eb;
                                       padding:16px 32px;text-align:center;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                Ceci est un message automatique — merci de ne pas y répondre.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, leaveTypeName, startDate, endDate, reference, myLeavesUrl);
    }

    private String buildEmployeeAbsenceDecisionHtml(String firstName, String absenceTypeName,
                                                     String date, String start, String end, String reference,
                                                     boolean approved, String myAbsencesUrl) {
        String statusColor = approved ? "#16a34a" : "#dc2626";
        String statusLabel = approved ? "Approuvée" : "Rejetée";
        String statusMessage = approved
                ? "Votre demande d'absence a été <strong>approuvée</strong>."
                : "Votre demande d'absence a été <strong>rejetée</strong>. Pour toute question, veuillez contacter votre responsable RH.";

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.5px;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">Gestion des absences</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;">
                                Bonjour <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">%s</p>
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="background:#f8fafc;border:1px solid #e5e7eb;
                                            border-radius:8px;margin-bottom:28px;">
                                <tr>
                                  <td style="padding:20px 24px;">
                                    <table width="100%%" cellpadding="6" cellspacing="0">
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;width:140px;">Type</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Date</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Début</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Fin</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Référence</td>
                                        <td style="color:#25509d;font-size:14px;font-weight:600;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Statut</td>
                                        <td style="color:%s;font-size:14px;font-weight:700;">%s</td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                              <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:8px;">
                                <tr>
                                  <td align="center">
                                    <a href="%s"
                                       style="display:inline-block;background:#25509d;color:#ffffff;
                                              font-size:14px;font-weight:600;text-decoration:none;
                                              padding:12px 32px;border-radius:8px;letter-spacing:.3px;">
                                      Voir mes absences
                                    </a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f8fafc;border-top:1px solid #e5e7eb;
                                       padding:16px 32px;text-align:center;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                Ceci est un message automatique — merci de ne pas y répondre.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, statusMessage, absenceTypeName,
                date, start, end, reference, statusColor, statusLabel, myAbsencesUrl);
    }

    private String buildEmployeeAbsenceCancellationHtml(String firstName, String absenceTypeName,
                                                         String date, String start, String end, String reference,
                                                         String myAbsencesUrl) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.5px;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">Gestion des absences</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;">
                                Bonjour <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                Votre demande d'absence a été <strong>annulée</strong>. Pour toute question, veuillez contacter votre responsable RH.
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="background:#f8fafc;border:1px solid #e5e7eb;
                                            border-radius:8px;margin-bottom:28px;">
                                <tr>
                                  <td style="padding:20px 24px;">
                                    <table width="100%%" cellpadding="6" cellspacing="0">
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;width:140px;">Type</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Date</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Début</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Fin</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Référence</td>
                                        <td style="color:#25509d;font-size:14px;font-weight:600;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Statut</td>
                                        <td style="color:#d97706;font-size:14px;font-weight:700;">Annulé</td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                              <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:8px;">
                                <tr>
                                  <td align="center">
                                    <a href="%s"
                                       style="display:inline-block;background:#25509d;color:#ffffff;
                                              font-size:14px;font-weight:600;text-decoration:none;
                                              padding:12px 32px;border-radius:8px;letter-spacing:.3px;">
                                      Voir mes absences
                                    </a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f8fafc;border-top:1px solid #e5e7eb;
                                       padding:16px 32px;text-align:center;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                Ceci est un message automatique — merci de ne pas y répondre.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, absenceTypeName, date, start, end, reference, myAbsencesUrl);
    }

    private void sendAbsenceApproverNotification(User approver, String requesterName,
                                                 String absenceTypeName, String date,
                                                 String start, String end, String absenceRequestId)
            throws MessagingException {

        String approverFirstName = approver.getFirstName() != null ? approver.getFirstName() : "Approbateur";
        String subject = "Nouvelle demande d'absence — " + requesterName;

        String html = buildAbsenceHtml(approverFirstName, requesterName, absenceTypeName,
                date, start, end, absenceRequestId, frontendUrl + "/rh/approbation-absences");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(approver.getEmail());
        helper.setSubject(subject);
        helper.setText(html, true);

        mailSender.send(message);
        log.info("Absence notification sent to {} for request #{}", approver.getEmail(), absenceRequestId);
    }

    private String buildAbsenceHtml(String approverFirstName, String requesterName,
                                    String absenceTypeName, String date,
                                    String start, String end, String absenceRequestId,
                                    String approvalUrl) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.5px;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">Gestion des absences</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 20px;color:#374151;font-size:15px;">
                                Bonjour <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                <strong>%s</strong> a soumis une nouvelle demande d'absence
                                qui nécessite votre approbation.
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="background:#f8fafc;border:1px solid #e5e7eb;
                                            border-radius:8px;margin-bottom:28px;">
                                <tr>
                                  <td style="padding:20px 24px;">
                                    <table width="100%%" cellpadding="6" cellspacing="0">
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;width:140px;">Type</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Date</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Début</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Fin</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Référence</td>
                                        <td style="color:#25509d;font-size:14px;font-weight:600;">%s</td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                              <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:8px;">
                                <tr>
                                  <td align="center">
                                    <a href="%s"
                                       style="display:inline-block;background:#25509d;color:#ffffff;
                                              font-size:14px;font-weight:600;text-decoration:none;
                                              padding:12px 32px;border-radius:8px;letter-spacing:.3px;">
                                      Traiter la demande
                                    </a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f8fafc;border-top:1px solid #e5e7eb;
                                       padding:16px 32px;text-align:center;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                Ceci est un message automatique — merci de ne pas y répondre.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(approverFirstName, requesterName, absenceTypeName,
                date, start, end, absenceRequestId, approvalUrl);
    }

    @Async
    public void notifyObserversOfNewLeaveRequest(LeaveRequest leaveRequest, List<User> observers) {
        if (observers == null || observers.isEmpty()) return;

        User requester = leaveRequest.getUser();
        String requesterName = requester != null
                ? (requester.getFirstName() + " " + requester.getLastName()).trim()
                : "Un employé";
        String leaveTypeName = leaveRequest.getLeaveType() != null
                ? leaveRequest.getLeaveType().getCode() : "Congé";

        String startDate = leaveRequest.getLeaves() != null && !leaveRequest.getLeaves().isEmpty()
                ? leaveRequest.getLeaves().stream().map(l -> l.getDate()).filter(d -> d != null)
                    .min(java.time.LocalDate::compareTo).map(d -> d.format(DATE_FMT)).orElse("—")
                : "—";
        String endDate = leaveRequest.getLeaves() != null && !leaveRequest.getLeaves().isEmpty()
                ? leaveRequest.getLeaves().stream().map(l -> l.getDate()).filter(d -> d != null)
                    .max(java.time.LocalDate::compareTo).map(d -> d.format(DATE_FMT)).orElse("—")
                : "—";

        List<User> approvers = leaveRequest.getApprovers();
        String approversNames = (approvers == null || approvers.isEmpty())
                ? "Équipe managériale &amp; RH"
                : approvers.stream()
                    .filter(a -> a != null)
                    .map(a -> (a.getFirstName() != null ? a.getFirstName() : "") + " "
                            + (a.getLastName() != null ? a.getLastName() : ""))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.joining(", "));

        for (User observer : observers) {
            if (observer == null || observer.getEmail() == null) continue;
            try {
                String firstName = observer.getFirstName() != null ? observer.getFirstName() : "Collaborateur";
                String subject = "Information — demande de congé de " + requesterName;
                String html = buildObserverHtml(firstName, requesterName, leaveTypeName, startDate, endDate, approversNames);
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(observer.getEmail());
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(message);
                log.info("Observer notification sent to {} for leave request #{}", observer.getEmail(), leaveRequest.getId());
            } catch (Exception e) {
                log.warn("Failed to send observer notification to {}: {}", observer.getEmail(), e.getMessage());
            }
        }
    }

    private String buildObserverHtml(String observerFirstName, String requesterName,
                                     String leaveTypeName, String startDate, String endDate,
                                     String approversNames) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">Gestion des congés</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 20px;color:#374151;font-size:15px;">
                                Bonjour <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                Pour information, <strong>%s</strong> a soumis une demande de congé.
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="background:#f8fafc;border-radius:8px;padding:20px;margin-bottom:24px;">
                                <tr><td style="padding:6px 0;">
                                  <span style="color:#6b7280;font-size:13px;">Type :</span>
                                  <strong style="color:#111827;font-size:13px;margin-left:8px;">%s</strong>
                                </td></tr>
                                <tr><td style="padding:6px 0;">
                                  <span style="color:#6b7280;font-size:13px;">Du :</span>
                                  <strong style="color:#111827;font-size:13px;margin-left:8px;">%s</strong>
                                </td></tr>
                                <tr><td style="padding:6px 0;">
                                  <span style="color:#6b7280;font-size:13px;">Au :</span>
                                  <strong style="color:#111827;font-size:13px;margin-left:8px;">%s</strong>
                                </td></tr>
                                <tr><td style="padding:6px 0;">
                                  <span style="color:#6b7280;font-size:13px;">Approbateur(s) :</span>
                                  <strong style="color:#111827;font-size:13px;margin-left:8px;">%s</strong>
                                </td></tr>
                              </table>
                              <p style="margin:0;color:#6b7280;font-size:13px;">
                                Ce message est envoyé à titre informatif uniquement.
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f9fafb;padding:16px 32px;text-align:center;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                Ceci est un message automatique — merci de ne pas y répondre.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(observerFirstName, requesterName, leaveTypeName, startDate, endDate, approversNames);
    }

    private void sendApproverNotification(User approver, String requesterName,
                                          String leaveTypeName, String startDate,
                                          String endDate, Integer leaveRequestId)
            throws MessagingException {

        String approverFirstName = approver.getFirstName() != null ? approver.getFirstName() : "Approbateur";
        String subject = "Nouvelle demande de congé — " + requesterName;

        String html = buildHtml(approverFirstName, requesterName, leaveTypeName,
                startDate, endDate, leaveRequestId, frontendUrl + "/rh/approbation-conges");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(approver.getEmail());
        helper.setSubject(subject);
        helper.setText(html, true);

        mailSender.send(message);
        log.info("Leave notification sent to {} for request #{}", approver.getEmail(), leaveRequestId);
    }

    private String buildHtml(String approverFirstName, String requesterName,
                              String leaveTypeName, String startDate,
                              String endDate, Integer leaveRequestId, String approvalUrl) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <!-- Header -->
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;
                                         letter-spacing:.5px;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">
                                Gestion des congés
                              </p>
                            </td>
                          </tr>
                          <!-- Body -->
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 20px;color:#374151;font-size:15px;">
                                Bonjour <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                <strong>%s</strong> a soumis une nouvelle demande de congé
                                qui nécessite votre approbation.
                              </p>
                              <!-- Details card -->
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="background:#f8fafc;border:1px solid #e5e7eb;
                                            border-radius:8px;margin-bottom:28px;">
                                <tr>
                                  <td style="padding:20px 24px;">
                                    <table width="100%%" cellpadding="6" cellspacing="0">
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;
                                                   width:140px;">Type</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Du</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Au</td>
                                        <td style="color:#111827;font-size:14px;">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="color:#6b7280;font-size:13px;font-weight:600;
                                                   text-transform:uppercase;letter-spacing:.6px;">Référence</td>
                                        <td style="color:#25509d;font-size:14px;font-weight:600;">#%d</td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                              <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:8px;">
                                <tr>
                                  <td align="center">
                                    <a href="%s"
                                       style="display:inline-block;background:#25509d;color:#ffffff;
                                              font-size:14px;font-weight:600;text-decoration:none;
                                              padding:12px 32px;border-radius:8px;letter-spacing:.3px;">
                                      Traiter la demande
                                    </a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="background:#f8fafc;border-top:1px solid #e5e7eb;
                                       padding:16px 32px;text-align:center;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                Ceci est un message automatique — merci de ne pas y répondre.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(approverFirstName, requesterName, leaveTypeName,
                startDate, endDate, leaveRequestId, approvalUrl);
    }

    @Async
    public void notifyObserverOfNewAbsenceRequest(AbsenceRequest absenceRequest, List<User> observers) {
        if (observers == null || observers.isEmpty()) return;
        User employee = absenceRequest.getUser();
        String employeeName = employee != null
                ? (employee.getFirstName() + " " + employee.getLastName()).trim() : "Un employé";
        String typeName = absenceRequest.getAbsenceType() != null ? absenceRequest.getAbsenceType().name() : "Absence";
        String date = absenceRequest.getDate() != null ? absenceRequest.getDate().format(DATE_FMT) : "—";
        for (User observer : observers) {
            if (observer == null || observer.getEmail() == null) continue;
            try {
                sendSimpleMail(observer.getEmail(),
                        "Information — demande d'absence de " + employeeName,
                        buildObserverNewAbsenceHtml(employeeName, typeName, date,
                                absenceRequest.getReferenceCode(), frontendUrl + "/rh/absences"));
                log.info("Observer new-absence notification sent to {} for absence {}", observer.getEmail(), absenceRequest.getReferenceCode());
            } catch (Exception e) {
                log.warn("Failed to send observer new-absence notification to {}: {}", observer.getEmail(), e.getMessage());
            }
        }
    }

    private String buildObserverNewAbsenceHtml(String employeeName, String absenceType,
                                                String date, String referenceCode, String absencesUrl) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">Gestion des absences</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 20px;color:#374151;font-size:15px;">Bonjour,</p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                Pour information, <strong>%s</strong> a soumis une nouvelle demande d'absence.
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="background:#f8fafc;border-radius:8px;padding:20px;margin-bottom:24px;">
                                <tr><td style="padding:6px 0;color:#6b7280;font-size:13px;">Réf.</td>
                                    <td style="padding:6px 0;color:#111827;font-size:13px;font-weight:600;">%s</td></tr>
                                <tr><td style="padding:6px 0;color:#6b7280;font-size:13px;">Type</td>
                                    <td style="padding:6px 0;color:#111827;font-size:13px;">%s</td></tr>
                                <tr><td style="padding:6px 0;color:#6b7280;font-size:13px;">Date</td>
                                    <td style="padding:6px 0;color:#111827;font-size:13px;">%s</td></tr>
                              </table>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(employeeName, referenceCode, absenceType, date, absencesUrl);
    }

    // -------------------------------------------------------------------------
    // Daily reminder digests (called by LeaveReminderScheduler)
    // -------------------------------------------------------------------------

    @Async
    public void sendPendingLeaveReminderToApprover(User approver, List<LeaveRequest> pending, String approvalUrl) {
        if (approver == null || approver.getEmail() == null || pending == null || pending.isEmpty()) return;
        String firstName = approver.getFirstName() != null ? approver.getFirstName() : "Approbateur";
        String subject = "Rappel — " + pending.size() + " demande(s) de congé en attente de votre validation";
        try {
            sendSimpleMail(approver.getEmail(), subject,
                    buildLeaveReminderHtml(firstName, pending, approvalUrl));
            log.info("Leave reminder email sent to {}", approver.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send leave reminder to {}: {}", approver.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendPendingAbsenceReminderToApprover(User approver, List<AbsenceRequest> pending, String approvalUrl) {
        if (approver == null || approver.getEmail() == null || pending == null || pending.isEmpty()) return;
        String firstName = approver.getFirstName() != null ? approver.getFirstName() : "Approbateur";
        String subject = "Rappel — " + pending.size() + " demande(s) d'absence en attente de votre validation";
        try {
            sendSimpleMail(approver.getEmail(), subject,
                    buildAbsenceReminderHtml(firstName, pending, approvalUrl));
            log.info("Absence reminder email sent to {}", approver.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send absence reminder to {}: {}", approver.getEmail(), e.getMessage());
        }
    }

    private String buildLeaveReminderHtml(String firstName, List<LeaveRequest> pending, String approvalUrl) {
        StringBuilder rows = new StringBuilder();
        for (LeaveRequest lr : pending) {
            String name = lr.getUser() != null
                    ? (lr.getUser().getFirstName() + " " + lr.getUser().getLastName()).trim() : "—";
            String type = lr.getLeaveType() != null ? lr.getLeaveType().getCode() : "Congé";
            String start = lr.getLeaves() != null ? lr.getLeaves().stream()
                    .map(Leave::getDate).filter(d -> d != null)
                    .min(java.time.LocalDate::compareTo).map(d -> d.format(DATE_FMT)).orElse("—") : "—";
            String end = lr.getLeaves() != null ? lr.getLeaves().stream()
                    .map(Leave::getDate).filter(d -> d != null)
                    .max(java.time.LocalDate::compareTo).map(d -> d.format(DATE_FMT)).orElse("—") : "—";
            rows.append("""
                    <tr>
                      <td style="padding:10px 12px;color:#111827;font-size:13px;border-bottom:1px solid #f3f4f6;">%s</td>
                      <td style="padding:10px 12px;color:#111827;font-size:13px;border-bottom:1px solid #f3f4f6;">%s</td>
                      <td style="padding:10px 12px;color:#111827;font-size:13px;border-bottom:1px solid #f3f4f6;">%s</td>
                      <td style="padding:10px 12px;color:#111827;font-size:13px;border-bottom:1px solid #f3f4f6;">%s</td>
                      <td style="padding:10px 12px;color:#25509d;font-size:13px;font-weight:600;border-bottom:1px solid #f3f4f6;">#%d</td>
                    </tr>
                    """.formatted(name, type, start, end, lr.getId()));
        }
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="620" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.5px;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">Gestion des congés</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;">
                                Bonjour <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                Les demandes de congé suivantes sont en attente de votre validation.
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;margin-bottom:28px;">
                                <thead>
                                  <tr style="background:#f8fafc;">
                                    <th style="padding:10px 12px;color:#6b7280;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.6px;text-align:left;">Employé</th>
                                    <th style="padding:10px 12px;color:#6b7280;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.6px;text-align:left;">Type</th>
                                    <th style="padding:10px 12px;color:#6b7280;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.6px;text-align:left;">Du</th>
                                    <th style="padding:10px 12px;color:#6b7280;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.6px;text-align:left;">Au</th>
                                    <th style="padding:10px 12px;color:#6b7280;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.6px;text-align:left;">Réf.</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  %s
                                </tbody>
                              </table>
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td align="center">
                                    <a href="%s"
                                       style="display:inline-block;background:#25509d;color:#ffffff;
                                              font-size:14px;font-weight:600;text-decoration:none;
                                              padding:12px 32px;border-radius:8px;letter-spacing:.3px;">
                                      Traiter les demandes
                                    </a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f8fafc;border-top:1px solid #e5e7eb;
                                       padding:16px 32px;text-align:center;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                Ceci est un message automatique — merci de ne pas y répondre.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, rows.toString(), approvalUrl);
    }

    private String buildAbsenceReminderHtml(String firstName, List<AbsenceRequest> pending, String approvalUrl) {
        StringBuilder rows = new StringBuilder();
        for (AbsenceRequest ar : pending) {
            String name = ar.getUser() != null
                    ? (ar.getUser().getFirstName() + " " + ar.getUser().getLastName()).trim() : "—";
            String type = ar.getAbsenceType() != null ? ar.getAbsenceType().name() : "Absence";
            String date = ar.getDate() != null ? ar.getDate().format(DATE_FMT) : "—";
            String ref = ar.getReferenceCode() != null ? ar.getReferenceCode() : "—";
            rows.append("""
                    <tr>
                      <td style="padding:10px 12px;color:#111827;font-size:13px;border-bottom:1px solid #f3f4f6;">%s</td>
                      <td style="padding:10px 12px;color:#111827;font-size:13px;border-bottom:1px solid #f3f4f6;">%s</td>
                      <td style="padding:10px 12px;color:#111827;font-size:13px;border-bottom:1px solid #f3f4f6;">%s</td>
                      <td style="padding:10px 12px;color:#25509d;font-size:13px;font-weight:600;border-bottom:1px solid #f3f4f6;">%s</td>
                    </tr>
                    """.formatted(name, type, date, ref));
        }
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="620" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:12px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,.08);">
                          <tr>
                            <td style="background:#25509d;padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.5px;">
                                Accel Connect — RH
                              </h1>
                              <p style="margin:4px 0 0;color:#c7d8f5;font-size:13px;">Gestion des absences</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 16px;color:#374151;font-size:15px;">
                                Bonjour <strong>%s</strong>,
                              </p>
                              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                                Les demandes d'absence suivantes sont en attente de votre validation.
                              </p>
                              <table width="100%%" cellpadding="0" cellspacing="0"
                                     style="border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;margin-bottom:28px;">
                                <thead>
                                  <tr style="background:#f8fafc;">
                                    <th style="padding:10px 12px;color:#6b7280;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.6px;text-align:left;">Employé</th>
                                    <th style="padding:10px 12px;color:#6b7280;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.6px;text-align:left;">Type</th>
                                    <th style="padding:10px 12px;color:#6b7280;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.6px;text-align:left;">Date</th>
                                    <th style="padding:10px 12px;color:#6b7280;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.6px;text-align:left;">Réf.</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  %s
                                </tbody>
                              </table>
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td align="center">
                                    <a href="%s"
                                       style="display:inline-block;background:#25509d;color:#ffffff;
                                              font-size:14px;font-weight:600;text-decoration:none;
                                              padding:12px 32px;border-radius:8px;letter-spacing:.3px;">
                                      Traiter les demandes
                                    </a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f8fafc;border-top:1px solid #e5e7eb;
                                       padding:16px 32px;text-align:center;">
                              <p style="margin:0;color:#9ca3af;font-size:12px;">
                                Ceci est un message automatique — merci de ne pas y répondre.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, rows.toString(), approvalUrl);
    }
}
