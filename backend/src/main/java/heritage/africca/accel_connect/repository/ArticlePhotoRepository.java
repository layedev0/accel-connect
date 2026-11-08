package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.dto.ArticlePhotoDTO;
import heritage.africca.accel_connect.entity.ArticlePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticlePhotoRepository extends JpaRepository<ArticlePhoto, Long> {

    /**
     * Constructor expression rather than a mapper: loading the entity would pull the whole
     * bytea column just to list file names.
     */
    @Query("""
            SELECT new heritage.africca.accel_connect.dto.ArticlePhotoDTO(
                p.id, p.fileName, p.contentType, p.size, p.position)
            FROM ArticlePhoto p
            WHERE p.article.id = :articleId
            ORDER BY p.position ASC
            """)
    List<ArticlePhotoDTO> findMetadataByArticleId(@Param("articleId") Long articleId);

    Optional<ArticlePhoto> findByIdAndArticleId(Long id, Long articleId);

    long countByArticleId(Long articleId);

    void deleteByArticleId(Long articleId);
}
