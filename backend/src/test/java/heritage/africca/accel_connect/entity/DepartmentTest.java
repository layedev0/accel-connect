package heritage.africca.accel_connect.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentTest {

    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department(1L, "IT", "001", "Information Technology", new ArrayList<>());
    }

    @Test
    @DisplayName("Default constructor via all-args: id, name, code, description")
    void testConstructorAndGetters() {
        assertEquals(1L, department.getId());
        assertEquals("IT", department.getName());
        assertEquals("001", department.getCode());
        assertEquals("Information Technology", department.getDescription());
        assertNotNull(department.getEmployees());
        assertTrue(department.getEmployees().isEmpty());
    }

    @Test
    @DisplayName("Setters update fields correctly")
    void testSetters() {
        department.setId(2L);
        department.setName("HR");
        department.setCode("002");
        department.setDescription("Human Resources");

        assertEquals(2L, department.getId());
        assertEquals("HR", department.getName());
        assertEquals("002", department.getCode());
        assertEquals("Human Resources", department.getDescription());
    }

    @Test
    @DisplayName("Employees list can be set")
    void testEmployeesAssociation() {
        List<EmployeeDetails> employees = new ArrayList<>();
        department.setEmployees(employees);
        assertSame(employees, department.getEmployees());
    }

    @Test
    @DisplayName("Equals and hashCode include id and fields")
    void testEqualsAndHashCode() {
        List<EmployeeDetails> sharedList = new ArrayList<>();
        Department same = new Department(1L, "IT", "001", "Information Technology", sharedList);
        department.setEmployees(sharedList);
        Department other = new Department(2L, "HR", "002", "Human Resources", new ArrayList<>());

        assertEquals(department, same);
        assertNotEquals(department, other);
        assertEquals(department.hashCode(), same.hashCode());
    }
}
