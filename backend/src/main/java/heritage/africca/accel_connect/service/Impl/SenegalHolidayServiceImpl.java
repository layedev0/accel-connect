package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.config.SenegalHolidayProperties;
import heritage.africca.accel_connect.dto.HolidayDto;
import heritage.africca.accel_connect.service.SenegalHolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SenegalHolidayServiceImpl implements SenegalHolidayService {

    private final SenegalHolidayProperties holidayProperties;

    /** Cache of date -> French holiday name, keyed by year. */
    private final Map<Integer, Map<LocalDate, String>> cache = new ConcurrentHashMap<>();

    @Override
    public Set<LocalDate> getHolidaysForYear(int year) {
        return Collections.unmodifiableSet(getNamedHolidaysForYear(year).keySet());
    }

    @Override
    public Set<LocalDate> getHolidaysForRange(LocalDate start, LocalDate end) {
        Set<LocalDate> holidays = new HashSet<>();
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            holidays.addAll(getHolidaysForYear(year));
        }
        return holidays;
    }

    @Override
    public List<HolidayDto> getNamedHolidaysForRange(LocalDate start, LocalDate end) {
        List<HolidayDto> result = new ArrayList<>();
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            getNamedHolidaysForYear(year).forEach((date, name) -> {
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    result.add(HolidayDto.builder().date(date).name(name).build());
                }
            });
        }
        result.sort(Comparator.comparing(HolidayDto::getDate));
        return result;
    }

    private Map<LocalDate, String> getNamedHolidaysForYear(int year) {
        return cache.computeIfAbsent(year, this::buildHolidaysForYear);
    }

    private Map<LocalDate, String> buildHolidaysForYear(int year) {
        Map<LocalDate, String> holidays = new HashMap<>();

        // Fixed Gregorian holidays
        holidays.put(LocalDate.of(year, 1, 1), "Nouvel An");
        holidays.put(LocalDate.of(year, 4, 4), "Fête de l'Indépendance");
        holidays.put(LocalDate.of(year, 5, 1), "Fête du Travail");
        holidays.put(LocalDate.of(year, 8, 15), "Assomption");
        holidays.put(LocalDate.of(year, 11, 1), "Toussaint");
        holidays.put(LocalDate.of(year, 12, 25), "Noël");

        // Easter-based Christian holidays (calculated)
        LocalDate easter = computeEaster(year);
        holidays.put(easter.plusDays(1), "Lundi de Pâques");
        holidays.put(easter.plusDays(39), "Ascension");
        holidays.put(easter.plusDays(50), "Lundi de Pentecôte");

        // Islamic holidays (dates vary yearly, read from config; names not provided by config)
        String islamicDates = holidayProperties.getIslamic().get(String.valueOf(year));
        if (islamicDates != null && !islamicDates.isBlank()) {
            for (String dateStr : islamicDates.split(",")) {
                try {
                    holidays.putIfAbsent(LocalDate.parse(dateStr.trim()), "Fête religieuse");
                } catch (Exception e) {
                    log.warn("Invalid Islamic holiday date '{}' for year {}", dateStr.trim(), year);
                }
            }
        } else {
            log.warn("No Islamic holiday dates configured for year {}. "
                    + "Add 'app.holidays.senegal.islamic.{}' to application-dev.properties.", year, year);
        }

        log.info("Loaded {} public holidays for Senegal ({})", holidays.size(), year);
        return Collections.unmodifiableMap(holidays);
    }

    // Anonymous Gregorian algorithm for Easter Sunday
    private LocalDate computeEaster(int year) {
        int a = year % 19, b = year / 100, c = year % 100;
        int d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
