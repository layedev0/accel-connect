package heritage.africca.accel_connect.dto;

public class LeaveResponseDto {
    private Integer id;
    private String date;
    private String lengthHours;
    private String lengthDays;
    private String status;
    private String leaveTypeName;
    private Long employeeId;
    private String startTime;
    private String endTime;
    private Integer durationType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLengthHours() {
        return lengthHours;
    }

    public void setLengthHours(String lengthHours) {
        this.lengthHours = lengthHours;
    }

    public String getLengthDays() {
        return lengthDays;
    }

    public void setLengthDays(String lengthDays) {
        this.lengthDays = lengthDays;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLeaveTypeName() {
        return leaveTypeName;
    }

    public void setLeaveTypeName(String leaveTypeName) {
        this.leaveTypeName = leaveTypeName;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getDurationType() {
        return durationType;
    }

    public void setDurationType(Integer durationType) {
        this.durationType = durationType;
    }
}
