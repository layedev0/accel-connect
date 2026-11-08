package heritage.africca.accel_connect.outils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeSkillIdTest {

    @Test
    @DisplayName("No-args constructor creates instance")
    void testNoArgsConstructor() {
        EmployeeSkillId id = new EmployeeSkillId();
        assertNotNull(id);
    }

    @Test
    @DisplayName("All-args constructor creates instance with given values")
    void testAllArgsConstructor() {
        EmployeeSkillId id = new EmployeeSkillId(1001L, 5);
        assertNotNull(id);
        assertEquals(id, new EmployeeSkillId(1001L, 5));
    }

    @Test
    @DisplayName("Equals returns true for same empNumber and skillId")
    void testEqualsSameValues() {
        EmployeeSkillId id1 = new EmployeeSkillId(1001L, 5);
        EmployeeSkillId id2 = new EmployeeSkillId(1001L, 5);
        assertEquals(id1, id2);
    }

    @Test
    @DisplayName("Equals returns false for different empNumber")
    void testEqualsDifferentEmpNumber() {
        EmployeeSkillId id1 = new EmployeeSkillId(1001L, 5);
        EmployeeSkillId id2 = new EmployeeSkillId(1002L, 5);
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("Equals returns false for different skillId")
    void testEqualsDifferentSkillId() {
        EmployeeSkillId id1 = new EmployeeSkillId(1001L, 5);
        EmployeeSkillId id2 = new EmployeeSkillId(1001L, 6);
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("Equals returns true for same reference")
    void testEqualsSameReference() {
        EmployeeSkillId id = new EmployeeSkillId(1001L, 5);
        assertTrue(id.equals(id));
    }

    @Test
    @DisplayName("Equals returns false for null")
    void testEqualsNull() {
        EmployeeSkillId id = new EmployeeSkillId(1001L, 5);
        assertFalse(id.equals(null));
    }

    @Test
    @DisplayName("Equals returns false for different type")
    void testEqualsDifferentType() {
        EmployeeSkillId id = new EmployeeSkillId(1001L, 5);
        assertFalse(id.equals("1001-5"));
        assertFalse(id.equals(1001));
    }

    @Test
    @DisplayName("HashCode is consistent with equals")
    void testHashCodeConsistentWithEquals() {
        EmployeeSkillId id1 = new EmployeeSkillId(1001L, 5);
        EmployeeSkillId id2 = new EmployeeSkillId(1001L, 5);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("HashCode is stable for same instance")
    void testHashCodeStable() {
        EmployeeSkillId id = new EmployeeSkillId(1001L, 5);
        assertEquals(id.hashCode(), id.hashCode());
    }

    @Test
    @DisplayName("Implements Serializable")
    void testImplementsSerializable() {
        assertTrue(new EmployeeSkillId(1001L, 5) instanceof java.io.Serializable);
    }
}
