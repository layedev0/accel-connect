package heritage.africca.accel_connect.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobCategoryTest {

    private JobCategory jobCategory;

    @BeforeEach
    void setUp() {
        jobCategory = new JobCategory();
        jobCategory.setId(1L);
        jobCategory.setName("Engineering");
        jobCategory.setDescription("Software and IT roles");
        jobCategory.setCode("ENG");
        jobCategory.setEmployees(new ArrayList<>());
    }

    @Test
    @DisplayName("Default constructor and setters")
    void testDefaultConstructorAndSetters() {
        JobCategory category = new JobCategory();
        assertNull(category.getId());
        assertNull(category.getName());
        assertNull(category.getDescription());
        assertNull(category.getCode());
        assertNull(category.getEmployees());
    }

    @Test
    @DisplayName("Getters return set values")
    void testGetters() {
        assertEquals(1L, jobCategory.getId());
        assertEquals("Engineering", jobCategory.getName());
        assertEquals("Software and IT roles", jobCategory.getDescription());
        assertEquals("ENG", jobCategory.getCode());
        assertNotNull(jobCategory.getEmployees());
        assertTrue(jobCategory.getEmployees().isEmpty());
    }

    @Test
    @DisplayName("Employees list can be set")
    void testEmployeesAssociation() {
        List<EmployeeDetails> employees = new ArrayList<>();
        jobCategory.setEmployees(employees);
        assertSame(employees, jobCategory.getEmployees());
    }

    @Test
    @DisplayName("Equals and hashCode")
    void testEqualsAndHashCode() {
        JobCategory same = new JobCategory();
        same.setId(1L);
        same.setName("Engineering");
        same.setDescription("Software and IT roles");
        same.setCode("ENG");
        same.setEmployees(jobCategory.getEmployees());

        JobCategory other = new JobCategory();
        other.setId(2L);
        other.setName("HR");

        assertEquals(jobCategory, same);
        assertNotEquals(jobCategory, other);
        assertEquals(jobCategory.hashCode(), same.hashCode());
    }
}
