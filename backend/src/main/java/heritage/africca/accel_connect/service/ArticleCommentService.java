package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.ArticleCommentDTO;
import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.entity.ArticleComment;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.mapper.ArticleCommentMapper;
import heritage.africca.accel_connect.repository.ArticleCommentRepository;
import heritage.africca.accel_connect.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArticleCommentService {

    /** Roles allowed to delete anyone's comment. */
    private static final Set<String> MODERATOR_ROLES = Set.of("ROLE_SUPER_ADMIN");

    private final ArticleCommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final ArticleCommentMapper commentMapper;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ArticleCommentDTO> getComments(Long articleId, Authentication authentication) {
        requireArticle(articleId);

        User current = userService.getCurrentUser(authentication);
        boolean moderator = isModerator(authentication);

        return commentRepository.findByArticleIdWithAuthor(articleId).stream()
                .map(comment -> {
                    ArticleCommentDTO dto = commentMapper.toDto(comment);
                    dto.setCanDelete(moderator || comment.getAuthor().getId().equals(current.getId()));
                    return dto;
                })
                .toList();
    }

    @Transactional
    public ArticleCommentDTO addComment(Long articleId, String content, Authentication authentication) {
        Article article = requireArticle(articleId);
        User author = userService.getCurrentUser(authentication);

        ArticleComment comment = new ArticleComment();
        comment.setArticle(article);
        comment.setAuthor(author);
        comment.setContent(content.trim());

        ArticleComment saved = commentRepository.save(comment);
        log.info("Commentaire {} ajouté sur l'article {} par {}", saved.getId(), articleId, author.getUsername());

        ArticleCommentDTO dto = commentMapper.toDto(saved);
        dto.setCanDelete(true);
        return dto;
    }

    @Transactional
    public void deleteComment(Long articleId, Long commentId, Authentication authentication) {
        ArticleComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire", commentId));

        // A comment id belonging to another article is a wrong address, not a permission problem.
        if (!comment.getArticle().getId().equals(articleId)) {
            throw new ResourceNotFoundException("Commentaire", commentId);
        }

        User current = userService.getCurrentUser(authentication);
        if (!comment.getAuthor().getId().equals(current.getId()) && !isModerator(authentication)) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres commentaires.");
        }

        commentRepository.delete(comment);
        log.info("Commentaire {} supprimé par {}", commentId, current.getUsername());
    }

    private Article requireArticle(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));
    }

    private boolean isModerator(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(MODERATOR_ROLES::contains);
    }
}
