package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.ArticlePhotoDTO;
import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.entity.ArticlePhoto;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.ArticlePhotoRepository;
import heritage.africca.accel_connect.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArticlePhotoService {

    private static final long MAX_PHOTO_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final int MAX_PHOTOS_PER_ARTICLE = 10;
    private static final List<String> IMAGE_TYPES = List.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE
    );

    private final ArticlePhotoRepository photoRepository;
    private final ArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public List<ArticlePhotoDTO> getPhotos(Long articleId) {
        requireArticle(articleId);
        return photoRepository.findMetadataByArticleId(articleId);
    }

    /** Returns the entity, bytes included, so the controller can stream it. */
    @Transactional(readOnly = true)
    public ArticlePhoto getPhotoWithData(Long articleId, Long photoId) {
        return photoRepository.findByIdAndArticleId(photoId, articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));
    }

    @Transactional
    public ArticlePhotoDTO addPhoto(Long articleId, MultipartFile file) {
        Article article = requireArticle(articleId);

        long existing = photoRepository.countByArticleId(articleId);
        if (existing >= MAX_PHOTOS_PER_ARTICLE) {
            throw new IllegalArgumentException(
                    "Un article ne peut pas contenir plus de " + MAX_PHOTOS_PER_ARTICLE + " photos.");
        }

        validate(file);

        ArticlePhoto photo = new ArticlePhoto();
        photo.setArticle(article);
        photo.setContentType(file.getContentType());
        photo.setFileName(file.getOriginalFilename());
        photo.setSize(file.getSize());
        // Gaps left by deletions are harmless: only the relative order matters.
        photo.setPosition((int) existing + 1);
        try {
            photo.setData(file.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire la photo téléversée.", e);
        }

        ArticlePhoto saved = photoRepository.save(photo);
        log.info("Photo {} ajoutée à l'article {}", saved.getId(), articleId);

        return toDto(saved);
    }

    @Transactional
    public void deletePhoto(Long articleId, Long photoId) {
        // A photo id belonging to another article is a wrong address, not a permission problem.
        ArticlePhoto photo = photoRepository.findByIdAndArticleId(photoId, articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));

        photoRepository.delete(photo);
        log.info("Photo {} supprimée de l'article {}", photoId, articleId);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier envoyé est vide.");
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw new IllegalArgumentException("La photo dépasse la taille maximale autorisée (10 Mo).");
        }
        String contentType = file.getContentType();
        if (contentType == null || !IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Photo non autorisée. Formats acceptés : JPEG, PNG.");
        }
    }

    private Article requireArticle(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));
    }

    private ArticlePhotoDTO toDto(ArticlePhoto photo) {
        return new ArticlePhotoDTO(
                photo.getId(),
                photo.getFileName(),
                photo.getContentType(),
                photo.getSize(),
                photo.getPosition()
        );
    }
}
