package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.LeaveTypeDto;
import heritage.africca.accel_connect.entity.enumeration.LeaveTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only endpoint that exposes the {@link LeaveTypeCode} enum catalogue.
 * Leave types are now defined in code — no DB table, no admin CRUD.
 */
@RestController
@RequestMapping("/api/v1/leave-types")
@CrossOrigin("*")
@RequiredArgsConstructor
public class LeaveTypeController {

    @GetMapping
    public ResponseEntity<List<LeaveTypeDto>> findAll() {
        List<LeaveTypeDto> types = Arrays.stream(LeaveTypeCode.values())
                .map(LeaveTypeDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @GetMapping("/{code}")
    public ResponseEntity<LeaveTypeDto> getByCode(@PathVariable String code) {
        return LeaveTypeCode.fromCode(code)
                .map(ltc -> ResponseEntity.ok(LeaveTypeDto.from(ltc)))
                .orElse(ResponseEntity.notFound().build());
    }
}
