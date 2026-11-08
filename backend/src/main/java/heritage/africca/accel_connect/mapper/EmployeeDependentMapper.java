package heritage.africca.accel_connect.mapper;

import heritage.africca.accel_connect.dto.EmployeeDependentDto;
import heritage.africca.accel_connect.dto.EmployeeDependentSummaryDto;
import heritage.africca.accel_connect.entity.EmployeeDependent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmployeeDependentMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    EmployeeDependentDto toDto(EmployeeDependent entity);

    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "seqNo", ignore = true)
    EmployeeDependent toEntity(EmployeeDependentDto dto);

    EmployeeDependentSummaryDto toSummaryDto(EmployeeDependent entity);

    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "seqNo", ignore = true)
    void updateEntity(EmployeeDependentDto dto, @MappingTarget EmployeeDependent entity);
}
