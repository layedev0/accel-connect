package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.ArticleDTO;
import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.mapper.ArticleMapper;
import heritage.africca.accel_connect.mapper.UserNameMapper;
import heritage.africca.accel_connect.repository.ArticleCommentRepository;
import heritage.africca.accel_connect.repository.ArticleLikeRepository;
import heritage.africca.accel_connect.repository.ArticlePhotoRepository;
import heritage.africca.accel_connect.repository.ArticleRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArticleService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final List<String> ALLOWED_TYPES = List.of(
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE
    );
    private static final String NEW_ARTICLE_TITLE = "📰 Nouvelle actualité";
    /** Notification.message is a varchar(500) at both entity and database level. */
    private static final int MESSAGE_MAX = 500;
    private static final List<String> IMAGE_TYPES = List.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE
    );

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final ArticleCommentRepository articleCommentRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final ArticlePhotoRepository articlePhotoRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserNameMapper userNameMapper;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<ArticleDTO> getAllArticles() {
        return articleRepository.findAllNewestFirst().stream()
                .map(articleMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticleDTO getArticleById(Long id) {
        return articleMapper.toDto(findArticle(id));
    }

    /** Returns the entity (with document bytes) so the controller can stream the file. */
    @Transactional(readOnly = true)
    public Article getArticleWithDocument(Long id) {
        return findArticle(id);
    }

    /** Returns the entity (with cover image bytes) so the controller can stream the cover. */
    @Transactional(readOnly = true)
    public Article getArticleWithCover(Long id) {
        return findArticle(id);
    }

    @Transactional
    public ArticleDTO createArticle(ArticleDTO dto, MultipartFile file, MultipartFile coverImage,
                                    Authentication authentication) {
        // A document is optional; validate it before building anything, so a rejected upload
        // never reaches the mapper or the user lookup.
        boolean hasDocument = file != null && !file.isEmpty();
        if (hasDocument) {
            validateFile(file);
        }

        Article article = articleMapper.toEntity(dto);
        // Stamped once, at creation: editing an article never reassigns its author.
        article.setAuthor(userService.getCurrentUser(authentication));

        if (hasDocument) {
            applyDocument(article, file);
        }
        if (coverImage != null && !coverImage.isEmpty()) {
            applyCover(article, coverImage);
        }

        Article saved = articleRepository.save(article);
        notifyEveryone(saved);
        log.info("Article créé: {}", saved.getTitle());
        return articleMapper.toDto(saved);
    }

    @Transactional
    public ArticleDTO updateArticle(Long id, ArticleDTO dto, MultipartFile file, MultipartFile coverImage) {
        Article article = findArticle(id);
        articleMapper.updateEntity(dto, article);

        // Replace the document only when a new file is supplied.
        if (file != null && !file.isEmpty()) {
            validateFile(file);
            applyDocument(article, file);
        }
        // Replace the cover image only when a new one is supplied.
        if (coverImage != null && !coverImage.isEmpty()) {
            applyCover(article, coverImage);
        }

        Article updated = articleRepository.save(article);
        log.info("Article mis à jour: {}", updated.getTitle());
        return articleMapper.toDto(updated);
    }

    @Transactional
    public void deleteArticle(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Article", id);
        }
        // Hibernate's ddl-auto creates the foreign keys without ON DELETE CASCADE, so the
        // children have to go first or the delete fails on a constraint violation.
        articleCommentRepository.deleteByArticleId(id);
        articleLikeRepository.deleteByArticleId(id);
        articlePhotoRepository.deleteByArticleId(id);
        articleRepository.deleteById(id);
        log.info("Article supprimé: {}", id);
    }

    /**
     * Every employee is told about a new article.
     */
    private void notifyEveryone(Article article) {
        String link = frontendUrl + "/actualites/" + article.getId();
        String message = truncate(userNameMapper.displayName(article.getAuthor())
                + " a publié : " + article.getTitle());

        notificationService.createForUsers(userRepository.findAll(), NEW_ARTICLE_TITLE, message, link);
    }

    /** Notification.message is capped at 500 characters; an article title has no limit. */
    private String truncate(String message) {
        return message.length() <= MESSAGE_MAX ? message : message.substring(0, MESSAGE_MAX - 1) + "…";
    }

    private Article findArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale autorisée (10 Mo).");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Formats acceptés : PDF, JPEG, PNG.");
        }
    }

    private void applyDocument(Article article, MultipartFile file) {
        try {
            article.setDocumentData(file.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier téléversé.", e);
        }
        article.setDocumentName(file.getOriginalFilename());
        article.setDocumentType(file.getContentType());
        article.setDocumentSize(file.getSize());
    }

    private void applyCover(Article article, MultipartFile coverImage) {
        if (coverImage.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("L'image de couverture dépasse la taille maximale autorisée (10 Mo).");
        }
        String contentType = coverImage.getContentType();
        if (contentType == null || !IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Image de couverture non autorisée. Formats acceptés : JPEG, PNG.");
        }
        try {
            article.setCoverImageData(coverImage.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire l'image de couverture téléversée.", e);
        }
        article.setCoverImageType(contentType);
    }
}
