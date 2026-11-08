package heritage.africca.accel_connect.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Initialise/met a jour les vues SQL au demarrage de l'application.
 * Permet de deployer des changements de vues sans intervention manuelle sur la base.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ViewsInitializer {

    private final DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeViews() {
        try {
            log.info("Initialisation des vues SQL...");

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/views.sql"));
            populator.setSeparator(";");
            populator.execute(dataSource);

            log.info("Vues SQL initialisees avec succes");
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des vues SQL", e);
        }
    }
}
