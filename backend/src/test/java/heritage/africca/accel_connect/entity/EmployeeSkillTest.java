package heritage.africca.accel_connect.entity;

import heritage.africca.accel_connect.outils.EmployeeSkillId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeSkillTest {

    @Test
    @DisplayName("Default constructor creates instance")
    void testDefaultConstructor() {
        EmployeeSkill employeeSkill = new EmployeeSkill();
        assertNotNull(employeeSkill);
    }

    @Test
    @DisplayName("EmployeeSkillId equals and hashCode")
    void testEmployeeSkillIdEqualsAndHashCode() {
        EmployeeSkillId id1 = new EmployeeSkillId(1001L, 5);
        EmployeeSkillId id2 = new EmployeeSkillId(1001L, 5);
        EmployeeSkillId id3 = new EmployeeSkillId(1002L, 5);

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("EmployeeSkillId no-args constructor")
    void testEmployeeSkillIdNoArgs() {
        assertNotNull(new EmployeeSkillId());
    }

    @Test
    @DisplayName("EmployeeSkillId equals with null and same reference")
    void testEmployeeSkillIdEqualsEdgeCases() {
        EmployeeSkillId id = new EmployeeSkillId(1001L, 5);
        assertTrue(id.equals(id));
        assertFalse(id.equals(null));
        assertFalse(id.equals("not an id"));
    }
}
