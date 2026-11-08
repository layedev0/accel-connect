package heritage.africca.accel_connect.converter;

import heritage.africca.accel_connect.entity.enumeration.AbsenceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AbsenceStatusConverter implements AttributeConverter<AbsenceStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AbsenceStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public AbsenceStatus convertToEntityAttribute(Integer code) {
        return code == null ? null : AbsenceStatus.fromCode(code);
    }
}
