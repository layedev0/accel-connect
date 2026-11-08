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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeDependentService {

    private final EmployeeDependentRepository dependentRepository;
    private final EmployeeDetailsRepository employeeRepository;
    private final EmployeeDependentMapper dependentMapper;

    // ========== Basic CRUD Operations ==========

    @Transactional
    @CacheEvict(value = {"employeeDependents", "employee"}, key = "#employeeId")
    public EmployeeDependentDto createDependent(Long employeeId, EmployeeDependentDto dependentDto) {
        log.info("Creating dependent for employee ID: {}", employeeId);

        // Check if employee exists
        EmployeeDetails employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        // Check for duplicate name
        if (dependentRepository.existsByEmployeeIdAndNameIgnoreCase(employeeId, dependentDto.getName())) {
            throw new DuplicateResourceException("Dependent with name '" + dependentDto.getName() + "' already exists for this employee");
        }

        // Validate contact number if provided
        if (dependentDto.getContactNumber() != null && !dependentDto.getContactNumber().trim().isEmpty()) {
            validateContactNumber(dependentDto.getContactNumber());
        }

        // Generate next sequence number
        Integer nextSeqNo = dependentRepository.findMaxSeqNoByEmployeeId(employeeId)
                .map(max -> max + 1)
                .orElse(1);

        // Create dependent
        EmployeeDependent dependent = dependentMapper.toEntity(dependentDto);
        dependent.setEmployee(employee);
        dependent.setSeqNo(nextSeqNo);

        EmployeeDependent savedDependent = dependentRepository.save(dependent);
        log.info("Dependent created successfully with sequence number: {}", nextSeqNo);

        return dependentMapper.toDto(savedDependent);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "employeeDependents", key = "#employeeId + '-' + #seqNo"),
            @CacheEvict(value = "employeeDependents", key = "#employeeId"),
            @CacheEvict(value = "employee", key = "#employeeId")
    })
    public EmployeeDependentDto updateDependent(Long employeeId, Integer seqNo, EmployeeDependentDto dependentDto) {
        log.info("Updating dependent for employee ID: {}, sequence: {}", employeeId, seqNo);

        EmployeeDependent dependent = dependentRepository.findByEmployeeIdAndSeqNo(employeeId, seqNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dependent not found with sequence number: " + seqNo + " for employee ID: " + employeeId));

        // Check for duplicate name if name changed
        if (!dependent.getName().equalsIgnoreCase(dependentDto.getName()) &&
                dependentRepository.existsByEmployeeIdAndNameIgnoreCase(employeeId, dependentDto.getName())) {
            throw new DuplicateResourceException("Dependent with name '" + dependentDto.getName() + "' already exists for this employee");
        }

        // Validate contact number if provided
        if (dependentDto.getContactNumber() != null && !dependentDto.getContactNumber().trim().isEmpty()) {
            validateContactNumber(dependentDto.getContactNumber());
        }

        // Update fields
        dependentMapper.updateEntity(dependentDto, dependent);

        EmployeeDependent updatedDependent = dependentRepository.save(dependent);
        log.info("Dependent updated successfully");

        return dependentMapper.toDto(updatedDependent);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "employeeDependents", key = "#employeeId + '-' + #seqNo"),
            @CacheEvict(value = "employeeDependents", key = "#employeeId"),
            @CacheEvict(value = "employee", key = "#employeeId")
    })
    public void deleteDependent(Long employeeId, Integer seqNo) {
        log.info("Deleting dependent for employee ID: {}, sequence: {}", employeeId, seqNo);

        if (!dependentRepository.existsById(new EmployeeDependentId(employeeId, seqNo))) {
            throw new ResourceNotFoundException(
                    "Dependent not found with sequence number: " + seqNo + " for employee ID: " + employeeId);
        }

        dependentRepository.deleteByEmployeeIdAndSeqNo(employeeId, seqNo);
        log.info("Dependent deleted successfully");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "employeeDependents", key = "#employeeId"),
            @CacheEvict(value = "employee", key = "#employeeId")
    })
    public void deleteAllDependents(Long employeeId) {
        log.info("Deleting all dependents for employee ID: {}", employeeId);

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }

        dependentRepository.deleteAllByEmployeeId(employeeId);
        log.info("All dependents deleted successfully for employee ID: {}", employeeId);
    }

    @Cacheable(value = "employeeDependents", key = "#employeeId + '-' + #seqNo")
    public EmployeeDependentDto getDependent(Long employeeId, Integer seqNo) {
        log.debug("Fetching dependent for employee ID: {}, sequence: {}", employeeId, seqNo);

        EmployeeDependent dependent = dependentRepository.findByEmployeeIdAndSeqNo(employeeId, seqNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dependent not found with sequence number: " + seqNo + " for employee ID: " + employeeId));

        return dependentMapper.toDto(dependent);
    }

    public List<EmployeeDependentSummaryDto> getAllDependents(Long employeeId) {
        log.debug("Fetching all dependents for employee ID: {}", employeeId);

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }

        return dependentRepository.findAllByEmployeeIdOrderBySeqNo(employeeId).stream()
                .map(this::enrichWithAge)
                .collect(Collectors.toList());
    }

    // ========== Statistics Methods ==========

    public long getDependentCount(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }
        return dependentRepository.countByEmployeeId(employeeId);
    }

    // ========== Helper Methods ==========

    private void validateContactNumber(String contactNumber) {
        if (!contactNumber.matches("^[0-9+\\-()\\s]*$")) {
            throw new ValidationException("Invalid contact number format. Allowed characters: numbers, +, -, (), and spaces");
        }
    }

    private EmployeeDependentSummaryDto enrichWithAge(EmployeeDependent dependent) {
        EmployeeDependentSummaryDto summary = dependentMapper.toSummaryDto(dependent);

        if (dependent.getDateOfBirth() != null) {
            int age = Period.between(dependent.getDateOfBirth(), LocalDate.now()).getYears();
            summary.setAge(age);
        }

        return summary;
    }

    // ========== Search Methods ==========

    public List<EmployeeDependentSummaryDto> searchDependents(Long employeeId, String searchTerm) {
        log.debug("Searching dependents for employee ID: {}, term: {}", employeeId, searchTerm);

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }

        return dependentRepository.searchDependents(employeeId, searchTerm).stream()
                .map(this::enrichWithAge)
                .collect(Collectors.toList());
    }
}