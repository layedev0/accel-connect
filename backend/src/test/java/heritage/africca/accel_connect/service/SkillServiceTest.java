package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.SkillRequest;
import heritage.africca.accel_connect.entity.Skill;
import heritage.africca.accel_connect.exception.DuplicateResourceException;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.SkillRepository;
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
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillService skillService;

    private Skill javaSkill;
    private Skill pythonSkill;

    @BeforeEach
    void setUp() {
        javaSkill = new Skill(1, "Java", "Java programming language");
        pythonSkill = new Skill(2, "Python", "Python programming");
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("returns all skills")
        void returnsAllSkills() {
            when(skillRepository.findAll()).thenReturn(List.of(javaSkill, pythonSkill));

            List<Skill> result = skillService.findAll();

            assertEquals(2, result.size());
            assertTrue(result.contains(javaSkill));
            assertTrue(result.contains(pythonSkill));
            verify(skillRepository).findAll();
        }

        @Test
        @DisplayName("returns empty list when no skills exist")
        void returnsEmptyListWhenNoSkills() {
            when(skillRepository.findAll()).thenReturn(List.of());

            List<Skill> result = skillService.findAll();

            assertTrue(result.isEmpty());
            verify(skillRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("returns skill when found")
        void returnsSkillWhenFound() {
            when(skillRepository.findById(1)).thenReturn(Optional.of(javaSkill));

            Skill result = skillService.getById(1);

            assertNotNull(result);
            assertEquals(1, result.getId());
            assertEquals("Java", result.getName());
            assertEquals("Java programming language", result.getDescription());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            when(skillRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> skillService.getById(999));
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("creates skill with name and description")
        void createsWithNameAndDescription() {
            SkillRequest input = new SkillRequest("Java", "Java programming language");
            when(skillRepository.existsByName("Java")).thenReturn(false);
            when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> {
                Skill s = inv.getArgument(0);
                s.setId(1);
                return s;
            });

            Skill result = skillService.create(input);

            assertNotNull(result.getId());
            assertEquals("Java", result.getName());
            assertEquals("Java programming language", result.getDescription());
            verify(skillRepository).existsByName("Java");
            verify(skillRepository).save(any(Skill.class));
        }

        @Test
        @DisplayName("trims name and description")
        void trimsNameAndDescription() {
            SkillRequest input = new SkillRequest("  Python  ", "  Python programming  ");
            when(skillRepository.existsByName("Python")).thenReturn(false);
            when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

            Skill result = skillService.create(input);

            assertEquals("Python", result.getName());
            assertEquals("Python programming", result.getDescription());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when name already exists")
        void throwsWhenNameDuplicate() {
            SkillRequest input = new SkillRequest("Java", null);
            when(skillRepository.existsByName("Java")).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> skillService.create(input));
            verify(skillRepository, never()).save(any());
        }

        @Test
        @DisplayName("accepts null description")
        void acceptsNullDescription() {
            SkillRequest input = new SkillRequest("Rust", null);
            when(skillRepository.existsByName("Rust")).thenReturn(false);
            when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

            Skill result = skillService.create(input);

            assertNull(result.getDescription());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("updates skill when found")
        void updatesWhenFound() {
            SkillRequest input = new SkillRequest("Java Updated", "Updated description");
            when(skillRepository.findById(1)).thenReturn(Optional.of(javaSkill));
            when(skillRepository.existsByNameAndIdNot("Java Updated", 1)).thenReturn(false);
            when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

            Skill result = skillService.update(1, input);

            assertNotNull(result);
            assertEquals("Java Updated", result.getName());
            assertEquals("Updated description", result.getDescription());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id not found")
        void throwsWhenNotFound() {
            SkillRequest input = new SkillRequest("Java", null);
            when(skillRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> skillService.update(999, input));
            verify(skillRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when updated name already exists")
        void throwsWhenNameDuplicate() {
            SkillRequest input = new SkillRequest("Python", null);
            when(skillRepository.findById(1)).thenReturn(Optional.of(javaSkill));
            when(skillRepository.existsByNameAndIdNot("Python", 1)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> skillService.update(1, input));
            verify(skillRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows same name when updating same skill")
        void allowsSameNameWhenSameSkill() {
            SkillRequest input = new SkillRequest("Java", "Updated desc");
            when(skillRepository.findById(1)).thenReturn(Optional.of(javaSkill));
            when(skillRepository.existsByNameAndIdNot("Java", 1)).thenReturn(false);
            when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

            Skill result = skillService.update(1, input);

            assertEquals("Java", result.getName());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deletes skill when not assigned to employees")
        void deletesWhenNoEmployeeSkills() {
            when(skillRepository.findById(1)).thenReturn(Optional.of(javaSkill));
            when(skillRepository.hasEmployeeSkills(1)).thenReturn(false);

            skillService.delete(1);

            verify(skillRepository).findById(1);
            verify(skillRepository).hasEmployeeSkills(1);
            verify(skillRepository).delete(javaSkill);
        }

        @Test
        @DisplayName("throws IllegalStateException when skill is assigned to employees")
        void throwsWhenHasEmployeeSkills() {
            when(skillRepository.findById(1)).thenReturn(Optional.of(javaSkill));
            when(skillRepository.hasEmployeeSkills(1)).thenReturn(true);

            assertThrows(IllegalStateException.class, () -> skillService.delete(1));

            verify(skillRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id not found")
        void throwsWhenIdNotFound() {
            when(skillRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> skillService.delete(999));

            verify(skillRepository, never()).hasEmployeeSkills(any());
            verify(skillRepository, never()).delete(any());
        }
    }
}
