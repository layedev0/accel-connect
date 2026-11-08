package heritage.africca.accel_connect.dto;

import heritage.africca.accel_connect.entity.enumeration.ArticleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDTO {

    private Long id;
    private String title;
    private String content;
    private ArticleType articleType;
    private String documentName;
    private String documentType;
    private Long documentSize;
    private boolean hasCoverImage;

    /** Publication date, set by {@link heritage.africca.accel_connect.entity.DateAudit} on insert. Read-only. */
    private LocalDateTime createdAt;

    /** Who published the article. Read-only, and absent on articles predating the column. */
    private String authorName;
}
