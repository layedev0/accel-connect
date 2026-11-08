package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.entity.EmployeeDetails;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.repository.EmployeeDetailsRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.Impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeDetailsRepository employeeDetailsRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private Jwt jwt;
    private User existingUser;

    @BeforeEach
    void setUp() {
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("keycloak-id-123");
        when(jwt.getClaim("preferred_username")).thenReturn("jdoe");
        when(jwt.getClaim("email")).thenReturn("jdoe@example.com");
        when(jwt.getClaim("given_name")).thenReturn("John");
        when(jwt.getClaim("family_name")).thenReturn("Doe");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setKeycloakId("keycloak-id-123");
        existingUser.setUsername("jdoe");
        existingUser.setEmail("jdoe@example.com");
        existingUser.setFirstName("John");
        existingUser.setLastName("Doe");
    }

    @Nested
    class GetOrCreateUserFromJwtTests {


        void existingUserWithEmployeeDetails() {
            when(userRepository.findByKeycloakId("keycloak-id-123")).thenReturn(Optional.of(existingUser));
            when(employeeDetailsRepository.findByUserId(1L)).thenReturn(Optional.of(new EmployeeDetails()));

            User result = userService.getOrCreateUserFromJwt(jwt);

            assertEquals(existingUser, result);
            verify(employeeDetailsRepository, never()).save(any());
        }

        void existingUserWithoutEmployeeDetails() {
            when(userRepository.findByKeycloakId("keycloak-id-123")).thenReturn(Optional.of(existingUser));
            when(employeeDetailsRepository.findByUserId(1L)).thenReturn(Optional.empty());

            userService.getOrCreateUserFromJwt(jwt);

            ArgumentCaptor<EmployeeDetails> captor = ArgumentCaptor.forClass(EmployeeDetails.class);
            verify(employeeDetailsRepository).save(captor.capture());
            assertEquals(existingUser, captor.getValue().getUser());
        }

        void newUserGetsUserAndStubCreated() {
            when(userRepository.findByKeycloakId("keycloak-id-123")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(2L);
                return u;
            });
            when(employeeDetailsRepository.findByUserId(2L)).thenReturn(Optional.empty());

            User result = userService.getOrCreateUserFromJwt(jwt);

            assertEquals("jdoe", result.getUsername());
            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());

            ArgumentCaptor<EmployeeDetails> captor = ArgumentCaptor.forClass(EmployeeDetails.class);
            verify(employeeDetailsRepository).save(captor.capture());
            assertEquals(result, captor.getValue().getUser());
        }

        void stubHasOnlyUserLinked() {
            when(userRepository.findByKeycloakId("keycloak-id-123")).thenReturn(Optional.of(existingUser));
            when(employeeDetailsRepository.findByUserId(1L)).thenReturn(Optional.empty());

            userService.getOrCreateUserFromJwt(jwt);

            ArgumentCaptor<EmployeeDetails> captor = ArgumentCaptor.forClass(EmployeeDetails.class);
            verify(employeeDetailsRepository).save(captor.capture());
            EmployeeDetails stub = captor.getValue();
            assertNull(stub.getJobTitle());
            assertNull(stub.getDepartment());
            assertNull(stub.getTypeContrat());
            assertNull(stub.getBirthday());
        }
    }

    @Nested
    class CreateUserFromJwtTests {

        @Test
        void mapsJwtClaimsToUser() {
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            User result = userService.createUserFromJwt(jwt, "keycloak-id-123");

            assertEquals("keycloak-id-123", result.getKeycloakId());
            assertEquals("jdoe", result.getUsername());
            assertEquals("jdoe@example.com", result.getEmail());
            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    class GetCurrentUserTests {

        @Test
        void returnsUserForValidAuthentication() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(jwt);
            when(userRepository.findByKeycloakId("keycloak-id-123")).thenReturn(Optional.of(existingUser));
            when(employeeDetailsRepository.findByUserId(1L)).thenReturn(Optional.of(new EmployeeDetails()));

            User result = userService.getCurrentUser(auth);

            assertEquals(existingUser, result);
        }

        @Test
        void throwsWhenAuthenticationNull() {
            assertThrows(RuntimeException.class, () -> userService.getCurrentUser(null));
        }

        @Test
        void throwsWhenPrincipalNotJwt() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn("not-a-jwt");

            assertThrows(RuntimeException.class, () -> userService.getCurrentUser(auth));
        }
    }

    @Nested
    class UpdateUserTests {

        @Test
        void updatesFields() {
            User update = new User();
            update.setFirstName("Jane");
            update.setLastName("Smith");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(1L, update);

            assertEquals("Jane", result.getFirstName());
            assertEquals("Smith", result.getLastName());
            verify(userRepository).save(existingUser);
        }

        @Test
        void keepsExistingWhenUpdateFieldsAreNull() {
            User update = new User();

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUser(1L, update);

            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());
        }

        @Test
        void throwsWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> userService.updateUser(999L, new User()));
            verify(userRepository, never()).save(any());
        }
    }
}
