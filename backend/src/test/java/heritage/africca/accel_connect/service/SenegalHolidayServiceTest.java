package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.config.SenegalHolidayProperties;
import heritage.africca.accel_connect.dto.HolidayDto;
import heritage.africca.accel_connect.service.Impl.SenegalHolidayServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SenegalHolidayServiceTest {

    private SenegalHolidayServiceImpl service() {
        SenegalHolidayProperties props = new SenegalHolidayProperties();
        props.getIslamic().put("2026", "2026-05-27"); // e.g. Tabaski
        return new SenegalHolidayServiceImpl(props);
    }

    @Test
    void returnsNamedHolidaysWithinRangeSortedByDate() {
        List<HolidayDto> holidays =
                service().getNamedHolidaysForRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        // May 2026: Fête du Travail (01), Ascension (14 = Easter 05-04 + 39),
        // Islamic date (27), Lundi de Pentecôte (25 = Easter + 50)
        assertEquals(4, holidays.size());

        // sorted ascending by date
        for (int i = 1; i < holidays.size(); i++) {
            assertFalse(holidays.get(i).getDate().isBefore(holidays.get(i - 1).getDate()));
        }

        assertEquals(LocalDate.of(2026, 5, 1), holidays.get(0).getDate());
        assertEquals("Fête du Travail", holidays.get(0).getName());
        assertTrue(holidays.stream().anyMatch(h -> h.getName().equals("Ascension")
                && h.getDate().equals(LocalDate.of(2026, 5, 14))));
        assertTrue(holidays.stream().anyMatch(h -> h.getName().equals("Lundi de Pentecôte")
                && h.getDate().equals(LocalDate.of(2026, 5, 25))));
        assertTrue(holidays.stream().anyMatch(h -> h.getDate().equals(LocalDate.of(2026, 5, 27))));
    }

    @Test
    void excludesDatesOutsideTheRange() {
        // Fixed holidays outside May must not leak in
        List<HolidayDto> holidays =
                service().getNamedHolidaysForRange(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 13));

        assertTrue(holidays.stream().noneMatch(h -> h.getName().equals("Fête du Travail")));
        assertTrue(holidays.stream().allMatch(h ->
                !h.getDate().isBefore(LocalDate.of(2026, 5, 2))
                        && !h.getDate().isAfter(LocalDate.of(2026, 5, 13))));
    }

    @Test
    void namedRangeStaysConsistentWithDateOnlyApi() {
        SenegalHolidayServiceImpl svc = service();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        List<HolidayDto> named = svc.getNamedHolidaysForRange(start, end);
        assertEquals(svc.getHolidaysForRange(start, end).size(), named.size());
        assertTrue(named.stream().allMatch(h -> h.getName() != null && !h.getName().isBlank()));
    }
}
