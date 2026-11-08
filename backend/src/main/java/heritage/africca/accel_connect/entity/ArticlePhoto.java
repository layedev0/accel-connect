package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "article_photos", indexes = @Index(name = "idx_article_photo_article", columnList = "article_id"))
public class ArticlePhoto extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "data", columnDefinition = "bytea", nullable = false)
    private byte[] data;

    @Column(nullable = false)
    private String contentType;

    private String fileName;

    private Long size;

    /** Display order. There is no reorder UI: the upload order is the order. */
    @Column(nullable = false)
    private Integer position;
}
