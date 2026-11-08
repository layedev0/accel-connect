package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.ArticleLikeStatusDTO;
import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.entity.ArticleLike;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.ArticleLikeRepository;
import heritage.africca.accel_connect.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleLikeService")
class ArticleLikeServiceTest {

    private static final Long ARTICLE_ID = 7L;
    private static final Long USER_ID = 1L;

    @Mock private ArticleLikeRepository likeRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private UserService userService;

    @InjectMocks
    private ArticleLikeService likeService;

    private Article article;
    private User current;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        article = new Article();
        article.setId(ARTICLE_ID);

        current = new User();
        current.setId(USER_ID);
        current.setUsername("pape");

        authentication = new UsernamePasswordAuthenticationToken("principal", "credentials", List.of());
    }

    private void articleExists() {
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));
        when(userService.getCurrentUser(any())).thenReturn(current);
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("reports the count and whether the caller already liked the article")
        void reportsCountAndOwnLike() {
            articleExists();
            when(likeRepository.countByArticleId(ARTICLE_ID)).thenReturn(12L);
            when(likeRepository.existsByArticleIdAndUserId(ARTICLE_ID, USER_ID)).thenReturn(true);

            ArticleLikeStatusDTO status = likeService.getStatus(ARTICLE_ID, authentication);

            assertEquals(12L, status.getLikeCount());
            assertTrue(status.isLikedByCurrentUser());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the article does not exist")
        void throwsWhenArticleMissing() {
            when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> likeService.getStatus(ARTICLE_ID, authentication));
        }
    }

    @Nested
    @DisplayName("like")
    class Like {

        @Test
        @DisplayName("saves a like bound to the article and the current user")
        void savesLike() {
            articleExists();
            when(likeRepository.existsByArticleIdAndUserId(ARTICLE_ID, USER_ID))
                    .thenReturn(false)   // before the insert
                    .thenReturn(true);   // when the status is rebuilt
            when(likeRepository.countByArticleId(ARTICLE_ID)).thenReturn(1L);

            ArticleLikeStatusDTO status = likeService.like(ARTICLE_ID, authentication);

            ArgumentCaptor<ArticleLike> captor = ArgumentCaptor.forClass(ArticleLike.class);
            verify(likeRepository).saveAndFlush(captor.capture());
            assertSame(article, captor.getValue().getArticle());
            assertSame(current, captor.getValue().getUser());

            assertEquals(1L, status.getLikeCount());
            assertTrue(status.isLikedByCurrentUser());
        }

        @Test
        @DisplayName("does not insert a second like when the caller already liked it")
        void isIdempotent() {
            articleExists();
            when(likeRepository.existsByArticleIdAndUserId(ARTICLE_ID, USER_ID)).thenReturn(true);
            when(likeRepository.countByArticleId(ARTICLE_ID)).thenReturn(1L);

            ArticleLikeStatusDTO status = likeService.like(ARTICLE_ID, authentication);

            verify(likeRepository, never()).saveAndFlush(any());
            assertEquals(1L, status.getLikeCount());
            assertTrue(status.isLikedByCurrentUser());
        }

        @Test
        @DisplayName("swallows the unique-constraint violation of a concurrent double click")
        void survivesConcurrentInsert() {
            articleExists();
            when(likeRepository.existsByArticleIdAndUserId(ARTICLE_ID, USER_ID))
                    .thenReturn(false)
                    .thenReturn(true);
            when(likeRepository.saveAndFlush(any(ArticleLike.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_article_like"));
            when(likeRepository.countByArticleId(ARTICLE_ID)).thenReturn(1L);

            ArticleLikeStatusDTO status = assertDoesNotThrow(
                    () -> likeService.like(ARTICLE_ID, authentication));

            assertEquals(1L, status.getLikeCount());
            assertTrue(status.isLikedByCurrentUser());
        }
    }

    @Nested
    @DisplayName("unlike")
    class Unlike {

        @Test
        @DisplayName("removes the caller's like and returns the refreshed status")
        void removesLike() {
            articleExists();
            when(likeRepository.countByArticleId(ARTICLE_ID)).thenReturn(0L);
            when(likeRepository.existsByArticleIdAndUserId(ARTICLE_ID, USER_ID)).thenReturn(false);

            ArticleLikeStatusDTO status = likeService.unlike(ARTICLE_ID, authentication);

            verify(likeRepository).deleteByArticleIdAndUserId(ARTICLE_ID, USER_ID);
            assertEquals(0L, status.getLikeCount());
            assertFalse(status.isLikedByCurrentUser());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the article does not exist")
        void throwsWhenArticleMissing() {
            when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> likeService.unlike(ARTICLE_ID, authentication));
            verify(likeRepository, never()).deleteByArticleIdAndUserId(any(), any());
        }
    }
}
