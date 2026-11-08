package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.EmployeeAttachmentDTO;
import heritage.africca.accel_connect.entity.EmployeeAttachment;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.EmployeeAttachmentRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeAttachmentService")
class EmployeeAttachmentServiceTest {

    @Mock private EmployeeAttachmentRepository attachmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private Authentication authentication;

    @InjectMocks
    private EmployeeAttachmentService attachmentService;

    private static final Long    USER_ID   = 1L;
    private static final Integer ATTACH_ID = 1;

    private User currentUser;
    private EmployeeAttachment attachment;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(99L);
        currentUser.setFirstName("Aminata");
        currentUser.setLastName("Diallo");
        currentUser.setUsername("aminata.diallo");

        attachment = new EmployeeAttachment();
        attachment.setUserId(USER_ID);
        attachment.setAttachId(ATTACH_ID);
        attachment.setFilename("contrat.pdf");
        attachment.setSize(102400);
        attachment.setFileType("application/pdf");
        attachment.setDescription("Contrat de travail");
        attachment.setScreen("contrat");
        attachment.setAttachedBy(99);
        attachment.setAttachedByName("Aminata Diallo");
        attachment.setAttachment(new byte[]{1, 2, 3});
        attachment.setCreatedAt(LocalDateTime.of(2026, 3, 5, 10, 0));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private MockMultipartFile pdfFile(int sizeBytes) {
        return new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[sizeBytes]);
    }

    private MockMultipartFile fileOfType(String contentType) {
        return new MockMultipartFile("file", "test", contentType, new byte[]{1});
    }

    // ── listAttachments ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("listAttachments")
    class ListAttachments {

        @Test
        @DisplayName("returns DTOs without binary data for existing user")
        void returnsListOfDtos() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(attachmentRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(attachment));

            List<EmployeeAttachmentDTO> result = attachmentService.listAttachments(USER_ID);

            assertEquals(1, result.size());
            EmployeeAttachmentDTO dto = result.get(0);
            assertEquals(USER_ID, dto.userId());
            assertEquals(ATTACH_ID, dto.attachId());
            assertEquals("contrat.pdf", dto.filename());
            assertEquals("Aminata Diallo", dto.attachedByName());
            verify(attachmentRepository).findByUserIdOrderByCreatedAtDesc(USER_ID);
        }

        @Test
        @DisplayName("returns empty list when user has no attachments")
        void returnsEmptyList() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(attachmentRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of());

            List<EmployeeAttachmentDTO> result = attachmentService.listAttachments(USER_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void throwsWhenUserNotFound() {
            when(userRepository.existsById(USER_ID)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> attachmentService.listAttachments(USER_ID));
            verify(attachmentRepository, never()).findByUserIdOrderByCreatedAtDesc(any());
        }
    }

    // ── upload ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("saves attachment and returns DTO on success")
        void savesAndReturnsDto() throws IOException {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userService.getCurrentUser(authentication)).thenReturn(currentUser);
            when(attachmentRepository.findMaxAttachIdByUserId(USER_ID)).thenReturn(Optional.of(2));
            when(attachmentRepository.save(any(EmployeeAttachment.class))).thenAnswer(inv -> {
                EmployeeAttachment a = inv.getArgument(0);
                a.setCreatedAt(LocalDateTime.now());
                return a;
            });

            MockMultipartFile file = pdfFile(1024);
            EmployeeAttachmentDTO result = attachmentService.upload(
                    USER_ID, file, "Contrat de travail", "contrat", authentication);

            assertNotNull(result);
            assertEquals(USER_ID, result.userId());
            assertEquals(3, result.attachId()); // max(2) + 1
            assertEquals("test.pdf", result.filename());
            assertEquals("application/pdf", result.fileType());
            assertEquals("Aminata Diallo", result.attachedByName());
        }

        @Test
        @DisplayName("assigns attachId = 1 when user has no prior attachments")
        void firstAttachmentGetsIdOne() throws IOException {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userService.getCurrentUser(authentication)).thenReturn(currentUser);
            when(attachmentRepository.findMaxAttachIdByUserId(USER_ID)).thenReturn(Optional.empty());
            when(attachmentRepository.save(any(EmployeeAttachment.class))).thenAnswer(inv -> inv.getArgument(0));

            EmployeeAttachmentDTO result = attachmentService.upload(
                    USER_ID, pdfFile(512), "Contrat", "contrat", authentication);

            assertEquals(1, result.attachId());
        }

        @Test
        @DisplayName("persists correct fields via ArgumentCaptor")
        void persistsCorrectFields() throws IOException {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userService.getCurrentUser(authentication)).thenReturn(currentUser);
            when(attachmentRepository.findMaxAttachIdByUserId(USER_ID)).thenReturn(Optional.empty());
            when(attachmentRepository.save(any(EmployeeAttachment.class))).thenAnswer(inv -> inv.getArgument(0));

            attachmentService.upload(USER_ID, pdfFile(100), "Ma description", "bulletin-salaire", authentication);

            ArgumentCaptor<EmployeeAttachment> captor = ArgumentCaptor.forClass(EmployeeAttachment.class);
            verify(attachmentRepository).save(captor.capture());
            EmployeeAttachment saved = captor.getValue();

            assertEquals(USER_ID, saved.getUserId());
            assertEquals(1, saved.getAttachId());
            assertEquals("application/pdf", saved.getFileType());
            assertEquals("bulletin-salaire", saved.getScreen());
            assertEquals("Ma description", saved.getDescription());
            assertEquals(99, saved.getAttachedBy());
            assertEquals("Aminata Diallo", saved.getAttachedByName());
            assertNotNull(saved.getAttachment());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when file is empty")
        void throwsOnEmptyFile() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

            assertThrows(IllegalArgumentException.class,
                    () -> attachmentService.upload(USER_ID, emptyFile, "desc", "contrat", authentication));
            verify(attachmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when file exceeds 10 MB")
        void throwsOnFileTooLarge() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            // 10 MB + 1 byte
            MockMultipartFile bigFile = new MockMultipartFile(
                    "file", "big.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1]);

            assertThrows(IllegalArgumentException.class,
                    () -> attachmentService.upload(USER_ID, bigFile, "desc", "contrat", authentication));
            verify(attachmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException for disallowed content type")
        void throwsOnInvalidContentType() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            MockMultipartFile exe = new MockMultipartFile(
                    "file", "script.exe", "application/octet-stream", new byte[]{1});

            assertThrows(IllegalArgumentException.class,
                    () -> attachmentService.upload(USER_ID, exe, "desc", "contrat", authentication));
            verify(attachmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when content type is null")
        void throwsOnNullContentType() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            MultipartFile noType = mock(MultipartFile.class);
            when(noType.isEmpty()).thenReturn(false);
            when(noType.getSize()).thenReturn(100L);
            when(noType.getContentType()).thenReturn(null);

            assertThrows(IllegalArgumentException.class,
                    () -> attachmentService.upload(USER_ID, noType, "desc", "contrat", authentication));
        }

        @Test
        @DisplayName("accepts JPEG content type")
        void acceptsJpeg() throws IOException {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userService.getCurrentUser(authentication)).thenReturn(currentUser);
            when(attachmentRepository.findMaxAttachIdByUserId(USER_ID)).thenReturn(Optional.empty());
            when(attachmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> attachmentService.upload(
                    USER_ID, fileOfType("image/jpeg"), "Photo", "autre", authentication));
        }

        @Test
        @DisplayName("accepts PNG content type")
        void acceptsPng() throws IOException {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userService.getCurrentUser(authentication)).thenReturn(currentUser);
            when(attachmentRepository.findMaxAttachIdByUserId(USER_ID)).thenReturn(Optional.empty());
            when(attachmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> attachmentService.upload(
                    USER_ID, fileOfType("image/png"), "Screenshot", "autre", authentication));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void throwsWhenUserNotFound() {
            when(userRepository.existsById(USER_ID)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> attachmentService.upload(USER_ID, pdfFile(100), "desc", "contrat", authentication));
            verify(attachmentRepository, never()).save(any());
        }
    }

    // ── download ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("download")
    class Download {

        @Test
        @DisplayName("returns full entity including binary when found")
        void returnsEntityWithBinary() {
            when(attachmentRepository.findByUserIdAndAttachId(USER_ID, ATTACH_ID))
                    .thenReturn(Optional.of(attachment));

            EmployeeAttachment result = attachmentService.download(USER_ID, ATTACH_ID);

            assertNotNull(result);
            assertArrayEquals(new byte[]{1, 2, 3}, result.getAttachment());
            assertEquals("contrat.pdf", result.getFilename());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when attachment not found")
        void throwsWhenNotFound() {
            when(attachmentRepository.findByUserIdAndAttachId(USER_ID, 99))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> attachmentService.download(USER_ID, 99));
        }
    }

    // ── listLeaveRequestAttachments ───────────────────────────────────────────

    @Nested
    @DisplayName("listLeaveRequestAttachments")
    class ListLeaveRequestAttachments {

        @Test
        @DisplayName("returns DTOs for all attachments tagged to the leave request")
        void returnsDtosForLeaveRequest() {
            String screen = "LEAVE_REQUEST_5";
            EmployeeAttachment att2 = new EmployeeAttachment();
            att2.setUserId(USER_ID);
            att2.setAttachId(2);
            att2.setFilename("justif.png");
            att2.setSize(20480);
            att2.setFileType("image/png");
            att2.setDescription("Justificatif");
            att2.setScreen(screen);
            att2.setAttachedBy(99);
            att2.setAttachedByName("Aminata Diallo");
            att2.setAttachment(new byte[]{4, 5, 6});
            att2.setCreatedAt(LocalDateTime.of(2026, 3, 6, 9, 0));

            attachment.setScreen(screen);
            when(attachmentRepository.findByScreen(screen)).thenReturn(List.of(attachment, att2));

            List<EmployeeAttachmentDTO> result = attachmentService.listLeaveRequestAttachments(5);

            assertEquals(2, result.size());
            assertEquals("contrat.pdf", result.get(0).filename());
            assertEquals("justif.png", result.get(1).filename());
            verify(attachmentRepository).findByScreen(screen);
        }

        @Test
        @DisplayName("returns empty list when leave request has no attachments")
        void returnsEmptyWhenNone() {
            when(attachmentRepository.findByScreen("LEAVE_REQUEST_7")).thenReturn(List.of());

            List<EmployeeAttachmentDTO> result = attachmentService.listLeaveRequestAttachments(7);

            assertTrue(result.isEmpty());
        }
    }

    // ── downloadLeaveRequestAttachment ────────────────────────────────────────

    @Nested
    @DisplayName("downloadLeaveRequestAttachment")
    class DownloadLeaveRequestAttachment {

        @Test
        @DisplayName("returns the full entity including binary when found")
        void returnsEntityWhenFound() {
            String screen = "LEAVE_REQUEST_3";
            attachment.setScreen(screen);
            when(attachmentRepository.findByScreenAndAttachId(screen, ATTACH_ID))
                    .thenReturn(Optional.of(attachment));

            EmployeeAttachment result = attachmentService.downloadLeaveRequestAttachment(3, ATTACH_ID);

            assertNotNull(result);
            assertArrayEquals(new byte[]{1, 2, 3}, result.getAttachment());
            assertEquals("contrat.pdf", result.getFilename());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when attachment not found")
        void throwsWhenNotFound() {
            when(attachmentRepository.findByScreenAndAttachId("LEAVE_REQUEST_3", 99))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> attachmentService.downloadLeaveRequestAttachment(3, 99));
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes attachment when found")
        void deletesWhenFound() {
            when(attachmentRepository.findByUserIdAndAttachId(USER_ID, ATTACH_ID))
                    .thenReturn(Optional.of(attachment));

            attachmentService.delete(USER_ID, ATTACH_ID);

            verify(attachmentRepository).deleteByUserIdAndAttachId(USER_ID, ATTACH_ID);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when attachment not found")
        void throwsWhenNotFound() {
            when(attachmentRepository.findByUserIdAndAttachId(USER_ID, 99))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> attachmentService.delete(USER_ID, 99));
            verify(attachmentRepository, never()).deleteByUserIdAndAttachId(any(), any());
        }
    }
}
