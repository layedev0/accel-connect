package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.JobCategoryRequest;
import heritage.africca.accel_connect.entity.JobCategory;
import heritage.africca.accel_connect.exception.DuplicateResourceException;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.JobCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobCategoryService {

    private static final Pattern CODE_FORMAT = Pattern.compile("^[0-9]{3}$");

    private final JobCategoryRepository jobCategoryRepository;

    @Transactional(readOnly = true)
    public List<JobCategory> findAll() {
        return jobCategoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public JobCategory getJobCategoryById(Long id) {
        return jobCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job category", id));
    }

    @Transactional
    public JobCategory createJobCategory(JobCategoryRequest request) {
        String code = resolveCode(request.getCode(), null);
        validateCodeFormat(code);
        validateCodeUniqueness(code, null);
        validateNameUniqueness(request.getName().trim(), null);

        validateCategoryCodeUniqueness(request.getCategoryCode().trim(), null);

        JobCategory entity = new JobCategory();
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        entity.setCode(code);
        entity.setCategoryCode(request.getCategoryCode().trim());
        entity.setSalaireBase(request.getSalaireBase());

        JobCategory saved = jobCategoryRepository.save(entity);
        log.info("Job category created with code: {}", saved.getCode());
        return saved;
    }

    @Transactional
    public JobCategory updateJobCategory(Long id, JobCategoryRequest request) {
        JobCategory existing = jobCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job category", id));

        String code = resolveCode(request.getCode(), existing.getCode());
        validateCodeFormat(code);
        validateCodeUniqueness(code, id);
        validateNameUniqueness(request.getName().trim(), id);

        validateCategoryCodeUniqueness(request.getCategoryCode().trim(), id);

        existing.setName(request.getName().trim());
        existing.setDescription(request.getDescription() != null
                ? request.getDescription().trim() : null);
        existing.setCode(code);
        existing.setCategoryCode(request.getCategoryCode().trim());
        existing.setSalaireBase(request.getSalaireBase());

        return jobCategoryRepository.save(existing);
    }

    @Transactional
    public void deleteJobCategory(Long id) {
        JobCategory existing = jobCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job category", id));
        if (jobCategoryRepository.hasEmployees(id)) {
            throw new IllegalStateException("Cannot delete job category that has employees");
        }
        jobCategoryRepository.delete(existing);
        log.info("Job category deleted with id: {}", id);
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
                ? jobCategoryRepository.existsByCode(code)
                : jobCategoryRepository.existsByCodeAndIdNot(code, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Job category code already exists: " + code);
        }
    }

    private void validateNameUniqueness(String name, Long excludeId) {
        boolean exists = excludeId == null
                ? jobCategoryRepository.existsByName(name)
                : jobCategoryRepository.existsByNameAndIdNot(name, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Job category name already exists: " + name);
        }
    }

    private void validateCategoryCodeUniqueness(String categoryCode, Long excludeId) {
        boolean exists = excludeId == null
                ? jobCategoryRepository.existsByCategoryCode(categoryCode)
                : jobCategoryRepository.existsByCategoryCodeAndIdNot(categoryCode, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Job category code already exists: " + categoryCode);
        }
    }
}
