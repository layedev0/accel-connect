package heritage.africca.accel_connect.entity;


import heritage.africca.accel_connect.entity.enumeration.ArticleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Entity
@NoArgsConstructor
@Table(name = "articles")
public class Article extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private ArticleType articleType;

    @Column(name = "document_data", columnDefinition = "bytea")
    private byte[] documentData;

    private String documentName;

    private String documentType;

    private Long documentSize;

    @Column(name = "cover_image_data", columnDefinition = "bytea")
    private byte[] coverImageData;

    private String coverImageType;

    /**
     * Who published the article. Nullable: articles created before this column existed have
     * no recorded author, and stamping them with someone would be a fabrication.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

}
