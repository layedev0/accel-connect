package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.JobTitleRequest;
import heritage.africca.accel_connect.entity.JobTitle;
import heritage.africca.accel_connect.exception.DuplicateResourceException;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.JobTitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobTitleService {

    private static final Pattern CODE_FORMAT = Pattern.compile("^[0-9]{3}$");

    private final JobTitleRepository jobTitleRepository;

    @Transactional(readOnly = true)
    public List<JobTitle> findAll() {
        return jobTitleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public JobTitle getJobTitleById(Long id) {
        return jobTitleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job title", id));
    }

    @Transactional
    public JobTitle createJobTitle(JobTitleRequest request) {
        String code = resolveCode(request.getJobTitleCode(), null);
        validateCodeFormat(code);
        validateCodeUniqueness(code, null);
        validateJobTitleUniqueness(request.getJobTitle().trim(), null);

        JobTitle entity = new JobTitle();
        entity.setJobTitle(request.getJobTitle().trim());
        entity.setJobDescription(request.getJobDescription() != null ? request.getJobDescription().trim() : null);
        entity.setJobTitleCode(code);

        JobTitle saved = jobTitleRepository.save(entity);
        log.info("Job title created with code: {}", saved.getJobTitleCode());
        return saved;
    }

    @Transactional
    public JobTitle updateJobTitle(Long id, JobTitleRequest request) {
        JobTitle existing = jobTitleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job title", id));

        String code = resolveCode(request.getJobTitleCode(), existing.getJobTitleCode());
        validateCodeFormat(code);
        validateCodeUniqueness(code, id);
        validateJobTitleUniqueness(request.getJobTitle().trim(), id);

        existing.setJobTitle(request.getJobTitle().trim());
        existing.setJobDescription(request.getJobDescription() != null
                ? request.getJobDescription().trim() : null);
        existing.setJobTitleCode(code);

        return jobTitleRepository.save(existing);
    }

    @Transactional
    public void deleteJobTitle(Long id) {
        JobTitle existing = jobTitleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job title", id));
        if (jobTitleRepository.hasEmployees(id)) {
            throw new IllegalStateException("Cannot delete job title that has employees");
        }
        jobTitleRepository.delete(existing);
        log.info("Job title deleted with id: {}", id);
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
                ? jobTitleRepository.existsByJobTitleCode(code)
                : jobTitleRepository.existsByJobTitleCodeAndIdNot(code, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Job title code already exists: " + code);
        }
    }

    private void validateJobTitleUniqueness(String jobTitle, Long excludeId) {
        boolean exists = excludeId == null
                ? jobTitleRepository.existsByJobTitle(jobTitle)
                : jobTitleRepository.existsByJobTitleAndIdNot(jobTitle, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Job title already exists: " + jobTitle);
        }
    }
}
