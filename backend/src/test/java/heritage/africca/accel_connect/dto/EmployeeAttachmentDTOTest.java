package heritage.africca.accel_connect.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmployeeAttachmentDTO")
class EmployeeAttachmentDTOTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 5, 10, 0);

    private EmployeeAttachmentDTO build(Long userId, Integer attachId) {
        return new EmployeeAttachmentDTO(
                userId, attachId,
                "Bulletin de salaire", "bulletin_mars_2026.pdf",
                204800, "application/pdf",
                "bulletin-salaire", "Aminata Diallo", NOW
        );
    }

    @Test
    @DisplayName("record components are accessible via accessors")
    void accessors() {
        var dto = build(1L, 1);

        assertEquals(1L, dto.userId());
        assertEquals(1, dto.attachId());
        assertEquals("Bulletin de salaire", dto.description());
        assertEquals("bulletin_mars_2026.pdf", dto.filename());
        assertEquals(204800, dto.size());
        assertEquals("application/pdf", dto.fileType());
        assertEquals("bulletin-salaire", dto.screen());
        assertEquals("Aminata Diallo", dto.attachedByName());
        assertEquals(NOW, dto.createdAt());
    }

    @Test
    @DisplayName("two records with identical values are equal")
    void equality() {
        var a = build(1L, 1);
        var b = build(1L, 1);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("records with different attachId are not equal")
    void inequalityOnAttachId() {
        assertNotEquals(build(1L, 1), build(1L, 2));
    }

    @Test
    @DisplayName("records with different userId are not equal")
    void inequalityOnUserId() {
        assertNotEquals(build(1L, 1), build(2L, 1));
    }

    @Test
    @DisplayName("toString contains key field values")
    void toStringContainsFields() {
        String s = build(42L, 7).toString();
        assertTrue(s.contains("42"));
        assertTrue(s.contains("7"));
        assertTrue(s.contains("bulletin_mars_2026.pdf"));
    }

    @Test
    @DisplayName("null fields are accepted")
    void nullFields() {
        var dto = new EmployeeAttachmentDTO(null, null, null, null, null, null, null, null, null);
        assertNull(dto.userId());
        assertNull(dto.filename());
        assertNull(dto.createdAt());
    }
}
