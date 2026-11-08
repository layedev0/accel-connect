package heritage.africca.accel_connect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app.leave.observer")
public class LeaveObserverProperties {

    /** Email addresses of the users to notify on every leave/absence request. */
    private List<String> emails = new ArrayList<>();
}
