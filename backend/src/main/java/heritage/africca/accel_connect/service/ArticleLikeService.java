package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.ArticleLikeStatusDTO;
import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.entity.ArticleLike;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.ArticleLikeRepository;
import heritage.africca.accel_connect.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArticleLikeService {

    private final ArticleLikeRepository likeRepository;
    private final ArticleRepository articleRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public ArticleLikeStatusDTO getStatus(Long articleId, Authentication authentication) {
        requireArticle(articleId);
        User current = userService.getCurrentUser(authentication);
        return buildStatus(articleId, current.getId());
    }

    @Transactional
    public ArticleLikeStatusDTO like(Long articleId, Authentication authentication) {
        Article article = requireArticle(articleId);
        User current = userService.getCurrentUser(authentication);

        if (!likeRepository.existsByArticleIdAndUserId(articleId, current.getId())) {
            ArticleLike like = new ArticleLike();
            like.setArticle(article);
            like.setUser(current);
            try {
                likeRepository.saveAndFlush(like);
            } catch (DataIntegrityViolationException e) {
                // Two concurrent clicks: the unique constraint won, and the like already exists.
                log.debug("Like déjà enregistré pour l'article {} et l'utilisateur {}", articleId, current.getId());
            }
        }

        return buildStatus(articleId, current.getId());
    }

    @Transactional
    public ArticleLikeStatusDTO unlike(Long articleId, Authentication authentication) {
        requireArticle(articleId);
        User current = userService.getCurrentUser(authentication);

        likeRepository.deleteByArticleIdAndUserId(articleId, current.getId());

        return buildStatus(articleId, current.getId());
    }

    private ArticleLikeStatusDTO buildStatus(Long articleId, Long userId) {
        return new ArticleLikeStatusDTO(
                likeRepository.countByArticleId(articleId),
                likeRepository.existsByArticleIdAndUserId(articleId, userId)
        );
    }

    private Article requireArticle(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));
    }
}
