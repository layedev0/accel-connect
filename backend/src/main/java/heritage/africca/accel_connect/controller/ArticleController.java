package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.ArticleDTO;
import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.entity.enumeration.ArticleType;
import heritage.africca.accel_connect.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@Tag(name = "Articles", description = "Endpoints for managing news articles and their documents")
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    @Operation(summary = "List all articles (metadata only)")
    public ResponseEntity<List<ArticleDTO>> list() {
        return ResponseEntity.ok(articleService.getAllArticles());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single article (metadata only)")
    public ResponseEntity<ArticleDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create an article with an optional uploaded document (and optional cover image)")
    public ResponseEntity<ArticleDTO> create(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("articleType") ArticleType articleType,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            Authentication authentication) {
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setArticleType(articleType);
        ArticleDTO created = articleService.createArticle(dto, file, coverImage, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/document")
    @Operation(summary = "Download/view an article's document")
    public ResponseEntity<byte[]> document(@PathVariable Long id) {
        Article article = articleService.getArticleWithDocument(id);
        if (article.getDocumentData() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(article.getDocumentType()));
        headers.setContentDisposition(
                ContentDisposition.inline().filename(article.getDocumentName()).build()
        );
        return ResponseEntity.ok().headers(headers).body(article.getDocumentData());
    }

    @GetMapping("/{id}/cover")
    @Operation(summary = "View an article's cover image")
    public ResponseEntity<byte[]> cover(@PathVariable Long id) {
        Article article = articleService.getArticleWithCover(id);
        if (article.getCoverImageData() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(article.getCoverImageType()));
        headers.setContentDisposition(ContentDisposition.inline().build());
        return ResponseEntity.ok().headers(headers).body(article.getCoverImageData());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update an article (document/cover replaced only when supplied)")
    public ResponseEntity<ArticleDTO> update(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("articleType") ArticleType articleType,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage) {
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setArticleType(articleType);
        return ResponseEntity.ok(articleService.updateArticle(id, dto, file, coverImage));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete an article")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}
