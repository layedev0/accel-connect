package heritage.africca.accel_connect.entity.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AbsenceTypeCode {

    MALADE("Malade"),
    URGENCE_FAMILIALE("Urgence familiale"),
    RENDEZ_VOUS("Rendez-vous"),
    ARRIVE_TARD("Arrive tard"),
    QUITTE_TOT("Quitte tôt"),
    LONG_LUNCH("Long Lunch"),
    UNDISCLOSED("Undisclosed"),
    TELECOMMUTE("Telecommute"),
    FERIE("Férié"),
    CONFERENCE_TRAINING("Conference Training");

    private final String displayName;

    AbsenceTypeCode(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static AbsenceTypeCode fromJson(String value) {
        if (value == null) return null;
        for (AbsenceTypeCode t : values()) {
            if (t.displayName.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("Unknown absence type: " + value);
    }

    public static AbsenceTypeCode fromDisplayName(String displayName) {
        return fromJson(displayName);
    }
}
