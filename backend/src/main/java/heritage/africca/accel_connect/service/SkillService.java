package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.SkillRequest;
import heritage.africca.accel_connect.entity.Skill;
import heritage.africca.accel_connect.exception.DuplicateResourceException;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<Skill> findAll() {
        return skillRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Skill getById(Integer id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", id));
    }

    @Transactional
    public Skill create(SkillRequest request) {
        validateNameUniqueness(request.getName().trim(), null);

        Skill entity = new Skill();
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        Skill saved = skillRepository.save(entity);
        log.info("Skill created with id: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Skill update(Integer id, SkillRequest request) {
        Skill existing = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", id));

        validateNameUniqueness(request.getName().trim(), id);

        existing.setName(request.getName().trim());
        existing.setDescription(request.getDescription() != null
                ? request.getDescription().trim() : null);

        return skillRepository.save(existing);
    }

    @Transactional
    public void delete(Integer id) {
        Skill existing = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", id));
        if (skillRepository.hasEmployeeSkills(id)) {
            throw new IllegalStateException("Cannot delete skill that is assigned to employees");
        }
        skillRepository.delete(existing);
        log.info("Skill deleted with id: {}", id);
    }

    private void validateNameUniqueness(String name, Integer excludeId) {
        boolean exists = excludeId == null
                ? skillRepository.existsByName(name)
                : skillRepository.existsByNameAndIdNot(name, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Skill name already exists: " + name);
        }
    }
}
