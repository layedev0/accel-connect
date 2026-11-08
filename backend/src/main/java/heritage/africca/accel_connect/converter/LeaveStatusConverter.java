package heritage.africca.accel_connect.converter;

import heritage.africca.accel_connect.entity.enumeration.LeaveStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts between LeaveStatus enum and integer for DB storage.
 */
@Converter
public class LeaveStatusConverter implements AttributeConverter<LeaveStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(LeaveStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public LeaveStatus convertToEntityAttribute(Integer code) {
        return code == null ? null : LeaveStatus.fromCode(code);
    }
}
