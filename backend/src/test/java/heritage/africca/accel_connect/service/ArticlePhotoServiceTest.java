package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.ArticlePhotoDTO;
import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.entity.ArticlePhoto;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.ArticlePhotoRepository;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticlePhotoService")
class ArticlePhotoServiceTest {

    private static final Long ARTICLE_ID = 7L;

    @Mock private ArticlePhotoRepository photoRepository;
    @Mock private ArticleRepository articleRepository;

    @InjectMocks
    private ArticlePhotoService photoService;

    private Article article;

    @BeforeEach
    void setUp() {
        article = new Article();
        article.setId(ARTICLE_ID);
    }

    private static MockMultipartFile jpeg(String name, int sizeBytes) {
        return new MockMultipartFile("file", name, "image/jpeg", new byte[sizeBytes]);
    }

    private void articleExists() {
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));
    }

    @Nested
    @DisplayName("addPhoto")
    class AddPhoto {

        @Test
        @DisplayName("stores the bytes, metadata and the next position")
        void storesPhoto() {
            articleExists();
            when(photoRepository.countByArticleId(ARTICLE_ID)).thenReturn(2L);
            when(photoRepository.save(any(ArticlePhoto.class))).thenAnswer(inv -> inv.getArgument(0));

            ArticlePhotoDTO dto = photoService.addPhoto(ARTICLE_ID, jpeg("plage.jpg", 4));

            ArgumentCaptor<ArticlePhoto> captor = ArgumentCaptor.forClass(ArticlePhoto.class);
            verify(photoRepository).save(captor.capture());
            ArticlePhoto saved = captor.getValue();

            assertSame(article, saved.getArticle());
            assertEquals(4, saved.getData().length);
            assertEquals("image/jpeg", saved.getContentType());
            assertEquals("plage.jpg", saved.getFileName());
            assertEquals(4L, saved.getSize());
            assertEquals(3, saved.getPosition(), "position continues after the existing photos");
            assertEquals(3, dto.position());
        }

        @Test
        @DisplayName("gives the first photo position 1")
        void firstPhotoStartsAtOne() {
            articleExists();
            when(photoRepository.countByArticleId(ARTICLE_ID)).thenReturn(0L);
            when(photoRepository.save(any(ArticlePhoto.class))).thenAnswer(inv -> inv.getArgument(0));

            assertEquals(1, photoService.addPhoto(ARTICLE_ID, jpeg("a.jpg", 1)).position());
        }

        @Test
        @DisplayName("refuses an eleventh photo")
        void refusesBeyondTheCap() {
            articleExists();
            when(photoRepository.countByArticleId(ARTICLE_ID)).thenReturn(10L);

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> photoService.addPhoto(ARTICLE_ID, jpeg("trop.jpg", 1)));

            assertTrue(error.getMessage().contains("10"));
            verify(photoRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses a file above 10 MB")
        void refusesOversizedFile() {
            articleExists();
            when(photoRepository.countByArticleId(ARTICLE_ID)).thenReturn(0L);

            assertThrows(IllegalArgumentException.class,
                    () -> photoService.addPhoto(ARTICLE_ID, jpeg("enorme.jpg", 10 * 1024 * 1024 + 1)));
            verify(photoRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses a content type other than JPEG or PNG")
        void refusesDisallowedType() {
            articleExists();
            when(photoRepository.countByArticleId(ARTICLE_ID)).thenReturn(0L);

            MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

            assertThrows(IllegalArgumentException.class, () -> photoService.addPhoto(ARTICLE_ID, pdf));
            verify(photoRepository, never()).save(any());
        }

        @Test
        @DisplayName("accepts PNG")
        void acceptsPng() {
            articleExists();
            when(photoRepository.countByArticleId(ARTICLE_ID)).thenReturn(0L);
            when(photoRepository.save(any(ArticlePhoto.class))).thenAnswer(inv -> inv.getArgument(0));

            MockMultipartFile png = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1});

            assertDoesNotThrow(() -> photoService.addPhoto(ARTICLE_ID, png));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the article does not exist")
        void throwsWhenArticleMissing() {
            when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> photoService.addPhoto(ARTICLE_ID, jpeg("a.jpg", 1)));
        }
    }

    @Nested
    @DisplayName("deletePhoto")
    class DeletePhoto {

        @Test
        @DisplayName("removes a photo that belongs to the article")
        void removesOwnPhoto() {
            ArticlePhoto photo = new ArticlePhoto();
            photo.setId(3L);
            when(photoRepository.findByIdAndArticleId(3L, ARTICLE_ID)).thenReturn(Optional.of(photo));

            photoService.deletePhoto(ARTICLE_ID, 3L);

            verify(photoRepository).delete(photo);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for a photo of another article")
        void rejectsPhotoOfAnotherArticle() {
            when(photoRepository.findByIdAndArticleId(3L, ARTICLE_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> photoService.deletePhoto(ARTICLE_ID, 3L));
            verify(photoRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("getPhotos")
    class GetPhotos {

        @Test
        @DisplayName("throws ResourceNotFoundException when the article does not exist")
        void throwsWhenArticleMissing() {
            when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> photoService.getPhotos(ARTICLE_ID));
        }
    }
}
