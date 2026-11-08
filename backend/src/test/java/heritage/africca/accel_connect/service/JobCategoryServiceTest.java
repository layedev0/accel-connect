package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.JobCategoryRequest;
import heritage.africca.accel_connect.entity.JobCategory;
import heritage.africca.accel_connect.exception.DuplicateResourceException;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.JobCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobCategoryServiceTest {

    @Mock
    private JobCategoryRepository jobCategoryRepository;

    @InjectMocks
    private JobCategoryService jobCategoryService;

    private JobCategory engineering;
    private JobCategory hr;

    @BeforeEach
    void setUp() {
        // (id, name, categoryCode, salaireBase, description, code, employees)
        engineering = new JobCategory(1L, "Engineering", "ENG", new BigDecimal("120000.00"), "IT roles", "001", null);
        hr = new JobCategory(2L, "Human Resources", "HR", new BigDecimal("90000.00"), "HR department", "002", null);
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("returns all job categories")
        void returnsAllJobCategories() {
            when(jobCategoryRepository.findAll()).thenReturn(List.of(engineering, hr));

            List<JobCategory> result = jobCategoryService.findAll();

            assertEquals(2, result.size());
            assertTrue(result.contains(engineering));
            assertTrue(result.contains(hr));
            verify(jobCategoryRepository).findAll();
        }

        @Test
        @DisplayName("returns empty list when no categories exist")
        void returnsEmptyListWhenNoCategories() {
            when(jobCategoryRepository.findAll()).thenReturn(List.of());

            List<JobCategory> result = jobCategoryService.findAll();

            assertTrue(result.isEmpty());
            verify(jobCategoryRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getJobCategoryById")
    class GetByIdTests {

        @Test
        @DisplayName("returns job category when found")
        void returnsJobCategoryWhenFound() {
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(engineering));

            JobCategory result = jobCategoryService.getJobCategoryById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Engineering", result.getName());
            assertEquals("001", result.getCode());
            assertEquals("ENG", result.getCategoryCode());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            when(jobCategoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> jobCategoryService.getJobCategoryById(999L));
        }
    }

    @Nested
    @DisplayName("createJobCategory")
    class CreateTests {

        @Test
        @DisplayName("creates job category with explicit 3-digit code")
        void createsWithExplicitCode() {
            // (name, description, code, categoryCode, salaireBase)
            JobCategoryRequest input = new JobCategoryRequest("Engineering", "IT roles", "001", "ENG", new BigDecimal("120000.00"));
            when(jobCategoryRepository.existsByCode("001")).thenReturn(false);
            when(jobCategoryRepository.existsByName("Engineering")).thenReturn(false);
            when(jobCategoryRepository.existsByCategoryCode("ENG")).thenReturn(false);
            when(jobCategoryRepository.save(any(JobCategory.class))).thenAnswer(inv -> {
                JobCategory c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });

            JobCategory result = jobCategoryService.createJobCategory(input);

            assertNotNull(result.getId());
            assertEquals("Engineering", result.getName());
            assertEquals("IT roles", result.getDescription());
            assertEquals("001", result.getCode());
            assertEquals("ENG", result.getCategoryCode());
            assertEquals(new BigDecimal("120000.00"), result.getSalaireBase());
            verify(jobCategoryRepository).existsByCode("001");
            verify(jobCategoryRepository).existsByCategoryCode("ENG");
            verify(jobCategoryRepository).save(any(JobCategory.class));
        }

        @Test
        @DisplayName("normalizes code to 3 digits (5 -> 005)")
        void normalizesCodeToThreeDigits() {
            JobCategoryRequest input = new JobCategoryRequest("Sales", "Sales dept", "5", "SLS", null);
            when(jobCategoryRepository.existsByCode("005")).thenReturn(false);
            when(jobCategoryRepository.existsByName("Sales")).thenReturn(false);
            when(jobCategoryRepository.existsByCategoryCode("SLS")).thenReturn(false);
            when(jobCategoryRepository.save(any(JobCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            JobCategory result = jobCategoryService.createJobCategory(input);

            assertEquals("005", result.getCode());
        }

        @Test
        @DisplayName("throws when code is blank")
        void throwsWhenCodeBlank() {
            JobCategoryRequest input = new JobCategoryRequest("Marketing", "Marketing dept", "", "MKT", null);

            assertThrows(IllegalArgumentException.class, () -> jobCategoryService.createJobCategory(input));
            verify(jobCategoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when code format is invalid (non-numeric)")
        void throwsWhenCodeFormatInvalid() {
            JobCategoryRequest input = new JobCategoryRequest("Engineering", null, "ABC", "ENG", null);

            assertThrows(IllegalArgumentException.class, () -> jobCategoryService.createJobCategory(input));
            verify(jobCategoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when code is duplicate")
        void throwsWhenCodeDuplicate() {
            JobCategoryRequest input = new JobCategoryRequest("Engineering", null, "001", "ENG", null);
            when(jobCategoryRepository.existsByCode("001")).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> jobCategoryService.createJobCategory(input));
            verify(jobCategoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when name already exists")
        void throwsWhenNameDuplicate() {
            JobCategoryRequest input = new JobCategoryRequest("Engineering", "IT roles", "005", "ENG", null);
            when(jobCategoryRepository.existsByCode("005")).thenReturn(false);
            when(jobCategoryRepository.existsByName("Engineering")).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> jobCategoryService.createJobCategory(input));
            verify(jobCategoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when code out of range (0 or 1000)")
        void throwsWhenCodeOutOfRange() {
            JobCategoryRequest inputZero = new JobCategoryRequest("Test", null, "0", "TST", null);
            JobCategoryRequest inputThousand = new JobCategoryRequest("Test", null, "1000", "TST", null);

            assertThrows(IllegalArgumentException.class, () -> jobCategoryService.createJobCategory(inputZero));
            assertThrows(IllegalArgumentException.class, () -> jobCategoryService.createJobCategory(inputThousand));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when categoryCode already exists")
        void throwsWhenCategoryCodeDuplicate() {
            JobCategoryRequest input = new JobCategoryRequest("Engineering", "IT roles", "005", "ENG", null);
            when(jobCategoryRepository.existsByCode("005")).thenReturn(false);
            when(jobCategoryRepository.existsByName("Engineering")).thenReturn(false);
            when(jobCategoryRepository.existsByCategoryCode("ENG")).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> jobCategoryService.createJobCategory(input));
            verify(jobCategoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates with salaireBase set correctly")
        void createsWithSalaireBase() {
            BigDecimal salary = new BigDecimal("75361.00");
            JobCategoryRequest input = new JobCategoryRequest("Catégorie 2ème", null, "002", "CAT2", salary);
            when(jobCategoryRepository.existsByCode("002")).thenReturn(false);
            when(jobCategoryRepository.existsByName("Catégorie 2ème")).thenReturn(false);
            when(jobCategoryRepository.existsByCategoryCode("CAT2")).thenReturn(false);
            when(jobCategoryRepository.save(any(JobCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            JobCategory result = jobCategoryService.createJobCategory(input);

            assertEquals(salary, result.getSalaireBase());
        }
    }

    @Nested
    @DisplayName("updateJobCategory")
    class UpdateTests {

        @Test
        @DisplayName("updates job category when found")
        void updatesWhenFound() {
            JobCategoryRequest input = new JobCategoryRequest("Engineering Updated", "Updated desc", "001", "ENG", new BigDecimal("125000.00"));
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(engineering));
            when(jobCategoryRepository.existsByCodeAndIdNot("001", 1L)).thenReturn(false);
            when(jobCategoryRepository.existsByNameAndIdNot("Engineering Updated", 1L)).thenReturn(false);
            when(jobCategoryRepository.existsByCategoryCodeAndIdNot("ENG", 1L)).thenReturn(false);
            when(jobCategoryRepository.save(any(JobCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            JobCategory result = jobCategoryService.updateJobCategory(1L, input);

            assertNotNull(result);
            assertEquals("Engineering Updated", result.getName());
            assertEquals("Updated desc", result.getDescription());
            assertEquals("001", result.getCode());
            assertEquals("ENG", result.getCategoryCode());
            assertEquals(new BigDecimal("125000.00"), result.getSalaireBase());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id not found")
        void throwsWhenNotFound() {
            JobCategoryRequest input = new JobCategoryRequest("Updated", null, "001", "UPD", null);
            when(jobCategoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> jobCategoryService.updateJobCategory(999L, input));
            verify(jobCategoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("keeps existing code when update has no code")
        void keepsExistingCodeWhenNotProvided() {
            JobCategoryRequest input = new JobCategoryRequest("Updated Name", null, null, "ENG", null);
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(engineering));
            when(jobCategoryRepository.existsByCodeAndIdNot("001", 1L)).thenReturn(false);
            when(jobCategoryRepository.existsByNameAndIdNot("Updated Name", 1L)).thenReturn(false);
            when(jobCategoryRepository.existsByCategoryCodeAndIdNot("ENG", 1L)).thenReturn(false);
            when(jobCategoryRepository.save(any(JobCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            JobCategory result = jobCategoryService.updateJobCategory(1L, input);

            assertNotNull(result);
            assertEquals("001", result.getCode());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when updated name already exists")
        void throwsWhenUpdatedNameDuplicate() {
            JobCategoryRequest input = new JobCategoryRequest("Human Resources", "Updated", "001", "ENG", null);
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(engineering));
            when(jobCategoryRepository.existsByCodeAndIdNot("001", 1L)).thenReturn(false);
            when(jobCategoryRepository.existsByNameAndIdNot("Human Resources", 1L)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> jobCategoryService.updateJobCategory(1L, input));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when updated code is duplicate")
        void throwsWhenUpdatedCodeDuplicate() {
            JobCategoryRequest input = new JobCategoryRequest("Engineering", null, "002", "ENG", null);
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(engineering));
            when(jobCategoryRepository.existsByCodeAndIdNot("002", 1L)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> jobCategoryService.updateJobCategory(1L, input));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when updated categoryCode is duplicate")
        void throwsWhenUpdatedCategoryCodeDuplicate() {
            JobCategoryRequest input = new JobCategoryRequest("Engineering", null, "001", "HR", null);
            when(jobCategoryRepository.findById(1L)).thenReturn(Optional.of(engineering));
            when(jobCategoryRepository.existsByCodeAndIdNot("001", 1L)).thenReturn(false);
            when(jobCategoryRepository.existsByNameAndIdNot("Engineering", 1L)).thenReturn(false);
            when(jobCategoryRepository.existsByCategoryCodeAndIdNot("HR", 1L)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> jobCategoryService.updateJobCategory(1L, input));
        }
    }
}
