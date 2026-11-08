package heritage.africca.accel_connect.entity.enumeration;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Leave status enum - serializes as display string in API responses.
 * DB stores integer code; use LeaveStatusConverter for JPA mapping.
 */
public enum LeaveStatus {

    REJECTED(-1, "Rejected"),
    CANCELLED(0, "Cancelled"),
    PENDING_APPROVAL(1, "Pending Approval"),
    SCHEDULED(2, "Scheduled"),
    TAKEN(3, "Taken"),
    WEEKEND(4, "Weekend"),
    HOLIDAY(5, "Holiday");

    private final int code;
    private final String displayName;

    LeaveStatus(int code, String displayName) {
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

    public static LeaveStatus fromCode(int code) {
        for (LeaveStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown leave status code: " + code);
    }
}
