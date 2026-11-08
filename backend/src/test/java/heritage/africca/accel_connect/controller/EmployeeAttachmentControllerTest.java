package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.EmployeeAttachmentDTO;
import heritage.africca.accel_connect.config.SecurityConfig;
import heritage.africca.accel_connect.entity.EmployeeAttachment;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.service.EmployeeAttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeAttachmentController.class)
@Import(SecurityConfig.class)
@DisplayName("EmployeeAttachmentController")
class EmployeeAttachmentControllerTest {

    @Autowired private MockMvc mockMvc;

    /** Prevents Spring from trying to fetch OIDC metadata from Keycloak. */
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private EmployeeAttachmentService attachmentService;

    private static final Long    USER_ID   = 1L;
    private static final Integer ATTACH_ID = 1;
    private static final String  BASE_URL  = "/api/v1/employees/{userId}/attachments";

    private EmployeeAttachmentDTO sampleDto;
    private EmployeeAttachment    sampleEntity;

    @BeforeEach
    void setUp() {
        sampleDto = new EmployeeAttachmentDTO(
                USER_ID, ATTACH_ID,
                "Contrat de travail", "contrat.pdf",
                102400, "application/pdf",
                "contrat", "Aminata Diallo",
                LocalDateTime.of(2026, 3, 5, 10, 0)
        );

        sampleEntity = new EmployeeAttachment();
        sampleEntity.setUserId(USER_ID);
        sampleEntity.setAttachId(ATTACH_ID);
        sampleEntity.setFilename("contrat.pdf");
        sampleEntity.setFileType("application/pdf");
        sampleEntity.setAttachment(new byte[]{1, 2, 3});
    }

    // ── GET /attachments ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees/{userId}/attachments")
    class GetList {

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 200 with list of attachment DTOs")
        void returnsListOk() throws Exception {
            when(attachmentService.listAttachments(USER_ID)).thenReturn(List.of(sampleDto));

            mockMvc.perform(get(BASE_URL, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(USER_ID))
                    .andExpect(jsonPath("$[0].attachId").value(ATTACH_ID))
                    .andExpect(jsonPath("$[0].filename").value("contrat.pdf"))
                    .andExpect(jsonPath("$[0].attachedByName").value("Aminata Diallo"));
        }

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 200 with empty array when no attachments")
        void returnsEmptyList() throws Exception {
            when(attachmentService.listAttachments(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL, USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 404 when user not found")
        void returns404WhenUserNotFound() throws Exception {
            when(attachmentService.listAttachments(USER_ID))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get(BASE_URL, USER_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("returns 403 for non-HR_MANAGER user")
        void returns403ForNonHrManager() throws Exception {
            mockMvc.perform(get(BASE_URL, USER_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 401 for unauthenticated request")
        void returns401ForUnauthenticated() throws Exception {
            mockMvc.perform(get(BASE_URL, USER_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── POST /attachments ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /employees/{userId}/attachments")
    class Upload {

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 201 with created DTO on success")
        void returnsCreated() throws Exception {
            when(attachmentService.upload(eq(USER_ID), any(), eq("Contrat"), eq("contrat"), any()))
                    .thenReturn(sampleDto);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "contrat.pdf", "application/pdf", new byte[]{1, 2, 3});

            mockMvc.perform(multipart(BASE_URL, USER_ID)
                            .file(file)
                            .param("description", "Contrat")
                            .param("screen", "contrat"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(USER_ID))
                    .andExpect(jsonPath("$.attachId").value(ATTACH_ID))
                    .andExpect(jsonPath("$.filename").value("contrat.pdf"));
        }

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 400 when service throws IllegalArgumentException")
        void returns400OnServiceError() throws Exception {
            when(attachmentService.upload(any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("File too large"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "big.pdf", "application/pdf", new byte[]{1});

            mockMvc.perform(multipart(BASE_URL, USER_ID)
                            .file(file)
                            .param("description", "desc")
                            .param("screen", "contrat"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("returns 403 for non-HR_MANAGER user")
        void returns403ForNonHrManager() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "f.pdf", "application/pdf", new byte[]{1});

            mockMvc.perform(multipart(BASE_URL, USER_ID)
                            .file(file)
                            .param("description", "desc")
                            .param("screen", "contrat"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 401 for unauthenticated request")
        void returns401ForUnauthenticated() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "f.pdf", "application/pdf", new byte[]{1});

            mockMvc.perform(multipart(BASE_URL, USER_ID)
                            .file(file)
                            .param("description", "desc")
                            .param("screen", "contrat"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /attachments/{attachId}/download ──────────────────────────────────

    @Nested
    @DisplayName("GET /employees/{userId}/attachments/{attachId}/download")
    class Download {

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 200 with file bytes and Content-Disposition header")
        void returnsFileBytes() throws Exception {
            when(attachmentService.download(USER_ID, ATTACH_ID)).thenReturn(sampleEntity);

            mockMvc.perform(get(BASE_URL + "/{attachId}/download", USER_ID, ATTACH_ID))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"contrat.pdf\""))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(content().bytes(new byte[]{1, 2, 3}));
        }

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 404 when attachment not found")
        void returns404WhenNotFound() throws Exception {
            when(attachmentService.download(USER_ID, 999))
                    .thenThrow(new ResourceNotFoundException("Not found"));

            mockMvc.perform(get(BASE_URL + "/{attachId}/download", USER_ID, 999))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("returns 403 for non-HR_MANAGER user")
        void returns403ForNonHrManager() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{attachId}/download", USER_ID, ATTACH_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 401 for unauthenticated request")
        void returns401ForUnauthenticated() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{attachId}/download", USER_ID, ATTACH_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── DELETE /attachments/{attachId} ────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /employees/{userId}/attachments/{attachId}")
    class Delete {

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 204 No Content on success")
        void returnsNoContent() throws Exception {
            doNothing().when(attachmentService).delete(USER_ID, ATTACH_ID);

            mockMvc.perform(delete(BASE_URL + "/{attachId}", USER_ID, ATTACH_ID))
                    .andExpect(status().isNoContent());

            verify(attachmentService).delete(USER_ID, ATTACH_ID);
        }

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 404 when attachment not found")
        void returns404WhenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Not found"))
                    .when(attachmentService).delete(USER_ID, 999);

            mockMvc.perform(delete(BASE_URL + "/{attachId}", USER_ID, 999))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("returns 403 for non-HR_MANAGER user")
        void returns403ForNonHrManager() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{attachId}", USER_ID, ATTACH_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 401 for unauthenticated request")
        void returns401ForUnauthenticated() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{attachId}", USER_ID, ATTACH_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
