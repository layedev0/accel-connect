package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.EmployeeDependentDto;
import heritage.africca.accel_connect.dto.EmployeeDependentSummaryDto;
import heritage.africca.accel_connect.entity.EmployeeDependent;
import heritage.africca.accel_connect.entity.EmployeeDetails;
import heritage.africca.accel_connect.exception.DuplicateResourceException;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.exception.ValidationException;
import heritage.africca.accel_connect.mapper.EmployeeDependentMapper;
import heritage.africca.accel_connect.outils.EmployeeDependentId;
import heritage.africca.accel_connect.repository.EmployeeDependentRepository;
import heritage.africca.accel_connect.repository.EmployeeDetailsRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeDependentServiceTest {

    @Mock
    private EmployeeDependentRepository dependentRepository;

    @Mock
    private EmployeeDetailsRepository employeeRepository;

    @Mock
    private EmployeeDependentMapper dependentMapper;

    @InjectMocks
    private EmployeeDependentService employeeDependentService;

    private static final Long EMPLOYEE_ID = 100L;
    private static final Integer SEQ_NO = 1;

    private EmployeeDetails employee;
    private EmployeeDependent dependent;
    private EmployeeDependentDto dependentDto;
    private EmployeeDependentSummaryDto summaryDto;

    @BeforeEach
    void setUp() {
        employee = new EmployeeDetails();
        employee.setId(EMPLOYEE_ID);

        dependent = new EmployeeDependent();
        dependent.setEmployee(employee);
        dependent.setSeqNo(SEQ_NO);
        dependent.setName("Alice Doe");
        dependent.setRelationship("Spouse");
        dependent.setContactNumber("+221771234567");
        dependent.setDateOfBirth(LocalDate.of(1990, 5, 15));

        dependentDto = EmployeeDependentDto.builder()
                .name("Alice Doe")
                .relationship("Spouse")
                .contactNumber("+221771234567")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();

        summaryDto = EmployeeDependentSummaryDto.builder()
                .seqNo(SEQ_NO)
                .name("Alice Doe")
                .relationship("Spouse")
                .contactNumber("+221771234567")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();
    }

    @Nested
    @DisplayName("createDependent")
    class CreateDependentTests {

        @Test
        @DisplayName("creates dependent successfully")
        void createsDependentSuccessfully() {
            when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
            when(dependentRepository.existsByEmployeeIdAndNameIgnoreCase(EMPLOYEE_ID, "Alice Doe")).thenReturn(false);
            when(dependentRepository.findMaxSeqNoByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());
            when(dependentMapper.toEntity(dependentDto)).thenReturn(new EmployeeDependent());
            when(dependentRepository.save(any(EmployeeDependent.class))).thenAnswer(inv -> {
                EmployeeDependent e = inv.getArgument(0);
                e.setEmployee(employee);
                e.setSeqNo(1);
                e.setName("Alice Doe");
                e.setRelationship("Spouse");
                e.setContactNumber("+221771234567");
                e.setDateOfBirth(LocalDate.of(1990, 5, 15));
                return e;
            });
            when(dependentMapper.toDto(any(EmployeeDependent.class))).thenAnswer(inv -> {
                EmployeeDependent e = inv.getArgument(0);
                return EmployeeDependentDto.builder()
                        .employeeId(e.getEmployee().getId())
                        .seqNo(e.getSeqNo())
                        .name(e.getName())
                        .relationship(e.getRelationship())
                        .contactNumber(e.getContactNumber())
                        .dateOfBirth(e.getDateOfBirth())
                        .build();
            });

            EmployeeDependentDto result = employeeDependentService.createDependent(EMPLOYEE_ID, dependentDto);

            assertNotNull(result);
            assertEquals("Alice Doe", result.getName());
            assertEquals("Spouse", result.getRelationship());
            assertEquals(1, result.getSeqNo());
            verify(employeeRepository).findById(EMPLOYEE_ID);
            verify(dependentRepository).existsByEmployeeIdAndNameIgnoreCase(EMPLOYEE_ID, "Alice Doe");
            verify(dependentRepository).findMaxSeqNoByEmployeeId(EMPLOYEE_ID);
            verify(dependentRepository).save(any(EmployeeDependent.class));
        }

        @Test
        @DisplayName("uses next sequence number when dependents exist")
        void usesNextSequenceNumber() {
            when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
            when(dependentRepository.existsByEmployeeIdAndNameIgnoreCase(EMPLOYEE_ID, "Alice Doe")).thenReturn(false);
            when(dependentRepository.findMaxSeqNoByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(3));
            when(dependentMapper.toEntity(dependentDto)).thenReturn(new EmployeeDependent());
            when(dependentRepository.save(any(EmployeeDependent.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dependentMapper.toDto(any(EmployeeDependent.class))).thenReturn(dependentDto);

            employeeDependentService.createDependent(EMPLOYEE_ID, dependentDto);

            ArgumentCaptor<EmployeeDependent> captor = ArgumentCaptor.forClass(EmployeeDependent.class);
            verify(dependentRepository).save(captor.capture());
            assertEquals(4, captor.getValue().getSeqNo());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee not found")
        void throwsWhenEmployeeNotFound() {
            when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> employeeDependentService.createDependent(999L, dependentDto));
            verify(dependentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when dependent name already exists")
        void throwsWhenDuplicateName() {
            when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
            when(dependentRepository.existsByEmployeeIdAndNameIgnoreCase(EMPLOYEE_ID, "Alice Doe")).thenReturn(true);

            assertThrows(DuplicateResourceException.class,
                    () -> employeeDependentService.createDependent(EMPLOYEE_ID, dependentDto));
            verify(dependentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ValidationException when contact number has invalid format")
        void throwsWhenInvalidContactNumber() {
            dependentDto.setContactNumber("invalid@email");
            when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
            when(dependentRepository.existsByEmployeeIdAndNameIgnoreCase(EMPLOYEE_ID, "Alice Doe")).thenReturn(false);

            assertThrows(ValidationException.class,
                    () -> employeeDependentService.createDependent(EMPLOYEE_ID, dependentDto));
            verify(dependentRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows null or empty contact number")
        void allowsNullContactNumber() {
            dependentDto.setContactNumber(null);
            when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
            when(dependentRepository.existsByEmployeeIdAndNameIgnoreCase(EMPLOYEE_ID, "Alice Doe")).thenReturn(false);
            when(dependentRepository.findMaxSeqNoByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());
            when(dependentMapper.toEntity(dependentDto)).thenReturn(new EmployeeDependent());
            when(dependentRepository.save(any(EmployeeDependent.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dependentMapper.toDto(any(EmployeeDependent.class))).thenReturn(dependentDto);

            assertDoesNotThrow(() -> employeeDependentService.createDependent(EMPLOYEE_ID, dependentDto));
        }
    }

    @Nested
    @DisplayName("updateDependent")
    class UpdateDependentTests {

        @Test
        @DisplayName("updates dependent successfully")
        void updatesDependentSuccessfully() {
            EmployeeDependentDto updateDto = EmployeeDependentDto.builder()
                    .name("Alice Doe Updated")
                    .relationship("Spouse")
                    .contactNumber("+221770000000")
                    .dateOfBirth(LocalDate.of(1990, 5, 15))
                    .build();

            when(dependentRepository.findByEmployeeIdAndSeqNo(EMPLOYEE_ID, SEQ_NO)).thenReturn(Optional.of(dependent));
            when(dependentRepository.existsByEmployeeIdAndNameIgnoreCase(EMPLOYEE_ID, "Alice Doe Updated")).thenReturn(false);
            when(dependentRepository.save(any(EmployeeDependent.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dependentMapper.toDto(any(EmployeeDependent.class))).thenAnswer(inv -> {
                EmployeeDependent e = inv.getArgument(0);
                return EmployeeDependentDto.builder()
                        .employeeId(EMPLOYEE_ID)
                        .seqNo(SEQ_NO)
                        .name(e.getName())
                        .relationship(e.getRelationship())
                        .contactNumber(e.getContactNumber())
                        .dateOfBirth(e.getDateOfBirth())
                        .build();
            });

            EmployeeDependentDto result = employeeDependentService.updateDependent(EMPLOYEE_ID, SEQ_NO, updateDto);

            assertNotNull(result);
            verify(dependentMapper).updateEntity(eq(updateDto), any(EmployeeDependent.class));
            verify(dependentRepository).save(dependent);
        }

        @Test
        @DisplayName("does not check duplicate when name unchanged")
        void noDuplicateCheckWhenNameUnchanged() {
            when(dependentRepository.findByEmployeeIdAndSeqNo(EMPLOYEE_ID, SEQ_NO)).thenReturn(Optional.of(dependent));
            when(dependentRepository.save(any(EmployeeDependent.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dependentMapper.toDto(any(EmployeeDependent.class))).thenReturn(dependentDto);

            employeeDependentService.updateDependent(EMPLOYEE_ID, SEQ_NO, dependentDto);

            verify(dependentRepository, never()).existsByEmployeeIdAndNameIgnoreCase(any(), any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when dependent not found")
        void throwsWhenDependentNotFound() {
            when(dependentRepository.findByEmployeeIdAndSeqNo(EMPLOYEE_ID, 99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> employeeDependentService.updateDependent(EMPLOYEE_ID, 99, dependentDto));
            verify(dependentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when new name already exists")
        void throwsWhenNewNameDuplicate() {
            dependentDto.setName("Bob Doe");
            when(dependentRepository.findByEmployeeIdAndSeqNo(EMPLOYEE_ID, SEQ_NO)).thenReturn(Optional.of(dependent));
            when(dependentRepository.existsByEmployeeIdAndNameIgnoreCase(EMPLOYEE_ID, "Bob Doe")).thenReturn(true);

            assertThrows(DuplicateResourceException.class,
                    () -> employeeDependentService.updateDependent(EMPLOYEE_ID, SEQ_NO, dependentDto));
            verify(dependentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteDependent")
    class DeleteDependentTests {

        @Test
        @DisplayName("deletes dependent successfully")
        void deletesDependentSuccessfully() {
            when(dependentRepository.existsById(new EmployeeDependentId(EMPLOYEE_ID, SEQ_NO))).thenReturn(true);

            employeeDependentService.deleteDependent(EMPLOYEE_ID, SEQ_NO);

            verify(dependentRepository).existsById(new EmployeeDependentId(EMPLOYEE_ID, SEQ_NO));
            verify(dependentRepository).deleteByEmployeeIdAndSeqNo(EMPLOYEE_ID, SEQ_NO);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when dependent not found")
        void throwsWhenDependentNotFound() {
            when(dependentRepository.existsById(new EmployeeDependentId(EMPLOYEE_ID, SEQ_NO))).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> employeeDependentService.deleteDependent(EMPLOYEE_ID, SEQ_NO));
            verify(dependentRepository, never()).deleteByEmployeeIdAndSeqNo(any(), any());
        }
    }

    @Nested
    @DisplayName("deleteAllDependents")
    class DeleteAllDependentsTests {

        @Test
        @DisplayName("deletes all dependents for employee")
        void deletesAllDependents() {
            when(employeeRepository.existsById(EMPLOYEE_ID)).thenReturn(true);

            employeeDependentService.deleteAllDependents(EMPLOYEE_ID);

            verify(employeeRepository).existsById(EMPLOYEE_ID);
            verify(dependentRepository).deleteAllByEmployeeId(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee not found")
        void throwsWhenEmployeeNotFound() {
            when(employeeRepository.existsById(999L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> employeeDependentService.deleteAllDependents(999L));
            verify(dependentRepository, never()).deleteAllByEmployeeId(any());
        }
    }

    @Nested
    @DisplayName("getDependent")
    class GetDependentTests {

        @Test
        @DisplayName("returns dependent when found")
        void returnsDependentWhenFound() {
            when(dependentRepository.findByEmployeeIdAndSeqNo(EMPLOYEE_ID, SEQ_NO)).thenReturn(Optional.of(dependent));
            when(dependentMapper.toDto(dependent)).thenReturn(dependentDto);

            EmployeeDependentDto result = employeeDependentService.getDependent(EMPLOYEE_ID, SEQ_NO);

            assertNotNull(result);
            assertEquals("Alice Doe", result.getName());
            verify(dependentRepository).findByEmployeeIdAndSeqNo(EMPLOYEE_ID, SEQ_NO);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            when(dependentRepository.findByEmployeeIdAndSeqNo(EMPLOYEE_ID, 99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> employeeDependentService.getDependent(EMPLOYEE_ID, 99));
        }
    }

    @Nested
    @DisplayName("getAllDependents")
    class GetAllDependentsTests {

        @Test
        @DisplayName("returns all dependents for employee")
        void returnsAllDependents() {
            when(employeeRepository.existsById(EMPLOYEE_ID)).thenReturn(true);
            when(dependentRepository.findAllByEmployeeIdOrderBySeqNo(EMPLOYEE_ID)).thenReturn(List.of(dependent));
            when(dependentMapper.toSummaryDto(dependent)).thenReturn(summaryDto);

            List<EmployeeDependentSummaryDto> result = employeeDependentService.getAllDependents(EMPLOYEE_ID);

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getAge());
            verify(employeeRepository).existsById(EMPLOYEE_ID);
            verify(dependentRepository).findAllByEmployeeIdOrderBySeqNo(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("returns empty list when no dependents")
        void returnsEmptyListWhenNoDependents() {
            when(employeeRepository.existsById(EMPLOYEE_ID)).thenReturn(true);
            when(dependentRepository.findAllByEmployeeIdOrderBySeqNo(EMPLOYEE_ID)).thenReturn(List.of());

            List<EmployeeDependentSummaryDto> result = employeeDependentService.getAllDependents(EMPLOYEE_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee not found")
        void throwsWhenEmployeeNotFound() {
            when(employeeRepository.existsById(999L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> employeeDependentService.getAllDependents(999L));
            verify(dependentRepository, never()).findAllByEmployeeIdOrderBySeqNo(any());
        }
    }

    @Nested
    @DisplayName("getDependentCount")
    class GetDependentCountTests {

        @Test
        @DisplayName("returns count of dependents")
        void returnsCount() {
            when(employeeRepository.existsById(EMPLOYEE_ID)).thenReturn(true);
            when(dependentRepository.countByEmployeeId(EMPLOYEE_ID)).thenReturn(3L);

            long count = employeeDependentService.getDependentCount(EMPLOYEE_ID);

            assertEquals(3L, count);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee not found")
        void throwsWhenEmployeeNotFound() {
            when(employeeRepository.existsById(999L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> employeeDependentService.getDependentCount(999L));
        }
    }

    @Nested
    @DisplayName("searchDependents")
    class SearchDependentsTests {

        @Test
        @DisplayName("returns matching dependents")
        void returnsMatchingDependents() {
            when(employeeRepository.existsById(EMPLOYEE_ID)).thenReturn(true);
            when(dependentRepository.searchDependents(EMPLOYEE_ID, "Alice")).thenReturn(List.of(dependent));
            when(dependentMapper.toSummaryDto(dependent)).thenReturn(summaryDto);

            List<EmployeeDependentSummaryDto> result = employeeDependentService.searchDependents(EMPLOYEE_ID, "Alice");

            assertEquals(1, result.size());
            verify(dependentRepository).searchDependents(EMPLOYEE_ID, "Alice");
        }

        @Test
        @DisplayName("returns empty list when no matches")
        void returnsEmptyWhenNoMatches() {
            when(employeeRepository.existsById(EMPLOYEE_ID)).thenReturn(true);
            when(dependentRepository.searchDependents(EMPLOYEE_ID, "NonExistent")).thenReturn(List.of());

            List<EmployeeDependentSummaryDto> result = employeeDependentService.searchDependents(EMPLOYEE_ID, "NonExistent");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee not found")
        void throwsWhenEmployeeNotFound() {
            when(employeeRepository.existsById(999L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> employeeDependentService.searchDependents(999L, "Alice"));
            verify(dependentRepository, never()).searchDependents(any(), any());
        }
    }
}
