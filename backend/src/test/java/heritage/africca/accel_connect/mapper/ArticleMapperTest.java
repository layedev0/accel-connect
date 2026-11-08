package heritage.africca.accel_connect.mapper;

import heritage.africca.accel_connect.dto.ArticleDTO;
import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.enumeration.ArticleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the generated mapper directly. A service test cannot cover this: with a mocked
 * mapper, the `@Mapping(ignore = true)` annotations are exactly what goes untested.
 */
@DisplayName("ArticleMapper (generated)")
class ArticleMapperTest {

    private final ArticleMapper mapper = new ArticleMapperImpl();

    private static User user(String firstName, String lastName, String username) {
        User user = new User();
        user.setId(1L);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        return user;
    }

    @Test
    @DisplayName("updateEntity leaves the author untouched: the incoming DTO never carries one")
    void updateEntityPreservesAuthor() {
        User author = user("Pape", "Ndiaye", "pape");
        Article existing = new Article();
        existing.setTitle("Ancien titre");
        existing.setAuthor(author);

        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("Nouveau titre");
        dto.setContent("Contenu mis à jour");
        dto.setArticleType(ArticleType.FORMATION);

        mapper.updateEntity(dto, existing);

        assertEquals("Nouveau titre", existing.getTitle());
        assertSame(author, existing.getAuthor(), "editing an article must not reassign it");
    }

    @Test
    @DisplayName("updateEntity leaves createdAt untouched")
    void updateEntityPreservesCreatedAt() {
        LocalDateTime published = LocalDateTime.of(2026, 3, 14, 9, 0);
        Article existing = new Article();
        existing.setCreatedAt(published);

        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("Nouveau titre");

        mapper.updateEntity(dto, existing);

        assertEquals(published, existing.getCreatedAt());
    }

    @Test
    @DisplayName("toEntity never takes an author from the DTO")
    void toEntityIgnoresAuthor() {
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("Titre");
        dto.setAuthorName("Quelqu'un d'autre");

        Article entity = mapper.toEntity(dto);

        assertNull(entity.getAuthor());
    }
}
