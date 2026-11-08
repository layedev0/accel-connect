package heritage.africca.accel_connect.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeDetailsTest {

    private EmployeeDetails employeeDetails;
    private Department department;
    private JobTitle jobTitle;
    private JobCategory jobCategory;

    @BeforeEach
    void setUp() {
        department = new Department(1L, "IT", "001", "Information Technology", null);
        jobTitle = new JobTitle(1L, "Developer", null, "001", null);
        jobCategory = new JobCategory();
        jobCategory.setId(1L);
        jobCategory.setName("Engineering");

        employeeDetails = new EmployeeDetails(
                1L,
                LocalDate.of(1990, 5, 15),
                "M",
                "SINGLE",
                "CNI123",
                jobTitle,
                jobCategory,
                department,
                "+221771234567",
                LocalDate.of(2020, 1, 10),
                "ACTIVE",
                15,
                5,
                null
        );
    }

    @Test
    @DisplayName("Constructor and getters for core fields")
    void testConstructorAndGetters() {
        assertEquals(1L, employeeDetails.getId());
        assertEquals(LocalDate.of(1990, 5, 15), employeeDetails.getBirthday());
        assertEquals("M", employeeDetails.getSexe());
        assertEquals("SINGLE", employeeDetails.getMaritalStatus());
        assertEquals("CNI123", employeeDetails.getCni());
        assertEquals("+221771234567", employeeDetails.getWorkTelephone());
        assertEquals(LocalDate.of(2020, 1, 10), employeeDetails.getJoinedDate());
        assertEquals("ACTIVE", employeeDetails.getTypeContrat());
    }

    @Test
    @DisplayName("Associations are set correctly")
    void testAssociations() {
        assertSame(jobTitle, employeeDetails.getJobTitle());
        assertSame(jobCategory, employeeDetails.getJobCategory());
        assertSame(department, employeeDetails.getDepartment());
        assertNull(employeeDetails.getUser());
    }

    @Test
    @DisplayName("Setters update fields correctly")
    void testSetters() {
        employeeDetails.setSexe("F");
        employeeDetails.setMaritalStatus("MARRIED");
        employeeDetails.setTypeContrat("INACTIVE");

        assertEquals("F", employeeDetails.getSexe());
        assertEquals("MARRIED", employeeDetails.getMaritalStatus());
        assertEquals("INACTIVE", employeeDetails.getTypeContrat());
    }

    @Test
    @DisplayName("workTelephone accepts valid local format +2217XXXXXXXX")
    void testWorkTelephoneValidLocalFormat() {
        employeeDetails.setWorkTelephone("+221761234567");
        assertEquals("+221761234567", employeeDetails.getWorkTelephone());

        employeeDetails.setWorkTelephone("+221770000000");
        assertEquals("+221770000000", employeeDetails.getWorkTelephone());
    }

    @Test
    @DisplayName("Equals and hashCode")
    void testEqualsAndHashCode() {
        EmployeeDetails same = new EmployeeDetails(
                1L, LocalDate.of(1990, 5, 15), "M", "SINGLE",
                "CNI123", jobTitle, jobCategory, department,
                "+221771234567", LocalDate.of(2020, 1, 10), "ACTIVE", 15, 5, null
        );
        EmployeeDetails other = new EmployeeDetails(
                2L, null, "F", "MARRIED",
                null, null, null, null, null, null, null, null, null, null
        );

        assertEquals(employeeDetails, same);
        assertNotEquals(employeeDetails, other);
        assertEquals(employeeDetails.hashCode(), same.hashCode());
    }
}
