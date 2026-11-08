package heritage.africca.accel_connect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI - public access
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**","/actuator/**").permitAll()
                        // Routes réservées aux admins
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/wallet/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/balance/deposit", "/api/v1/balance/update",
                                "/api/v1/balance/admin/stats")
                        .hasRole("ADMIN")
                        // Tout le reste nécessite juste une authentification
                        .anyRequest().authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Récupérer les rôles depuis realm_access
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            Collection<String> realmRoles = realmAccess != null
                    ? (Collection<String>) realmAccess.get("roles")
                    : Collections.emptyList();

            // Récupérer les rôles depuis resource_access
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            Collection<String> resourceRoles = new ArrayList<>();
            if (resourceAccess != null) {
                resourceAccess.values().forEach(resource -> {
                    if (resource instanceof Map) {
                        Object roles = ((Map<?, ?>) resource).get("roles");
                        if (roles instanceof Collection) {
                            resourceRoles.addAll((Collection<String>) roles);
                        }
                    }
                });
            }

            // Combiner tous les rôles
            Set<String> allRoles = Stream.concat(
                    realmRoles.stream(),
                    resourceRoles.stream()).collect(Collectors.toSet());

            // Convertir en authorities avec le préfixe ROLE_
            Collection<GrantedAuthority> authorities = allRoles.stream()
                    .map(role -> {
                        // Ajouter ROLE_ si pas déjà présent
                        String roleName = role.toUpperCase();
                        if (!roleName.startsWith("ROLE_")) {
                            roleName = "ROLE_" + roleName;
                        }
                        return new SimpleGrantedAuthority(roleName);
                    })
                    .collect(Collectors.toList());

            // Ajouter aussi les scopes
            JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
            Collection<GrantedAuthority> scopeAuthorities = scopesConverter.convert(jwt);

            authorities.addAll(scopeAuthorities);

            return authorities;
        });

        return converter;
    }
}