package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.HolidayDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface SenegalHolidayService {
    Set<LocalDate> getHolidaysForYear(int year);
    Set<LocalDate> getHolidaysForRange(LocalDate start, LocalDate end);

    List<HolidayDto> getNamedHolidaysForRange(LocalDate start, LocalDate end);
}
