package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBasicInfoDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String image;
    private String role;
}
