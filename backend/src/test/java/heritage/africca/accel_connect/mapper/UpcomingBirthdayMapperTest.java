package heritage.africca.accel_connect.mapper;

import heritage.africca.accel_connect.dto.UpcomingBirthdayDTO;
import heritage.africca.accel_connect.entity.Department;
import heritage.africca.accel_connect.entity.EmployeeDetails;
import heritage.africca.accel_connect.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UpcomingBirthdayMapperTest {

    private final UpcomingBirthdayMapper mapper = new UpcomingBirthdayMapperImpl();

    private EmployeeDetails employee(Integer day, Integer month, LocalDate birthday) {
        User user = new User();
        user.setId(10L);
        user.setFirstName("Awa");
        user.setLastName("Diop");

        Department dept = new Department();
        dept.setName("IT");

        EmployeeDetails emp = new EmployeeDetails();
        emp.setUser(user);
        emp.setDepartment(dept);
        emp.setDayBirth(day);
        emp.setMonthBirth(month);
        emp.setBirthday(birthday);
        return emp;
    }

    @Test
    @DisplayName("Maps basic fields from user and department")
    void mapsBasicFields() {
        LocalDate today = LocalDate.of(2026, 7, 7);
        UpcomingBirthdayDTO dto = mapper.toDto(employee(10, 7, LocalDate.of(1990, 7, 10)), today);

        assertEquals(10L, dto.getUserId());
        assertEquals("Awa", dto.getFirstName());
        assertEquals("Diop", dto.getLastName());
        assertEquals("IT", dto.getDepartmentName());
        assertEquals(10, dto.getDay());
        assertEquals(7, dto.getMonth());
    }

    @Test
    @DisplayName("Birthday today resolves to 0 days and today=true")
    void birthdayToday() {
        LocalDate today = LocalDate.of(2026, 7, 7);
        UpcomingBirthdayDTO dto = mapper.toDto(employee(7, 7, LocalDate.of(1990, 7, 7)), today);

        assertEquals(LocalDate.of(2026, 7, 7), dto.getNextBirthday());
        assertEquals(0, dto.getDaysUntil());
        assertTrue(dto.isToday());
        assertEquals(36, dto.getTurningAge());
    }

    @Test
    @DisplayName("Upcoming birthday counts days from today")
    void upcomingBirthday() {
        LocalDate today = LocalDate.of(2026, 7, 7);
        UpcomingBirthdayDTO dto = mapper.toDto(employee(10, 7, LocalDate.of(2000, 7, 10)), today);

        assertEquals(LocalDate.of(2026, 7, 10), dto.getNextBirthday());
        assertEquals(3, dto.getDaysUntil());
        assertFalse(dto.isToday());
        assertEquals(26, dto.getTurningAge());
    }

    @Test
    @DisplayName("Birthday already passed this year rolls over to next year")
    void wrapsToNextYear() {
        LocalDate today = LocalDate.of(2026, 7, 7);
        UpcomingBirthdayDTO dto = mapper.toDto(employee(1, 7, LocalDate.of(1985, 7, 1)), today);

        assertEquals(LocalDate.of(2027, 7, 1), dto.getNextBirthday());
        assertEquals(42, dto.getTurningAge());
    }

    @Test
    @DisplayName("Feb 29 birthday celebrates on Feb 28 in a non-leap year")
    void feb29InNonLeapYear() {
        LocalDate today = LocalDate.of(2026, 1, 1); // 2026 is not a leap year
        UpcomingBirthdayDTO dto = mapper.toDto(employee(29, 2, LocalDate.of(1992, 2, 29)), today);

        assertEquals(LocalDate.of(2026, 2, 28), dto.getNextBirthday());
    }

    @Test
    @DisplayName("Missing day/month leaves nextBirthday null so the record can be filtered out")
    void missingDayMonth() {
        LocalDate today = LocalDate.of(2026, 7, 7);
        UpcomingBirthdayDTO dto = mapper.toDto(employee(null, null, null), today);

        assertNull(dto.getNextBirthday());
    }

    @Test
    @DisplayName("Null birth year leaves turningAge null but still computes the date")
    void nullBirthYear() {
        LocalDate today = LocalDate.of(2026, 7, 7);
        UpcomingBirthdayDTO dto = mapper.toDto(employee(10, 7, null), today);

        assertEquals(LocalDate.of(2026, 7, 10), dto.getNextBirthday());
        assertNull(dto.getTurningAge());
    }
}
