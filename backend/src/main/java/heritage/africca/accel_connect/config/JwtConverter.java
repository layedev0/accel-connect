package heritage.africca.accel_connect.config;

import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Value("${jwt.auth.converter.principal-attribute:preferred_username}")
    private String principalAttribute;

    @Value("${jwt.auth.converter.resource-id:accel}")
    private String resourceId;

    @Override
    public @NonNull AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
                extractResourceRoles(jwt).stream()).collect(Collectors.toSet());

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                getPrincipalClaimName(jwt));
    }

    private String getPrincipalClaimName(Jwt jwt) {
        String claimName = JwtClaimNames.SUB;
        if (principalAttribute != null && !principalAttribute.isEmpty()) {
            claimName = principalAttribute;
        }
        return jwt.getClaim(claimName);
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess;
        Map<?, ?> resource;
        Collection<?> resourceRoles;

        if (jwt.getClaim("resource_access") == null) {
            return Set.of();
        }

        resourceAccess = jwt.getClaim("resource_access");

        Object resourceObj = resourceAccess.get(resourceId);
        if (!(resourceObj instanceof Map)) {
            return Set.of();
        }
        resource = (Map<?, ?>) resourceObj;

        Object rolesObj = resource.get("roles");
        if (!(rolesObj instanceof Collection)) {
            return Set.of();
        }
        resourceRoles = (Collection<?>) rolesObj;

        return resourceRoles.stream()
                .filter(String.class::isInstance)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + ((String) role).toUpperCase()))
                .collect(Collectors.toSet());
    }
}