package heritage.africca.accel_connect.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobTitleTest {

    private JobTitle jobTitle;

    @BeforeEach
    void setUp() {
        jobTitle = new JobTitle(1L, "Software Developer", null, "001", null);
    }

    @Test
    @DisplayName("All-args constructor and getters")
    void testConstructorAndGetters() {
        assertEquals(1L, jobTitle.getId());
        assertEquals("Software Developer", jobTitle.getJobTitle());
        assertEquals("001", jobTitle.getJobTitleCode());
    }

    @Test
    @DisplayName("Setters update fields correctly")
    void testSetters() {
        jobTitle.setId(2L);
        jobTitle.setJobTitle("Senior Developer");
        jobTitle.setJobTitleCode("002");

        assertEquals(2L, jobTitle.getId());
        assertEquals("Senior Developer", jobTitle.getJobTitle());
        assertEquals("002", jobTitle.getJobTitleCode());
    }

    @Test
    @DisplayName("Equals and hashCode")
    void testEqualsAndHashCode() {
        JobTitle same = new JobTitle(1L, "Software Developer", null, "001", null);
        JobTitle other = new JobTitle(2L, "Project Manager", null, "002", null);

        assertEquals(jobTitle, same);
        assertNotEquals(jobTitle, other);
        assertEquals(jobTitle.hashCode(), same.hashCode());
    }
}
