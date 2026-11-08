package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.EmployeInfoPersonnelDTO;
import heritage.africca.accel_connect.dto.EmployeeDetailsRequest;
import heritage.africca.accel_connect.dto.UpcomingBirthdayDTO;
import heritage.africca.accel_connect.entity.Department;
import heritage.africca.accel_connect.entity.EmployeeDetails;
import heritage.africca.accel_connect.entity.JobCategory;
import heritage.africca.accel_connect.entity.JobTitle;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.mapper.UpcomingBirthdayMapper;
import heritage.africca.accel_connect.repository.DepartmentRepository;
import heritage.africca.accel_connect.repository.EmployeeDetailsRepository;
import heritage.africca.accel_connect.repository.JobCategoryRepository;
import heritage.africca.accel_connect.repository.JobTitleRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeDetailsServiceTest {

    @Mock
    private EmployeeDetailsRepository employeeDetailsRepository;

    @Mock
    private JobTitleRepository jobTitleRepository;

    @Mock
    private JobCategoryRepository jobCategoryRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UpcomingBirthdayMapper upcomingBirthdayMapper;

    @InjectMocks
    private EmployeeDetailsService employeeDetailsService;

    private Department department;
    private JobTitle jobTitle;
    private JobCategory jobCategory;
    private User user;
    private EmployeeDetails existing;

    @BeforeEach
    void setUp() {
        department = new Department(1L, "IT", "001", "Information Technology", null);
        jobTitle = new JobTitle(1L, "Developer", null, "001", null);
        jobCategory = new JobCategory(1L, "Engineering", "ENG", null, "Engineering dept", "001", null);
        user = new User(10L, "kc-1", "john", "john@example.com", "John", "Doe", false, new java.util.HashSet<>(), null, null);

        existing = new EmployeeDetails(
                100L,
                LocalDate.of(1990, 5, 15),
                "M",
                "SINGLE",
                "CNI123",
                jobTitle,
                jobCategory,
                department,
                "+221771234567",
                LocalDate.of(2020, 1, 10),
                "ACTIVE",
                15,
                5,
                user
        );
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("returns all employee details")
        void returnsAllEmployeeDetails() {
            when(employeeDetailsRepository.findAll()).thenReturn(List.of(existing));

            List<EmployeeDetails> result = employeeDetailsService.findAll();

            assertEquals(1, result.size());
            assertTrue(result.contains(existing));
            verify(employeeDetailsRepository).findAll();
        }

        @Test
        @DisplayName("returns empty list when none exist")
        void returnsEmptyListWhenNone() {
            when(employeeDetailsRepository.findAll()).thenReturn(List.of());

            List<EmployeeDetails> result = employeeDetailsService.findAll();

            assertTrue(result.isEmpty());
            verify(employeeDetailsRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("returns employee details when found")
        void returnsWhenFound() {
            when(employeeDetailsRepository.findByIdWithAssociations(100L)).thenReturn(Optional.of(existing));

            EmployeeDetails result = employeeDetailsService.getById(100L);

            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("ACTIVE", result.getTypeContrat());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            when(employeeDetailsRepository.findByIdWithAssociations(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> employeeDetailsService.getById(999L));
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("creates employee details with user")
        void createsWithUser() {
            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    LocalDate.of(1990, 5, 15),
                    "M",
                    "SINGLE",
                    "CNI123",
                    1L,
                    1L,
                    1L,
                    "+221771234567",
                    LocalDate.of(2020, 1, 10),
                    "ACTIVE",
                    10L,
                    null
            );

            when(jobTitleRepository.findById(1L)).thenReturn(Optional.of(jobTitle));
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(jobCategory));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(userRepository.findById(10L)).thenReturn(Optional.of(user));
            when(employeeDetailsRepository.save(any(EmployeeDetails.class))).thenAnswer(inv -> {
                EmployeeDetails e = inv.getArgument(0);
                e.setId(100L);
                return e;
            });

            EmployeeDetails result = employeeDetailsService.create(request);

            assertNotNull(result.getId());
            assertEquals("M", result.getSexe());
            assertEquals("SINGLE", result.getMaritalStatus());
            assertEquals("+221771234567", result.getWorkTelephone());
            assertEquals("ACTIVE", result.getTypeContrat());
            assertSame(jobTitle, result.getJobTitle());
            assertSame(jobCategory, result.getJobCategory());
            assertSame(department, result.getDepartment());
            assertSame(user, result.getUser());
            verify(employeeDetailsRepository).save(any(EmployeeDetails.class));
        }

        @Test
        @DisplayName("throws when job title not found")
        void throwsWhenJobTitleNotFound() {
            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    null, null, null, null,
                    1L, 1L, 1L, null, null, null, null, null
            );

            when(jobTitleRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> employeeDetailsService.create(request));
            verify(employeeDetailsRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when job category not found")
        void throwsWhenJobCategoryNotFound() {
            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    null, null, null, null,
                    1L, 1L, 1L, null, null, null, null, null
            );

            when(jobTitleRepository.findById(1L)).thenReturn(Optional.of(jobTitle));
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> employeeDetailsService.create(request));
            verify(employeeDetailsRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when department not found")
        void throwsWhenDepartmentNotFound() {
            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    null, null, null, null,
                    1L, 1L, 1L, null, null, null, null, null
            );

            when(jobTitleRepository.findById(1L)).thenReturn(Optional.of(jobTitle));
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(jobCategory));
            when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> employeeDetailsService.create(request));
            verify(employeeDetailsRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when user not found if userId provided")
        void throwsWhenUserNotFound() {
            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    null, null, null, null,
                    1L, 1L, 1L, null, null, null, 10L, null
            );

            when(jobTitleRepository.findById(1L)).thenReturn(Optional.of(jobTitle));
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(jobCategory));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(userRepository.findById(10L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> employeeDetailsService.create(request));
            verify(employeeDetailsRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("updates employee details when found")
        void updatesWhenFound() {
            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    LocalDate.of(1991, 6, 20),
                    "F",
                    "MARRIED",
                    "CNI999",
                    1L,
                    1L,
                    1L,
                    "+221770000000",
                    LocalDate.of(2021, 2, 1),
                    "INACTIVE",
                    10L,
                    null
            );

            when(employeeDetailsRepository.findById(100L)).thenReturn(Optional.of(existing));
            when(jobTitleRepository.findById(1L)).thenReturn(Optional.of(jobTitle));
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(jobCategory));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(userRepository.findById(10L)).thenReturn(Optional.of(user));
            when(employeeDetailsRepository.save(any(EmployeeDetails.class))).thenAnswer(inv -> inv.getArgument(0));

            EmployeeDetails result = employeeDetailsService.update(100L, request);

            assertEquals("F", result.getSexe());
            assertEquals("MARRIED", result.getMaritalStatus());
            assertEquals("INACTIVE", result.getTypeContrat());
            assertEquals("+221770000000", result.getWorkTelephone());
            assertSame(user, result.getUser());
        }

        @Test
        @DisplayName("clears user when userId is null")
        void clearsUserWhenUserIdNull() {
            existing.setUser(user);

            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    null, null, null, null,
                    1L, 1L, 1L, null, null, null, null, null
            );

            when(employeeDetailsRepository.findById(100L)).thenReturn(Optional.of(existing));
            when(jobTitleRepository.findById(1L)).thenReturn(Optional.of(jobTitle));
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(jobCategory));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(employeeDetailsRepository.save(any(EmployeeDetails.class))).thenAnswer(inv -> inv.getArgument(0));

            EmployeeDetails result = employeeDetailsService.update(100L, request);

            assertNull(result.getUser());
            assertNull(result.getWorkTelephone());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id not found")
        void throwsWhenIdNotFound() {
            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    null, null, null, null,
                    1L, 1L, 1L, null, null, null, null, null
            );

            when(employeeDetailsRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> employeeDetailsService.update(999L, request));
            verify(employeeDetailsRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createOrUpdate")
    class CreateOrUpdateTests {

        @Test
        @DisplayName("creates new employee when id is null")
        void createsWhenIdIsNull() {
            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    LocalDate.of(1992, 3, 20),
                    "F",
                    "SINGLE",
                    "CNI456",
                    1L,
                    1L,
                    1L,
                    "+221771111111",
                    LocalDate.of(2021, 5, 1),
                    "ACTIVE",
                    null,
                    null
            );

            when(jobTitleRepository.findById(1L)).thenReturn(Optional.of(jobTitle));
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(jobCategory));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(employeeDetailsRepository.save(any(EmployeeDetails.class))).thenAnswer(inv -> {
                EmployeeDetails e = inv.getArgument(0);
                e.setId(200L);
                return e;
            });

            EmployeeDetails result = employeeDetailsService.createOrUpdate(request);

            assertNotNull(result.getId());
            assertEquals(200L, result.getId());
            verify(employeeDetailsRepository).save(any(EmployeeDetails.class));
            verify(employeeDetailsRepository, never()).findById(any());
        }

        @Test
        @DisplayName("creates new employee when id is provided but not found")
        void createsWhenIdNotFound() {
            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    LocalDate.of(1988, 7, 10),
                    "M",
                    "MARRIED",
                    "CNI789",
                    1L,
                    1L,
                    1L,
                    "+221772222222",
                    LocalDate.of(2019, 3, 15),
                    "ACTIVE",
                    null,
                    999L
            );

            when(employeeDetailsRepository.findById(999L)).thenReturn(Optional.empty());
            when(jobTitleRepository.findById(1L)).thenReturn(Optional.of(jobTitle));
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(jobCategory));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(employeeDetailsRepository.save(any(EmployeeDetails.class))).thenAnswer(inv -> {
                EmployeeDetails e = inv.getArgument(0);
                e.setId(300L);
                return e;
            });

            EmployeeDetails result = employeeDetailsService.createOrUpdate(request);

            assertNotNull(result.getId());
            assertEquals(300L, result.getId());
            verify(employeeDetailsRepository).findById(999L);
            verify(employeeDetailsRepository).save(any(EmployeeDetails.class));
        }

        @Test
        @DisplayName("updates existing employee when id is provided and exists")
        void updatesWhenIdExists() {
            EmployeeDetailsRequest request = new EmployeeDetailsRequest(
                    LocalDate.of(1991, 6, 20),
                    "F",
                    "MARRIED",
                    "CNI999",
                    1L,
                    1L,
                    1L,
                    "+221770000000",
                    LocalDate.of(2021, 2, 1),
                    "INACTIVE",
                    10L,
                    100L
            );

            when(employeeDetailsRepository.findById(100L)).thenReturn(Optional.of(existing));
            when(jobTitleRepository.findById(1L)).thenReturn(Optional.of(jobTitle));
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(jobCategory));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(userRepository.findById(10L)).thenReturn(Optional.of(user));
            when(employeeDetailsRepository.save(any(EmployeeDetails.class))).thenAnswer(inv -> inv.getArgument(0));

            EmployeeDetails result = employeeDetailsService.createOrUpdate(request);

            assertEquals(100L, result.getId());
            assertEquals("F", result.getSexe());
            assertEquals("MARRIED", result.getMaritalStatus());
            assertEquals("INACTIVE", result.getTypeContrat());
            assertEquals("+221770000000", result.getWorkTelephone());
            assertSame(user, result.getUser());
            verify(employeeDetailsRepository, times(2)).findById(100L); // once in createOrUpdate, once in update
            verify(employeeDetailsRepository).save(existing);
        }
    }

    @Nested
    @DisplayName("getEmployeInfoPersonnelByUserId")
    class GetEmployeInfoPersonnelByUserIdTests {

        @Test
        @DisplayName("maps core fields to DTO")
        void mapsCoreFields() {
            when(employeeDetailsRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

            EmployeInfoPersonnelDTO result = employeeDetailsService.getEmployeInfoPersonnelByUserId(10L);

            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals(10L, result.getUserId());
            assertEquals("john", result.getUsername());
            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());
            assertEquals("CNI123", result.getCni());
            assertEquals("ACTIVE", result.getStatut());
            assertEquals("+221771234567", result.getTelephone());
        }

        @Test
        @DisplayName("maps job title and job category fields when present")
        void mapsJobFieldsWhenPresent() {
            when(employeeDetailsRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

            EmployeInfoPersonnelDTO result = employeeDetailsService.getEmployeInfoPersonnelByUserId(10L);

            assertEquals(1L, result.getJobTitleId());
            assertEquals("Developer", result.getJobTitleName());
            assertEquals(1L, result.getJobCategoryId());
            assertEquals("Engineering", result.getJobCategoryName());
        }

        @Test
        @DisplayName("maps department fields when present")
        void mapsDepartmentWhenPresent() {
            when(employeeDetailsRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

            EmployeInfoPersonnelDTO result = employeeDetailsService.getEmployeInfoPersonnelByUserId(10L);

            assertEquals(1L, result.getDepartmentId());
            assertEquals("IT", result.getDepartmentName());
        }

        @Test
        @DisplayName("job title and category fields are null in DTO when not set on entity")
        void jobFieldsNullWhenNotSet() {
            existing.setJobTitle(null);
            existing.setJobCategory(null);
            when(employeeDetailsRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

            EmployeInfoPersonnelDTO result = employeeDetailsService.getEmployeInfoPersonnelByUserId(10L);

            assertNull(result.getJobTitleId());
            assertNull(result.getJobTitleName());
            assertNull(result.getJobCategoryId());
            assertNull(result.getJobCategoryName());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee not found")
        void throwsWhenNotFound() {
            when(employeeDetailsRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> employeeDetailsService.getEmployeInfoPersonnelByUserId(999L));
        }
    }

    @Nested
    @DisplayName("getEmployeInfoPersonnelByUserIdOptional")
    class GetEmployeInfoPersonnelByUserIdOptionalTests {

        @Test
        @DisplayName("returns present optional with mapped job fields")
        void returnsPresentWithJobFields() {
            when(employeeDetailsRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

            Optional<EmployeInfoPersonnelDTO> result =
                    employeeDetailsService.getEmployeInfoPersonnelByUserIdOptional(10L);

            assertTrue(result.isPresent());
            assertEquals(1L, result.get().getJobTitleId());
            assertEquals("Developer", result.get().getJobTitleName());
            assertEquals(1L, result.get().getJobCategoryId());
            assertEquals("Engineering", result.get().getJobCategoryName());
        }

        @Test
        @DisplayName("returns present optional with null job fields when not set")
        void returnsPresentWithNullJobFields() {
            existing.setJobTitle(null);
            existing.setJobCategory(null);
            when(employeeDetailsRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

            Optional<EmployeInfoPersonnelDTO> result =
                    employeeDetailsService.getEmployeInfoPersonnelByUserIdOptional(10L);

            assertTrue(result.isPresent());
            assertNull(result.get().getJobTitleId());
            assertNull(result.get().getJobTitleName());
            assertNull(result.get().getJobCategoryId());
            assertNull(result.get().getJobCategoryName());
        }

        @Test
        @DisplayName("returns empty optional when employee not found")
        void returnsEmptyWhenNotFound() {
            when(employeeDetailsRepository.findByUserId(999L)).thenReturn(Optional.empty());

            Optional<EmployeInfoPersonnelDTO> result =
                    employeeDetailsService.getEmployeInfoPersonnelByUserIdOptional(999L);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deletes employee details when found")
        void deletesWhenFound() {
            when(employeeDetailsRepository.findById(100L)).thenReturn(Optional.of(existing));

            employeeDetailsService.delete(100L);

            verify(employeeDetailsRepository).findById(100L);
            verify(employeeDetailsRepository).delete(existing);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id not found")
        void throwsWhenIdNotFound() {
            when(employeeDetailsRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> employeeDetailsService.delete(999L));

            verify(employeeDetailsRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("getUpcomingBirthdays")
    class GetUpcomingBirthdaysTests {

        private EmployeeDetails candidate() {
            // Content is irrelevant here: the mapper is mocked, so we only need
            // distinct candidate instances for the repository to return.
            return new EmployeeDetails();
        }

        private UpcomingBirthdayDTO dto(int daysUntil, boolean today, LocalDate nextBirthday) {
            return UpcomingBirthdayDTO.builder()
                    .daysUntil(daysUntil)
                    .today(today)
                    .nextBirthday(nextBirthday)
                    .build();
        }

        @Test
        @DisplayName("returns birthdays within the window ordered soonest first")
        void returnsWithinWindowSortedBySoonest() {
            UpcomingBirthdayDTO soon = dto(5, false, LocalDate.of(2026, 7, 12));
            UpcomingBirthdayDTO todayDto = dto(0, true, LocalDate.of(2026, 7, 7));
            UpcomingBirthdayDTO later = dto(20, false, LocalDate.of(2026, 7, 27));

            when(employeeDetailsRepository.findBirthdayCandidates())
                    .thenReturn(List.of(candidate(), candidate(), candidate()));
            when(upcomingBirthdayMapper.toDto(any(EmployeeDetails.class), any(LocalDate.class)))
                    .thenReturn(soon, todayDto, later);

            List<UpcomingBirthdayDTO> result = employeeDetailsService.getUpcomingBirthdays(30);

            assertEquals(List.of(todayDto, soon, later), result);
        }

        @Test
        @DisplayName("excludes birthdays that fall beyond the window")
        void excludesBeyondWindow() {
            UpcomingBirthdayDTO within = dto(10, false, LocalDate.of(2026, 7, 17));
            UpcomingBirthdayDTO beyond = dto(40, false, LocalDate.of(2026, 8, 16));

            when(employeeDetailsRepository.findBirthdayCandidates())
                    .thenReturn(List.of(candidate(), candidate()));
            when(upcomingBirthdayMapper.toDto(any(EmployeeDetails.class), any(LocalDate.class)))
                    .thenReturn(within, beyond);

            List<UpcomingBirthdayDTO> result = employeeDetailsService.getUpcomingBirthdays(30);

            assertEquals(1, result.size());
            assertSame(within, result.get(0));
        }

        @Test
        @DisplayName("includes a birthday exactly on the window boundary")
        void includesBirthdayOnBoundary() {
            UpcomingBirthdayDTO onBoundary = dto(30, false, LocalDate.of(2026, 8, 6));

            when(employeeDetailsRepository.findBirthdayCandidates())
                    .thenReturn(List.of(candidate()));
            when(upcomingBirthdayMapper.toDto(any(EmployeeDetails.class), any(LocalDate.class)))
                    .thenReturn(onBoundary);

            List<UpcomingBirthdayDTO> result = employeeDetailsService.getUpcomingBirthdays(30);

            assertEquals(1, result.size());
            assertSame(onBoundary, result.get(0));
        }

        @Test
        @DisplayName("filters out candidates with no resolvable next birthday")
        void filtersOutNullNextBirthday() {
            UpcomingBirthdayDTO valid = dto(3, false, LocalDate.of(2026, 7, 10));
            UpcomingBirthdayDTO noDate = dto(0, false, null); // e.g. missing day/month of birth

            when(employeeDetailsRepository.findBirthdayCandidates())
                    .thenReturn(List.of(candidate(), candidate()));
            when(upcomingBirthdayMapper.toDto(any(EmployeeDetails.class), any(LocalDate.class)))
                    .thenReturn(valid, noDate);

            List<UpcomingBirthdayDTO> result = employeeDetailsService.getUpcomingBirthdays(30);

            assertEquals(1, result.size());
            assertSame(valid, result.get(0));
        }

        @Test
        @DisplayName("returns an empty list when there are no candidates")
        void returnsEmptyWhenNoCandidates() {
            when(employeeDetailsRepository.findBirthdayCandidates()).thenReturn(List.of());

            List<UpcomingBirthdayDTO> result = employeeDetailsService.getUpcomingBirthdays(30);

            assertTrue(result.isEmpty());
            verifyNoInteractions(upcomingBirthdayMapper);
        }

        @Test
        @DisplayName("maps each candidate against today's date")
        void mapsCandidatesAgainstToday() {
            when(employeeDetailsRepository.findBirthdayCandidates())
                    .thenReturn(List.of(candidate()));
            when(upcomingBirthdayMapper.toDto(any(EmployeeDetails.class), any(LocalDate.class)))
                    .thenReturn(dto(1, false, LocalDate.now().plusDays(1)));

            employeeDetailsService.getUpcomingBirthdays(30);

            ArgumentCaptor<LocalDate> todayCaptor = ArgumentCaptor.forClass(LocalDate.class);
            verify(upcomingBirthdayMapper).toDto(any(EmployeeDetails.class), todayCaptor.capture());
            assertEquals(LocalDate.now(), todayCaptor.getValue());
        }
    }
}

