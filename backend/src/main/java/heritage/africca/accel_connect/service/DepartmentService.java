package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.DepartmentRequest;
import heritage.africca.accel_connect.entity.Department;
import heritage.africca.accel_connect.exception.DuplicateResourceException;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class DepartmentService {

    private static final Pattern CODE_FORMAT = Pattern.compile("^[0-9]{3}$");

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    @Transactional
    public Department createDepartment(DepartmentRequest request) {
        String code = resolveCode(request.getCode(), null);
        validateCodeFormat(code);
        validateCodeUniqueness(code, null);
        validateNameUniqueness(request.getName().trim(), null);

        Department entity = new Department();
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        entity.setCode(code);

        Department saved = departmentRepository.save(entity);
        log.info("Department created with code: {}", saved.getCode());
        return saved;
    }

    @Transactional
    public Department updateDepartment(Long id, DepartmentRequest request) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));

        String code = resolveCode(request.getCode(), existing.getCode());
        validateCodeFormat(code);
        validateCodeUniqueness(code, id);
        validateNameUniqueness(request.getName().trim(), id);

        existing.setName(request.getName().trim());
        existing.setDescription(request.getDescription() != null
                ? request.getDescription().trim() : null);
        existing.setCode(code);

        return departmentRepository.save(existing);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
        if (departmentRepository.hasEmployees(id)) {
            throw new IllegalStateException("Cannot delete department that has employees");
        }
        departmentRepository.delete(existing);
        log.info("Department deleted with id: {}", id);
    }

    private String resolveCode(String providedCode, String existingCode) {
        if (providedCode == null || providedCode.isBlank()) {
            if (existingCode != null) {
                return existingCode;
            }
            throw new IllegalArgumentException("Code must not be blank");
        }
        String trimmed = providedCode.trim();
        try {
            int num = Integer.parseInt(trimmed);
            if (num < 1 || num > 999) {
                throw new IllegalArgumentException("Code must be between 001 and 999");
            }
            return String.format("%03d", num);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Code must be exactly 3 digits (e.g. 001, 002, 999)");
        }
    }

    private void validateCodeFormat(String code) {
        if (code == null || !CODE_FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException("Code must be exactly 3 digits (e.g. 001, 002, 999)");
        }
    }

    private void validateCodeUniqueness(String code, Long excludeId) {
        boolean exists = excludeId == null
                ? departmentRepository.existsByCode(code)
                : departmentRepository.existsByCodeAndIdNot(code, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Department code already exists: " + code);
        }
    }

    private void validateNameUniqueness(String name, Long excludeId) {
        boolean exists = excludeId == null
                ? departmentRepository.existsByName(name)
                : departmentRepository.existsByNameAndIdNot(name, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Department name already exists: " + name);
        }
    }
}
