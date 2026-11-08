package heritage.africca.accel_connect.converter;

import heritage.africca.accel_connect.entity.enumeration.LeaveTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores {@link LeaveTypeCode} as its {@code code} string in the DB column {@code leave_type_code}.
 */
@Converter
public class LeaveTypeConverter implements AttributeConverter<LeaveTypeCode, String> {

    @Override
    public String convertToDatabaseColumn(LeaveTypeCode leaveType) {
        return leaveType == null ? null : leaveType.getCode();
    }

    @Override
    public LeaveTypeCode convertToEntityAttribute(String code) {
        return code == null ? null : LeaveTypeCode.fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown leave type code: " + code));
    }
}
