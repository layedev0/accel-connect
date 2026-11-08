package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.EmployeeSkillDto;
import heritage.africca.accel_connect.entity.EmployeeDetails;
import heritage.africca.accel_connect.entity.EmployeeSkill;
import heritage.africca.accel_connect.entity.Skill;
import heritage.africca.accel_connect.exception.DuplicateResourceException;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.outils.EmployeeSkillId;
import heritage.africca.accel_connect.repository.EmployeeDetailsRepository;
import heritage.africca.accel_connect.repository.EmployeeSkillRepository;
import heritage.africca.accel_connect.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeSkillService {

    private final EmployeeSkillRepository employeeSkillRepository;
    private final EmployeeDetailsRepository employeeDetailsRepository;
    private final SkillRepository skillRepository;

    // ========== Create ==========

    @Transactional
    public EmployeeSkillDto addSkillToEmployee(Long employeeId, EmployeeSkillDto dto) {
        log.info("Adding skill {} to employee {}", dto.getSkillId(), employeeId);

        EmployeeDetails employee = employeeDetailsRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        Skill skill = skillRepository.findById(dto.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill", dto.getSkillId()));

        if (employeeSkillRepository.existsByEmployee_IdAndSkillId(employeeId, dto.getSkillId())) {
            throw new DuplicateResourceException("Skill already assigned to this employee");
        }

        EmployeeSkill entity = new EmployeeSkill();
        entity.setEmployee(employee);
        entity.setSkillId(dto.getSkillId());
        entity.setYearsOfExperience(dto.getYearsOfExperience());
        entity.setComments(dto.getComments());

        EmployeeSkill saved = employeeSkillRepository.save(entity);
        log.info("Skill {} assigned to employee {}", dto.getSkillId(), employeeId);

        return toDto(saved, skill);
    }

    // ========== Read ==========

    @Transactional(readOnly = true)
    public List<EmployeeSkillDto> getSkillsForEmployee(Long employeeId) {
        log.info("Fetching skills for employee {}", employeeId);

        if (!employeeDetailsRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }

        List<EmployeeSkill> skills = employeeSkillRepository.findByEmployee_Id(employeeId);

        return skills.stream()
                .map(es -> {
                    Skill skill = skillRepository.findById(es.getSkillId())
                            .orElse(null);
                    return toDto(es, skill);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeSkillDto getEmployeeSkill(Long employeeId, Integer skillId) {
        log.info("Fetching skill {} for employee {}", skillId, employeeId);

        EmployeeSkill skill = employeeSkillRepository.findByEmployee_IdAndSkillId(employeeId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Skill with id " + skillId + " not found for employee " + employeeId));

        Skill skillEntity = skillRepository.findById(skillId).orElse(null);
        return toDto(skill, skillEntity);
    }

    // ========== Update ==========

    @Transactional
    public EmployeeSkillDto updateEmployeeSkill(Long employeeId, Integer skillId, EmployeeSkillDto dto) {
        log.info("Updating skill {} for employee {}", skillId, employeeId);

        EmployeeSkill existing = employeeSkillRepository.findByEmployee_IdAndSkillId(employeeId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Skill with id " + skillId + " not found for employee " + employeeId));

        existing.setYearsOfExperience(dto.getYearsOfExperience());
        existing.setComments(dto.getComments());

        EmployeeSkill saved = employeeSkillRepository.save(existing);
        Skill skill = skillRepository.findById(skillId).orElse(null);
        return toDto(saved, skill);
    }

    // ========== Delete ==========

    @Transactional
    public void removeEmployeeSkill(Long employeeId, Integer skillId) {
        log.info("Removing skill {} from employee {}", skillId, employeeId);

        if (!employeeSkillRepository.existsById(new EmployeeSkillId(employeeId, skillId))) {
            throw new ResourceNotFoundException(
                    "Skill with id " + skillId + " not found for employee " + employeeId);
        }

        employeeSkillRepository.deleteByEmployee_IdAndSkillId(employeeId, skillId);
        log.info("Skill {} removed from employee {}", skillId, employeeId);
    }

    // ========== Mapping ==========

    private EmployeeSkillDto toDto(EmployeeSkill entity, Skill skill) {
        EmployeeSkillDto dto = new EmployeeSkillDto();
        dto.setEmployeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null);
        dto.setSkillId(entity.getSkillId());
        dto.setYearsOfExperience(entity.getYearsOfExperience());
        dto.setComments(entity.getComments());
        if (skill != null) {
            dto.setSkillName(skill.getName());
        }
        return dto;
    }
}

