package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.ArticleDTO;
import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.enumeration.ArticleType;
import heritage.africca.accel_connect.mapper.ArticleMapper;
import heritage.africca.accel_connect.mapper.UserNameMapper;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.repository.ArticleCommentRepository;
import heritage.africca.accel_connect.repository.ArticleLikeRepository;
import heritage.africca.accel_connect.repository.ArticlePhotoRepository;
import heritage.africca.accel_connect.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleService")
class ArticleServiceTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private ArticleMapper articleMapper;
    @Mock private ArticleCommentRepository articleCommentRepository;
    @Mock private ArticleLikeRepository articleLikeRepository;
    @Mock private ArticlePhotoRepository articlePhotoRepository;
    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private UserNameMapper userNameMapper;

    @InjectMocks
    private ArticleService articleService;

    private ArticleDTO inputDto;
    private User currentUser;
    private User otherUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        inputDto = new ArticleDTO();
        inputDto.setTitle("Nouveau programme de formation");
        inputDto.setContent("Développez vos compétences");
        inputDto.setArticleType(ArticleType.FORMATION);

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("pape");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("aicha");

        authentication = new UsernamePasswordAuthenticationToken("principal", "credentials", List.of());
        // Lenient: the validation-failure tests reject the upload before any of this is reached.
        lenient().when(userService.getCurrentUser(any())).thenReturn(currentUser);
        lenient().when(userRepository.findAll()).thenReturn(List.of(currentUser, otherUser));
        lenient().when(userNameMapper.displayName(any())).thenReturn("Pape Ndiaye");

        ReflectionTestUtils.setField(articleService, "frontendUrl", "http://localhost:4200");
    }

    private MockMultipartFile pdfFile(int sizeBytes) {
        return new MockMultipartFile("file", "article.pdf", "application/pdf", new byte[sizeBytes]);
    }

    private MockMultipartFile fileOfType(String contentType) {
        return new MockMultipartFile("file", "doc", contentType, new byte[]{1});
    }

    @Nested
    @DisplayName("createArticle")
    class CreateArticle {

        @Test
        @DisplayName("persists the document bytes and metadata from the uploaded file")
        void persistsDocumentFromFile() {
            when(articleMapper.toEntity(any(ArticleDTO.class))).thenReturn(new Article());
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
            when(articleMapper.toDto(any(Article.class))).thenReturn(new ArticleDTO());

            MockMultipartFile file = new MockMultipartFile(
                    "file", "article.pdf", "application/pdf", new byte[]{1, 2, 3, 4});

            articleService.createArticle(inputDto, file, null, authentication);

            ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
            verify(articleRepository).save(captor.capture());
            Article saved = captor.getValue();

            assertArrayEquals(new byte[]{1, 2, 3, 4}, saved.getDocumentData());
            assertEquals("article.pdf", saved.getDocumentName());
            assertEquals("application/pdf", saved.getDocumentType());
            assertEquals(4L, saved.getDocumentSize());
        }

        @Test
        @DisplayName("stamps the caller as the article's author")
        void stampsCurrentUserAsAuthor() {
            when(articleMapper.toEntity(any(ArticleDTO.class))).thenReturn(new Article());
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
            when(articleMapper.toDto(any(Article.class))).thenReturn(new ArticleDTO());

            articleService.createArticle(inputDto, null, null, authentication);

            ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
            verify(articleRepository).save(captor.capture());
            assertSame(currentUser, captor.getValue().getAuthor());
        }

        @Test
        @DisplayName("notifies every employee, with a link to the new article")
        void notifiesEveryone() {
            Article persisted = new Article();
            persisted.setId(42L);
            persisted.setTitle("Programme Data & IA");
            when(articleMapper.toEntity(any(ArticleDTO.class))).thenReturn(persisted);
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
            when(articleMapper.toDto(any(Article.class))).thenReturn(new ArticleDTO());

            articleService.createArticle(inputDto, null, null, authentication);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<User>> recipients = ArgumentCaptor.forClass(Collection.class);
            ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
            verify(notificationService).createForUsers(
                    recipients.capture(), any(), message.capture(), link.capture());

            assertEquals(2, recipients.getValue().size(), "every employee is notified");
            assertTrue(message.getValue().contains("Programme Data & IA"));
            assertEquals("http://localhost:4200/actualites/42", link.getValue());
        }

        @Test
        @DisplayName("truncates the notification message to the 500-character column")
        void truncatesLongTitle() {
            Article persisted = new Article();
            persisted.setId(42L);
            persisted.setTitle("T".repeat(900));
            when(articleMapper.toEntity(any(ArticleDTO.class))).thenReturn(persisted);
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
            when(articleMapper.toDto(any(Article.class))).thenReturn(new ArticleDTO());

            articleService.createArticle(inputDto, null, null, authentication);

            ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
            verify(notificationService).createForUsers(any(), any(), message.capture(), any());

            assertTrue(message.getValue().length() <= 500,
                    "a longer message would be rejected by the database");
        }

        @Test
        @DisplayName("notifies nobody when the upload is rejected")
        void notifiesNobodyOnValidationFailure() {
            MockMultipartFile big = new MockMultipartFile(
                    "file", "big.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1]);

            assertThrows(IllegalArgumentException.class,
                    () -> articleService.createArticle(inputDto, big, null, authentication));

            verify(notificationService, never()).createForUsers(any(), any(), any(), any());
        }

        @Test
        @DisplayName("returns the mapped DTO on success")
        void returnsMappedDto() {
            ArticleDTO returned = new ArticleDTO();
            returned.setId(7L);
            when(articleMapper.toEntity(any(ArticleDTO.class))).thenReturn(new Article());
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
            when(articleMapper.toDto(any(Article.class))).thenReturn(returned);

            ArticleDTO result = articleService.createArticle(inputDto, pdfFile(1024), null, authentication);

            assertNotNull(result);
            assertEquals(7L, result.getId());
        }

        @Test
        @DisplayName("creates the article without a document when no file is supplied")
        void createsWithoutDocumentWhenFileNull() {
            when(articleMapper.toEntity(any(ArticleDTO.class))).thenReturn(new Article());
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
            when(articleMapper.toDto(any(Article.class))).thenReturn(new ArticleDTO());

            articleService.createArticle(inputDto, null, null, authentication);

            ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
            verify(articleRepository).save(captor.capture());
            Article saved = captor.getValue();

            assertNull(saved.getDocumentData());
            assertNull(saved.getDocumentName());
        }

        @Test
        @DisplayName("creates the article without a document when the file is empty")
        void createsWithoutDocumentWhenFileEmpty() {
            when(articleMapper.toEntity(any(ArticleDTO.class))).thenReturn(new Article());
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
            when(articleMapper.toDto(any(Article.class))).thenReturn(new ArticleDTO());

            MockMultipartFile empty = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

            assertDoesNotThrow(() -> articleService.createArticle(inputDto, empty, null, authentication));

            ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
            verify(articleRepository).save(captor.capture());
            assertNull(captor.getValue().getDocumentData());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when the file exceeds 10 MB")
        void throwsOnFileTooLarge() {
            MockMultipartFile big = new MockMultipartFile(
                    "file", "big.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1]);

            assertThrows(IllegalArgumentException.class,
                    () -> articleService.createArticle(inputDto, big, null, authentication));
            verify(articleRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException for a disallowed content type")
        void throwsOnInvalidContentType() {
            MockMultipartFile exe = new MockMultipartFile(
                    "file", "script.exe", "application/octet-stream", new byte[]{1});

            assertThrows(IllegalArgumentException.class,
                    () -> articleService.createArticle(inputDto, exe, null, authentication));
            verify(articleRepository, never()).save(any());
        }

        @Test
        @DisplayName("accepts JPEG and PNG documents")
        void acceptsImages() {
            when(articleMapper.toEntity(any(ArticleDTO.class))).thenReturn(new Article());
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
            when(articleMapper.toDto(any(Article.class))).thenReturn(new ArticleDTO());

            assertDoesNotThrow(() -> articleService.createArticle(inputDto, fileOfType("image/jpeg"), null, authentication));
            assertDoesNotThrow(() -> articleService.createArticle(inputDto, fileOfType("image/png"), null, authentication));
        }
    }

    @Nested
    @DisplayName("getAllArticles")
    class GetAllArticles {

        @Test
        @DisplayName("delegates to the ordering query")
        void usesTheOrderingQuery() {
            when(articleRepository.findAllNewestFirst()).thenReturn(List.of());

            articleService.getAllArticles();

            // The ordering itself lives in the repository's JPQL and is covered by
            // ArticleRepositoryTest, which actually runs the query.
            verify(articleRepository).findAllNewestFirst();
        }
    }

    @Nested
    @DisplayName("deleteArticle")
    class DeleteArticle {

        @Test
        @DisplayName("purges comments and likes before deleting the article")
        void purgesChildrenFirst() {
            when(articleRepository.existsById(5L)).thenReturn(true);

            articleService.deleteArticle(5L);

            // Order matters: the foreign keys have no ON DELETE CASCADE, so the children
            // must be gone before the article row is removed.
            InOrder inOrder = inOrder(
                    articleCommentRepository, articleLikeRepository, articlePhotoRepository, articleRepository);
            inOrder.verify(articleCommentRepository).deleteByArticleId(5L);
            inOrder.verify(articleLikeRepository).deleteByArticleId(5L);
            inOrder.verify(articlePhotoRepository).deleteByArticleId(5L);
            inOrder.verify(articleRepository).deleteById(5L);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException and touches nothing when the article is unknown")
        void throwsWhenArticleMissing() {
            when(articleRepository.existsById(404L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> articleService.deleteArticle(404L));

            verify(articleCommentRepository, never()).deleteByArticleId(any());
            verify(articleLikeRepository, never()).deleteByArticleId(any());
            verify(articlePhotoRepository, never()).deleteByArticleId(any());
            verify(articleRepository, never()).deleteById(any());
        }
    }
}
