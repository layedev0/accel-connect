package heritage.africca.accel_connect.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class NextcloudTalkService {

    @Value("${nextcloud.url}")
    private String nextcloudUrl;

    @Value("${nextcloud.username}")
    private String botUsername;

    @Value("${nextcloud.password}")
    private String botPassword;

    // Timeout 10s par appel pour éviter de bloquer trop longtemps
    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
            .build();

    public boolean sendDirectMessage(String nextcloudAccountId, String message) {
        try {
            String roomToken = getOrCreateDirectRoom(nextcloudAccountId);
            if (roomToken == null) {
                log.warn("⚠️ Conversation introuvable pour {}", nextcloudAccountId);
                return false;
            }
            return sendMessageToRoom(roomToken, message);
        } catch (Exception e) {
            log.error("❌ Erreur envoi message à {}: {}", nextcloudAccountId, e.getMessage());
            return false;
        }
    }

    private String getOrCreateDirectRoom(String nextcloudAccountId) {
        try {
            String url = nextcloudUrl + "/ocs/v2.php/apps/spreed/api/v4/room";

            HttpHeaders headers = buildHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "roomType=1&invite=" + nextcloudAccountId + "&source=users";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> ocs = (Map<?, ?>) response.getBody().get("ocs");
                if (ocs != null) {
                    Map<?, ?> data = (Map<?, ?>) ocs.get("data");
                    if (data != null) {
                        return (String) data.get("token");
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur création conversation avec {}: {}", nextcloudAccountId, e.getMessage());
        }
        return null;
    }

    private boolean sendMessageToRoom(String roomToken, String message) {
        try {
            String url = nextcloudUrl + "/ocs/v2.php/apps/spreed/api/v1/chat/" + roomToken;

            HttpHeaders headers = buildHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "message=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("❌ Erreur envoi message room {}: {}", roomToken, e.getMessage());
            return false;
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = botUsername + ":" + botPassword;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
        headers.set("Authorization", "Basic " + encoded);
        headers.set("OCS-APIREQUEST", "true");
        headers.set("Accept", "application/json");
        return headers;
    }
}
