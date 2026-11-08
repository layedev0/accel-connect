package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.UserLeaveBalanceDto;
import heritage.africca.accel_connect.config.SecurityConfig;
import heritage.africca.accel_connect.service.LeaveBalanceService;
import heritage.africca.accel_connect.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveBalanceController.class)
@Import(SecurityConfig.class)
@DisplayName("LeaveBalanceController")
class LeaveBalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private LeaveBalanceService leaveBalanceService;
    @MockitoBean
    private UserService userService;

    private static final String BASE_URL = "/api/v1/leave-balances";

    private UserLeaveBalanceDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = UserLeaveBalanceDto.builder()
                .userId(1L)
                .userName("John Doe")
                .userEmail("john@example.com")
                .balanceDays(BigDecimal.valueOf(10))
                .lastUpdated(LocalDate.now())
                .build();
    }

    // ── GET /leave-balances ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /leave-balances")
    class ListAll {

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("HR_MANAGER: returns 200 with list of balances")
        void hrManagerReturnsListOk() throws Exception {
            when(leaveBalanceService.listAllBalances()).thenReturn(List.of(sampleDto));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(1))
                    .andExpect(jsonPath("$[0].userName").value("John Doe"))
                    .andExpect(jsonPath("$[0].balanceDays").value(10));
        }

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("HR_MANAGER: returns 200 with empty list when no balances")
        void hrManagerReturnsEmptyList() throws Exception {
            when(leaveBalanceService.listAllBalances()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("non-HR_MANAGER: returns 403 Forbidden")
        void nonHrManagerReturnsForbidden() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated: returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /leave-balances/{userId} ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /leave-balances/{userId}")
    class GetByUser {

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("HR_MANAGER: returns 200 with balance when exists")
        void hrManagerReturnsOkWhenExists() throws Exception {
            when(leaveBalanceService.getBalanceForUser(1L)).thenReturn(sampleDto);

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.userName").value("John Doe"))
                    .andExpect(jsonPath("$.balanceDays").value(10));
        }

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("HR_MANAGER: returns 404 when balance not found")
        void hrManagerReturns404WhenNotFound() throws Exception {
            when(leaveBalanceService.getBalanceForUser(999L)).thenReturn(null);

            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("non-HR_MANAGER: returns 403 Forbidden")
        void nonHrManagerReturnsForbidden() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated: returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── PUT /leave-balances/{userId}/add-balance ──────────────────────────────

    @Nested
    @DisplayName("PUT /leave-balances/{userId}/add-balance")
    class AddBalance {

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("HR_MANAGER: returns 200 and calls service")
        void hrManagerReturnsOkAndAddsBalance() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1/add-balance").param("days", "8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Balance updated successfully"))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.addedDays").value(8));

            verify(leaveBalanceService).addBalanceForUser(eq(1L), eq(BigDecimal.valueOf(8)));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("non-HR_MANAGER: returns 403 Forbidden")
        void nonHrManagerReturnsForbidden() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1/add-balance").param("days", "8"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated: returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1/add-balance").param("days", "8"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── PUT /leave-balances/{userId}/deduct-balance ───────────────────────────

    @Nested
    @DisplayName("PUT /leave-balances/{userId}/deduct-balance")
    class DeductBalance {

        @Test
        @WithMockUser(roles = "HR_MANAGER")
        @DisplayName("HR_MANAGER: returns 200 and calls service")
        void hrManagerReturnsOkAndDeductsBalance() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1/deduct-balance").param("days", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Balance deducted successfully"))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.deductedDays").value(5));

            verify(leaveBalanceService).deductBalanceForUser(eq(1L), eq(BigDecimal.valueOf(5)));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("non-HR_MANAGER: returns 403 Forbidden")
        void nonHrManagerReturnsForbidden() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1/deduct-balance").param("days", "5"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated: returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1/deduct-balance").param("days", "5"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
