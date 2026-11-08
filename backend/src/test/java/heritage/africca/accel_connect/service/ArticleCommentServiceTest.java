package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.ArticleCommentDTO;
import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.entity.ArticleComment;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.mapper.ArticleCommentMapper;
import heritage.africca.accel_connect.repository.ArticleCommentRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleCommentService")
class ArticleCommentServiceTest {

    private static final Long ARTICLE_ID = 7L;

    @Mock private ArticleCommentRepository commentRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private ArticleCommentMapper commentMapper;
    @Mock private UserService userService;

    @InjectMocks
    private ArticleCommentService commentService;

    private Article article;
    private User author;
    private User someoneElse;

    @BeforeEach
    void setUp() {
        article = new Article();
        article.setId(ARTICLE_ID);

        author = user(1L, "pape");
        someoneElse = user(2L, "aicha");
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    /** An authenticated caller carrying the given authorities (none for a plain employee). */
    private static Authentication authenticationWith(String... roles) {
        return new UsernamePasswordAuthenticationToken(
                "principal",
                "credentials",
                List.of(roles).stream().map(SimpleGrantedAuthority::new).toList()
        );
    }

    private ArticleComment comment(Long id, User commentAuthor, Article onArticle) {
        ArticleComment comment = new ArticleComment();
        comment.setId(id);
        comment.setArticle(onArticle);
        comment.setAuthor(commentAuthor);
        comment.setContent("Très bonne initiative");
        return comment;
    }

    /** The mapper is a mock, so hand back a fresh DTO carrying the author id it was given. */
    private void stubMapper() {
        when(commentMapper.toDto(any(ArticleComment.class))).thenAnswer(invocation -> {
            ArticleComment source = invocation.getArgument(0);
            ArticleCommentDTO dto = new ArticleCommentDTO();
            dto.setId(source.getId());
            dto.setContent(source.getContent());
            dto.setAuthorId(source.getAuthor().getId());
            return dto;
        });
    }

    @Nested
    @DisplayName("getComments")
    class GetComments {

        @Test
        @DisplayName("marks canDelete on the caller's own comments only")
        void marksOwnCommentsDeletable() {
            when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));
            when(userService.getCurrentUser(any())).thenReturn(author);
            when(commentRepository.findByArticleIdWithAuthor(ARTICLE_ID)).thenReturn(List.of(
                    comment(10L, author, article),
                    comment(11L, someoneElse, article)
            ));
            stubMapper();

            List<ArticleCommentDTO> result = commentService.getComments(ARTICLE_ID, authenticationWith());

            assertEquals(2, result.size());
            assertTrue(result.get(0).isCanDelete(), "own comment should be deletable");
            assertFalse(result.get(1).isCanDelete(), "someone else's comment should not be deletable");
        }

        @Test
        @DisplayName("marks every comment deletable for a super admin")
        void marksAllCommentsDeletableForModerator() {
            when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));
            when(userService.getCurrentUser(any())).thenReturn(author);
            when(commentRepository.findByArticleIdWithAuthor(ARTICLE_ID))
                    .thenReturn(List.of(comment(11L, someoneElse, article)));
            stubMapper();

            List<ArticleCommentDTO> result =
                    commentService.getComments(ARTICLE_ID, authenticationWith("ROLE_SUPER_ADMIN"));

            assertTrue(result.get(0).isCanDelete());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the article does not exist")
        void throwsWhenArticleMissing() {
            when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.getComments(ARTICLE_ID, authenticationWith()));
        }
    }

    @Nested
    @DisplayName("addComment")
    class AddComment {

        @Test
        @DisplayName("persists the trimmed content against the article and the current user")
        void persistsComment() {
            when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));
            when(userService.getCurrentUser(any())).thenReturn(author);
            when(commentRepository.save(any(ArticleComment.class))).thenAnswer(inv -> inv.getArgument(0));
            stubMapper();

            ArticleCommentDTO result =
                    commentService.addComment(ARTICLE_ID, "  Merci pour l'info  ", authenticationWith());

            ArgumentCaptor<ArticleComment> captor = ArgumentCaptor.forClass(ArticleComment.class);
            verify(commentRepository).save(captor.capture());
            ArticleComment saved = captor.getValue();

            assertEquals("Merci pour l'info", saved.getContent());
            assertSame(article, saved.getArticle());
            assertSame(author, saved.getAuthor());
            assertTrue(result.isCanDelete(), "the author can always delete what they just posted");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the article does not exist")
        void throwsWhenArticleMissing() {
            when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.addComment(ARTICLE_ID, "Bonjour", authenticationWith()));
            verify(commentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("lets the author delete their own comment")
        void authorCanDeleteOwnComment() {
            ArticleComment own = comment(10L, author, article);
            when(commentRepository.findById(10L)).thenReturn(Optional.of(own));
            when(userService.getCurrentUser(any())).thenReturn(author);

            commentService.deleteComment(ARTICLE_ID, 10L, authenticationWith());

            verify(commentRepository).delete(own);
        }

        @Test
        @DisplayName("lets a super admin delete someone else's comment")
        void superAdminCanDeleteAnyComment() {
            ArticleComment other = comment(11L, someoneElse, article);
            when(commentRepository.findById(11L)).thenReturn(Optional.of(other));
            when(userService.getCurrentUser(any())).thenReturn(author);

            commentService.deleteComment(ARTICLE_ID, 11L, authenticationWith("ROLE_SUPER_ADMIN"));

            verify(commentRepository).delete(other);
        }

        @Test
        @DisplayName("refuses to delete another employee's comment")
        void plainEmployeeCannotDeleteAnotherComment() {
            ArticleComment other = comment(11L, someoneElse, article);
            when(commentRepository.findById(11L)).thenReturn(Optional.of(other));
            when(userService.getCurrentUser(any())).thenReturn(author);

            assertThrows(AccessDeniedException.class,
                    () -> commentService.deleteComment(ARTICLE_ID, 11L, authenticationWith("ROLE_HR_MANAGER")));
            verify(commentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("refuses ROLE_ADMIN: moderation is reserved to super admins")
        void plainAdminIsNotAModerator() {
            ArticleComment other = comment(11L, someoneElse, article);
            when(commentRepository.findById(11L)).thenReturn(Optional.of(other));
            when(userService.getCurrentUser(any())).thenReturn(author);

            assertThrows(AccessDeniedException.class,
                    () -> commentService.deleteComment(ARTICLE_ID, 11L, authenticationWith("ROLE_ADMIN")));
            verify(commentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the comment belongs to another article")
        void rejectsCommentFromAnotherArticle() {
            Article otherArticle = new Article();
            otherArticle.setId(99L);
            ArticleComment elsewhere = comment(12L, author, otherArticle);
            when(commentRepository.findById(12L)).thenReturn(Optional.of(elsewhere));

            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.deleteComment(ARTICLE_ID, 12L, authenticationWith("ROLE_SUPER_ADMIN")));
            verify(commentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for an unknown comment id")
        void throwsWhenCommentMissing() {
            when(commentRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> commentService.deleteComment(ARTICLE_ID, 404L, authenticationWith()));
        }
    }
}
