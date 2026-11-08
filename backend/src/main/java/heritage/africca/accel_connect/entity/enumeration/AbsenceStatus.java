package heritage.africca.accel_connect.entity.enumeration;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AbsenceStatus {

    PENDING_APPROVAL(1, "En attente"),
    APPROVED(2, "Approuvé"),
    REJECTED(3, "Rejeté"),
    CANCELLED(4, "Annulé");

    private final int code;
    private final String displayName;

    AbsenceStatus(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public int getCode() {
        return code;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static AbsenceStatus fromCode(int code) {
        for (AbsenceStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown absence status code: " + code);
    }
}
