package heritage.africca.accel_connect.mapper;

import heritage.africca.accel_connect.entity.User;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Turns a {@link User} into the name shown to readers. Shared so that comments and articles
 * label the same person the same way.
 */
@Component
public class UserNameMapper {

    @Named("displayName")
    public String displayName(User user) {
        if (user == null) return null;
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getUsername() : fullName;
    }
}
