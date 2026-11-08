package heritage.africca.accel_connect.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillTest {

    private Skill skill;

    @BeforeEach
    void setUp() {
        skill = new Skill(1, "Java", "Java programming language");
    }

    @Test
    @DisplayName("No-args constructor creates empty skill")
    void testNoArgsConstructor() {
        Skill empty = new Skill();
        assertNull(empty.getId());
        assertNull(empty.getName());
        assertNull(empty.getDescription());
    }

    @Test
    @DisplayName("All-args constructor and getters")
    void testConstructorAndGetters() {
        assertEquals(1, skill.getId());
        assertEquals("Java", skill.getName());
        assertEquals("Java programming language", skill.getDescription());
    }

    @Test
    @DisplayName("Setters update fields correctly")
    void testSetters() {
        skill.setId(2);
        skill.setName("Python");
        skill.setDescription("Python programming");

        assertEquals(2, skill.getId());
        assertEquals("Python", skill.getName());
        assertEquals("Python programming", skill.getDescription());
    }

    @Test
    @DisplayName("Equals and hashCode")
    void testEqualsAndHashCode() {
        Skill same = new Skill(1, "Java", "Java programming language");
        Skill other = new Skill(2, "Python", null);

        assertEquals(skill, same);
        assertNotEquals(skill, other);
        assertEquals(skill.hashCode(), same.hashCode());
    }
}
