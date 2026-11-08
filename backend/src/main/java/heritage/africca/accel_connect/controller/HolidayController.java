package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.HolidayDto;
import heritage.africca.accel_connect.service.SenegalHolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
@CrossOrigin("*")
@RequiredArgsConstructor
public class HolidayController {

    private final SenegalHolidayService holidayService;

    @GetMapping
    public ResponseEntity<List<HolidayDto>> getHolidays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(holidayService.getNamedHolidaysForRange(start, end));
    }
}
