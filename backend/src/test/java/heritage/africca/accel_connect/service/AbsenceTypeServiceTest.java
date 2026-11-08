package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.entity.enumeration.AbsenceTypeCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbsenceTypeServiceTest {

    @Test
    void enumContainsAllExpectedValues() {
        AbsenceTypeCode[] values = AbsenceTypeCode.values();
        assertEquals(10, values.length);
    }

    @Test
    void eachValueHasDisplayName() {
        for (AbsenceTypeCode type : AbsenceTypeCode.values()) {
            assertNotNull(type.getDisplayName(), type.name() + " must have a display name");
            assertFalse(type.getDisplayName().isBlank(), type.name() + " display name must not be blank");
        }
    }

    @Test
    void fromDisplayNameReturnsCorrectValue() {
        assertEquals(AbsenceTypeCode.MALADE, AbsenceTypeCode.fromDisplayName("Malade"));
        assertEquals(AbsenceTypeCode.FERIE, AbsenceTypeCode.fromDisplayName("Férié"));
        assertEquals(AbsenceTypeCode.CONFERENCE_TRAINING, AbsenceTypeCode.fromDisplayName("Conference Training"));
    }

    @Test
    void fromDisplayNameThrowsOnUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> AbsenceTypeCode.fromDisplayName("Unknown"));
    }
}
