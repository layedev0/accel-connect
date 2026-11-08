package heritage.africca.accel_connect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AccelConnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccelConnectApplication.class, args);
	}

}
