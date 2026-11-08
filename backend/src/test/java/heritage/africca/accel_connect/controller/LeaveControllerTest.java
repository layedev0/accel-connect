package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.EmployeeAttachmentDTO;
import heritage.africca.accel_connect.dto.LeaveRequestSummaryDto;
import heritage.africca.accel_connect.config.SecurityConfig;
import heritage.africca.accel_connect.entity.EmployeeAttachment;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.EmployeeDetailsRepository;
import heritage.africca.accel_connect.service.EmployeeAttachmentService;
import heritage.africca.accel_connect.service.LeaveService;
import heritage.africca.accel_connect.service.UserService;
import heritage.africca.accel_connect.entity.LeaveRequest;
import heritage.africca.accel_connect.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveController.class)
@Import(SecurityConfig.class)
@DisplayName("LeaveController")
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private LeaveService leaveService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private EmployeeAttachmentService attachmentService;
    @MockitoBean
    private EmployeeDetailsRepository employeeDetailsRepository;

    private static final String BASE_URL = "/api/v1/leaves";

    private LeaveRequestSummaryDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = LeaveRequestSummaryDto.builder()
                .id(1)
                .employeeName("Alice Diallo")
                .leaveTypeCode("ANNUAL_LEAVE")
                .leaveTypeName("Congé annuel")
                .dateApplied(LocalDate.now().minusDays(1))
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(9))
                .totalDays(BigDecimal.valueOf(2))
                .overallStatus("En attente")
                .days(List.of())
                .comments(List.of())
                .build();
    }

    @Nested
    @DisplayName("GET /pending-requests")
    class GetAllPendingRequests {

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 200 with pending requests for HR_MANAGER")
        void returns200ForHrManager() throws Exception {
            User user = new User();
            user.setId(1L);
            Set<String> roles = Set.of("ROLE_HR_MANAGER");
            
            when(userService.getCurrentUser(any(Authentication.class))).thenReturn(user);
            when(leaveService.getAllPendingRequests(1L, roles)).thenReturn(List.of(sampleDto));

            mockMvc.perform(get(BASE_URL + "/pending-requests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].employeeName").value("Alice Diallo"))
                    .andExpect(jsonPath("$[0].leaveTypeName").value("Congé annuel"))
                    .andExpect(jsonPath("$[0].overallStatus").value("En attente"))
                    .andExpect(jsonPath("$[0].totalDays").value(2));

            verify(userService).getCurrentUser(any(Authentication.class));
            verify(leaveService).getAllPendingRequests(1L, roles);
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("returns 200 with pending requests for MANAGER")
        void returns200ForManager() throws Exception {
            User user = new User();
            user.setId(2L);
            Set<String> roles = Set.of("ROLE_MANAGER");
            
            when(userService.getCurrentUser(any(Authentication.class))).thenReturn(user);
            when(leaveService.getAllPendingRequests(2L, roles)).thenReturn(List.of(sampleDto));

            mockMvc.perform(get(BASE_URL + "/pending-requests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].employeeName").value("Alice Diallo"));

            verify(userService).getCurrentUser(any(Authentication.class));
            verify(leaveService).getAllPendingRequests(2L, roles);
        }

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 200 with empty array when no pending requests")
        void returns200WithEmptyList() throws Exception {
            User user = new User();
            user.setId(1L);
            Set<String> roles = Set.of("ROLE_HR_MANAGER");
            
            when(userService.getCurrentUser(any(Authentication.class))).thenReturn(user);
            when(leaveService.getAllPendingRequests(1L, roles)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/pending-requests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("returns 403 for user without MANAGER or HR_MANAGER role")
        void returns403ForRegularEmployee() throws Exception {
            mockMvc.perform(get(BASE_URL + "/pending-requests"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 401 for unauthenticated request")
        void returns401ForUnauthenticated() throws Exception {
            mockMvc.perform(get(BASE_URL + "/pending-requests"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns multiple pending requests in response")
        void returnsMultiplePendingRequests() throws Exception {
            User user = new User();
            user.setId(1L);
            Set<String> roles = Set.of("ROLE_HR_MANAGER");
            
            LeaveRequestSummaryDto second = LeaveRequestSummaryDto.builder()
                    .id(2)
                    .employeeName("Moussa Ndiaye")
                    .leaveTypeCode("ANNUAL_LEAVE")
                    .leaveTypeName("Congé annuel")
                    .dateApplied(LocalDate.now())
                    .startDate(LocalDate.now().plusDays(10))
                    .endDate(LocalDate.now().plusDays(12))
                    .totalDays(BigDecimal.valueOf(3))
                    .overallStatus("En attente")
                    .days(List.of())
                    .comments(List.of())
                    .build();

            when(userService.getCurrentUser(any(Authentication.class))).thenReturn(user);
            when(leaveService.getAllPendingRequests(1L, roles)).thenReturn(List.of(sampleDto, second));

            mockMvc.perform(get(BASE_URL + "/pending-requests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].employeeName").value("Alice Diallo"))
                    .andExpect(jsonPath("$[1].employeeName").value("Moussa Ndiaye"));
        }
    }

    @Nested
    @DisplayName("GET /leave-requests/{id}")
    class GetLeaveRequest {

        @Test
        @WithMockUser
        @DisplayName("returns 200 when employee views their own request")
        void returns200WhenViewingOwnRequest() throws Exception {
            User user = new User();
            user.setId(1L);
            
            LeaveRequest request = LeaveRequest.builder()
                    .id(1)
                    .user(user)
                    .build();
            
            Set<String> roles = Set.of("ROLE_USER");
            when(userService.getCurrentUser(any(Authentication.class))).thenReturn(user);
            when(leaveService.getLeaveRequestById(1)).thenReturn(request);

            mockMvc.perform(get(BASE_URL + "/leave-requests/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("returns 200 when HR_MANAGER requests any leave request")
        void returns200WhenHrManagerViewsRequest() throws Exception {
            User requester = new User();
            requester.setId(2L);
            
            User hrManager = new User();
            hrManager.setId(1L);
            
            LeaveRequest request = LeaveRequest.builder()
                    .id(10)
                    .user(requester)
                    .build();
            
            Set<String> roles = Set.of("ROLE_HR_MANAGER");
            when(userService.getCurrentUser(any(Authentication.class))).thenReturn(hrManager);
            when(leaveService.getLeaveRequestById(10)).thenReturn(request);

            mockMvc.perform(get(BASE_URL + "/leave-requests/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("returns 403 when non-approver manager tries to view request")
        void returns403WhenNonApproverManageTries() throws Exception {
            User requester = new User();
            requester.setId(2L);
            
            User manager = new User();
            manager.setId(5L);
            
            LeaveRequest request = LeaveRequest.builder()
                    .id(10)
                    .user(requester)
                    .approvers(List.of())
                    .build();
            
            Set<String> roles = Set.of("ROLE_MANAGER");
            when(userService.getCurrentUser(any(Authentication.class))).thenReturn(manager);
            when(leaveService.getLeaveRequestById(10)).thenReturn(request);
            doThrow(new AccessDeniedException("You do not have permission to view this leave request"))
                    .when(leaveService).validateLeaveRequestViewAccess(request, 5L, roles);

            mockMvc.perform(get(BASE_URL + "/leave-requests/10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 401 for unauthenticated request")
        void returns401ForUnauthenticated() throws Exception {
            mockMvc.perform(get(BASE_URL + "/leave-requests/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class ApproveLeaveRequest {

        @Test
        @WithMockUser(roles = "MANAGER")
        void returns200ForManagerWithValidRequest() throws Exception {
            // Arrange: simulate an authenticated manager and successful service call
            User user = new User();
            user.setId(1L);

            when(userService.getCurrentUser(any(Authentication.class)))
                    .thenReturn(user);
            when(leaveService.approveLeaveRequest(eq(1), any(), eq(1L), anySet()))
                    .thenReturn(new LeaveRequest());

            // Act & Assert
            mockMvc.perform(patch(BASE_URL + "/leave-requests/1/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        void returns403ForRegularEmployee() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/leave-requests/1/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /leave-requests/{id}/reject")
    class RejectLeaveRequest {

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        void returns200ForHrManagerWithValidRequest() throws Exception {
            // Arrange: simulate an authenticated HR manager and successful service call
            User user = new User();
            user.setId(1L);

            when(userService.getCurrentUser(any(Authentication.class)))
                    .thenReturn(user);
            when(leaveService.rejectLeaveRequest(eq(1), any(), eq(1L), anySet()))
                    .thenReturn(new LeaveRequest());

            // Act & Assert
            mockMvc.perform(patch(BASE_URL + "/leave-requests/1/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        void returns403ForRegularEmployee() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/leave-requests/1/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ListLeaveRequestAttachments {

        private static final Integer LEAVE_REQUEST_ID = 42;
        private static final String  ATTACH_URL =
                BASE_URL + "/leave-requests/{leaveRequestId}/attachments";

        @Test
        @WithMockUser
        void returns200WithDtos() throws Exception {
            EmployeeAttachmentDTO dto = new EmployeeAttachmentDTO(
                    1L, 1, "Justificatif", "doc.pdf", 1024,
                    "application/pdf", "LEAVE_REQUEST_42",
                    "Alice Diallo", LocalDateTime.of(2026, 3, 1, 9, 0));

            LeaveRequest leaveRequest = LeaveRequest.builder().id(LEAVE_REQUEST_ID).build();
            when(leaveService.getLeaveRequestById(LEAVE_REQUEST_ID)).thenReturn(leaveRequest);
            when(attachmentService.listLeaveRequestAttachments(LEAVE_REQUEST_ID))
                    .thenReturn(List.of(dto));

            mockMvc.perform(get(ATTACH_URL, LEAVE_REQUEST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].filename").value("doc.pdf"))
                    .andExpect(jsonPath("$[0].fileType").value("application/pdf"));
        }

        @Test
        @WithMockUser
        void returns200WithEmptyList() throws Exception {
            LeaveRequest leaveRequest = LeaveRequest.builder().id(LEAVE_REQUEST_ID).build();
            when(leaveService.getLeaveRequestById(LEAVE_REQUEST_ID)).thenReturn(leaveRequest);
            when(attachmentService.listLeaveRequestAttachments(LEAVE_REQUEST_ID))
                    .thenReturn(List.of());

            mockMvc.perform(get(ATTACH_URL, LEAVE_REQUEST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @WithMockUser
        void returns404WhenLeaveRequestNotFound() throws Exception {
            when(leaveService.getLeaveRequestById(LEAVE_REQUEST_ID))
                    .thenThrow(new ResourceNotFoundException("Not found"));

            mockMvc.perform(get(ATTACH_URL, LEAVE_REQUEST_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns401ForUnauthenticated() throws Exception {
            mockMvc.perform(get(ATTACH_URL, LEAVE_REQUEST_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /leave-requests/{leaveRequestId}/attachments/{attachId}/download")
    class DownloadLeaveRequestAttachment {

        private static final Integer LEAVE_REQUEST_ID = 42;
        private static final Integer ATTACH_ID        = 1;
        private static final String  DOWNLOAD_URL     =
                BASE_URL + "/leave-requests/{leaveRequestId}/attachments/{attachId}/download";

        @Test
        @WithMockUser
        void returnsFileBytes() throws Exception {
            EmployeeAttachment entity = new EmployeeAttachment();
            entity.setFilename("doc.pdf");
            entity.setFileType("application/pdf");
            entity.setAttachment(new byte[]{1, 2, 3});

            when(attachmentService.downloadLeaveRequestAttachment(LEAVE_REQUEST_ID, ATTACH_ID))
                    .thenReturn(entity);

            mockMvc.perform(get(DOWNLOAD_URL, LEAVE_REQUEST_ID, ATTACH_ID))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "inline; filename=\"doc.pdf\""))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(content().bytes(new byte[]{1, 2, 3}));
        }

        @Test
        @WithMockUser
        void returns404WhenNotFound() throws Exception {
            when(attachmentService.downloadLeaveRequestAttachment(LEAVE_REQUEST_ID, 999))
                    .thenThrow(new ResourceNotFoundException("Not found"));

            mockMvc.perform(get(DOWNLOAD_URL, LEAVE_REQUEST_ID, 999))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns401ForUnauthenticated() throws Exception {
            mockMvc.perform(get(DOWNLOAD_URL, LEAVE_REQUEST_ID, ATTACH_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
