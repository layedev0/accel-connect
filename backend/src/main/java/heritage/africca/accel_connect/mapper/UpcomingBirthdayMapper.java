package heritage.africca.accel_connect.mapper;

import heritage.africca.accel_connect.dto.UpcomingBirthdayDTO;
import heritage.africca.accel_connect.entity.EmployeeDetails;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring")
public interface UpcomingBirthdayMapper {

    Logger log = LoggerFactory.getLogger(UpcomingBirthdayMapper.class);

    @Mapping(source = "emp.user.id", target = "userId")
    @Mapping(source = "emp.user.firstName", target = "firstName")
    @Mapping(source = "emp.user.lastName", target = "lastName")
    @Mapping(source = "emp.department.name", target = "departmentName")
    @Mapping(source = "emp.dayBirth", target = "day")
    @Mapping(source = "emp.monthBirth", target = "month")
    @Mapping(target = "nextBirthday", ignore = true)
    @Mapping(target = "daysUntil", ignore = true)
    @Mapping(target = "today", ignore = true)
    @Mapping(target = "turningAge", ignore = true)
    UpcomingBirthdayDTO toDto(EmployeeDetails emp, LocalDate today);


    @AfterMapping
    default void computeSchedule(EmployeeDetails emp, LocalDate today,
                                 @MappingTarget UpcomingBirthdayDTO dto) {
        LocalDate next = nextBirthdayOccurrence(today, emp.getMonthBirth(), emp.getDayBirth());
        if (next == null) {
            return;
        }
        dto.setNextBirthday(next);
        dto.setDaysUntil((int) ChronoUnit.DAYS.between(today, next));
        dto.setToday(next.isEqual(today));
        if (emp.getBirthday() != null) {
            dto.setTurningAge(next.getYear() - emp.getBirthday().getYear());
        }
    }


    default LocalDate nextBirthdayOccurrence(LocalDate today, Integer month, Integer day) {
        if (month == null || day == null) {
            return null;
        }
        LocalDate candidate = birthdayInYear(today.getYear(), month, day);
        if (candidate == null) {
            return null;
        }
        if (candidate.isBefore(today)) {
            candidate = birthdayInYear(today.getYear() + 1, month, day);
        }
        return candidate;
    }

    default LocalDate birthdayInYear(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            if (month == 2 && day == 29) {
                return LocalDate.of(year, 2, 28);
            }
            log.warn("Ignoring invalid birthday day/month: {}/{}", day, month);
            return null;
        }
    }
}
