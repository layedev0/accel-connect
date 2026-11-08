package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCommentDTO {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private Long authorId;
    private String authorName;

    /** True when the caller may delete this comment: its author, or a moderator. */
    private boolean canDelete;
}
