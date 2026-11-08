package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.entity.enumeration.AbsenceTypeCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/absence-types")
@CrossOrigin("*")
public class AbsenceTypeController {

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> findAll() {
        List<Map<String, String>> types = Arrays.stream(AbsenceTypeCode.values())
                .map(t -> Map.of("code", t.name(), "name", t.getDisplayName()))
                .toList();
        return ResponseEntity.ok(types);
    }
}
