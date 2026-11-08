package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.DepartmentRequest;
import heritage.africca.accel_connect.entity.Department;
import heritage.africca.accel_connect.exception.DuplicateResourceException;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department itDept;
    private Department hrDept;

    @BeforeEach
    void setUp() {
        itDept = new Department(1L, "IT", "001", "IT department", null);
        hrDept = new Department(2L, "HR", "002", "HR department", null);
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("returns all departments")
        void returnsAllDepartments() {
            when(departmentRepository.findAll()).thenReturn(List.of(itDept, hrDept));

            List<Department> result = departmentService.findAll();

            assertEquals(2, result.size());
            assertTrue(result.contains(itDept));
            assertTrue(result.contains(hrDept));
            verify(departmentRepository).findAll();
        }

        @Test
        @DisplayName("returns empty list when no departments exist")
        void returnsEmptyListWhenNoDepartments() {
            when(departmentRepository.findAll()).thenReturn(List.of());

            List<Department> result = departmentService.findAll();

            assertTrue(result.isEmpty());
            verify(departmentRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getDepartmentById")
    class GetByIdTests {

        @Test
        @DisplayName("returns department when found")
        void returnsDepartmentWhenFound() {
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(itDept));

            Department result = departmentService.getDepartmentById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("IT", result.getName());
            assertEquals("001", result.getCode());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> departmentService.getDepartmentById(999L));
        }
    }

    @Nested
    @DisplayName("createDepartment")
    class CreateTests {

        @Test
        @DisplayName("creates department with explicit 3-digit code")
        void createsWithExplicitCode() {
            DepartmentRequest input = new DepartmentRequest("IT", "IT department", "001");
            when(departmentRepository.existsByCode("001")).thenReturn(false);
            when(departmentRepository.existsByName("IT")).thenReturn(false);
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
                Department d = inv.getArgument(0);
                d.setId(1L);
                return d;
            });

            Department result = departmentService.createDepartment(input);

            assertNotNull(result.getId());
            assertEquals("IT", result.getName());
            assertEquals("IT department", result.getDescription());
            assertEquals("001", result.getCode());
            verify(departmentRepository).existsByCode("001");
            verify(departmentRepository).existsByName("IT");
            verify(departmentRepository).save(any(Department.class));
        }

        @Test
        @DisplayName("normalizes code to 3 digits (5 -> 005)")
        void normalizesCodeToThreeDigits() {
            DepartmentRequest input = new DepartmentRequest("Sales", "Sales dept", "5");
            when(departmentRepository.existsByCode("005")).thenReturn(false);
            when(departmentRepository.existsByName("Sales")).thenReturn(false);
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

            Department result = departmentService.createDepartment(input);

            assertEquals("005", result.getCode());
        }

        @Test
        @DisplayName("throws when code is blank")
        void throwsWhenCodeBlank() {
            DepartmentRequest input = new DepartmentRequest("Marketing", "Marketing dept", "");

            assertThrows(IllegalArgumentException.class, () -> departmentService.createDepartment(input));
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when code format is invalid (non-numeric)")
        void throwsWhenCodeFormatInvalid() {
            DepartmentRequest input = new DepartmentRequest("IT", null, "ABC");

            assertThrows(IllegalArgumentException.class, () -> departmentService.createDepartment(input));
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when code is out of range (0 or 1000)")
        void throwsWhenCodeOutOfRange() {
            DepartmentRequest inputZero = new DepartmentRequest("Test", null, "0");
            DepartmentRequest inputThousand = new DepartmentRequest("Test", null, "1000");

            assertThrows(IllegalArgumentException.class, () -> departmentService.createDepartment(inputZero));
            assertThrows(IllegalArgumentException.class, () -> departmentService.createDepartment(inputThousand));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when code already exists")
        void throwsWhenCodeDuplicate() {
            DepartmentRequest input = new DepartmentRequest("IT", null, "001");
            when(departmentRepository.existsByCode("001")).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> departmentService.createDepartment(input));
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when name already exists")
        void throwsWhenNameDuplicate() {
            DepartmentRequest input = new DepartmentRequest("IT", "IT roles", "005");
            when(departmentRepository.existsByCode("005")).thenReturn(false);
            when(departmentRepository.existsByName("IT")).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> departmentService.createDepartment(input));
            verify(departmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateDepartment")
    class UpdateTests {

        @Test
        @DisplayName("updates department when found")
        void updatesWhenFound() {
            DepartmentRequest input = new DepartmentRequest("IT Updated", "Updated desc", "001");
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(itDept));
            when(departmentRepository.existsByCodeAndIdNot("001", 1L)).thenReturn(false);
            when(departmentRepository.existsByNameAndIdNot("IT Updated", 1L)).thenReturn(false);
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

            Department result = departmentService.updateDepartment(1L, input);

            assertNotNull(result);
            assertEquals("IT Updated", result.getName());
            assertEquals("Updated desc", result.getDescription());
            assertEquals("001", result.getCode());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id not found")
        void throwsWhenNotFound() {
            DepartmentRequest input = new DepartmentRequest("Updated", null, "001");
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> departmentService.updateDepartment(999L, input));
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("keeps existing code when update has no code")
        void keepsExistingCodeWhenNotProvided() {
            DepartmentRequest input = new DepartmentRequest("Updated Name", null, null);
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(itDept));
            when(departmentRepository.existsByCodeAndIdNot("001", 1L)).thenReturn(false);
            when(departmentRepository.existsByNameAndIdNot("Updated Name", 1L)).thenReturn(false);
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

            Department result = departmentService.updateDepartment(1L, input);

            assertNotNull(result);
            assertEquals("001", result.getCode());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when updated name already exists")
        void throwsWhenUpdatedNameDuplicate() {
            DepartmentRequest input = new DepartmentRequest("HR", "Updated", "001");
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(itDept));
            when(departmentRepository.existsByCodeAndIdNot("001", 1L)).thenReturn(false);
            when(departmentRepository.existsByNameAndIdNot("HR", 1L)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> departmentService.updateDepartment(1L, input));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when updated code is duplicate")
        void throwsWhenUpdatedCodeDuplicate() {
            DepartmentRequest input = new DepartmentRequest("IT", null, "002");
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(itDept));
            when(departmentRepository.existsByCodeAndIdNot("002", 1L)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> departmentService.updateDepartment(1L, input));
        }
    }

    @Nested
    @DisplayName("deleteDepartment")
    class DeleteTests {

        @Test
        @DisplayName("deletes department when it has no employees")
        void deletesWhenNoEmployees() {
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(itDept));
            when(departmentRepository.hasEmployees(1L)).thenReturn(false);

            departmentService.deleteDepartment(1L);

            verify(departmentRepository).findById(1L);
            verify(departmentRepository).hasEmployees(1L);
            verify(departmentRepository).delete(itDept);
        }

        @Test
        @DisplayName("throws IllegalStateException when department has employees")
        void throwsWhenHasEmployees() {
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(itDept));
            when(departmentRepository.hasEmployees(1L)).thenReturn(true);

            assertThrows(IllegalStateException.class, () -> departmentService.deleteDepartment(1L));

            verify(departmentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id not found")
        void throwsWhenIdNotFound() {
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> departmentService.deleteDepartment(999L));

            verify(departmentRepository, never()).hasEmployees(any());
            verify(departmentRepository, never()).delete(any());
        }
    }
}

