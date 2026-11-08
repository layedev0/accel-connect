package heritage.africca.accel_connect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.holidays.senegal")
public class SenegalHolidayProperties {

    // Keys are years as strings ("2025", "2026"), values are comma-separated ISO dates
    private Map<String, String> islamic = new HashMap<>();

    public Map<String, String> getIslamic() { return islamic; }
    public void setIslamic(Map<String, String> islamic) { this.islamic = islamic; }
}
