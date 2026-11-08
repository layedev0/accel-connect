package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.ArticlePhotoDTO;
import heritage.africca.accel_connect.entity.ArticlePhoto;
import heritage.africca.accel_connect.service.ArticlePhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/articles/{articleId}/photos")
@RequiredArgsConstructor
@Tag(name = "Article photos", description = "Endpoints for an article's photo gallery")
public class ArticlePhotoController {

    private final ArticlePhotoService photoService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List a gallery's photo metadata, in display order (no image bytes)")
    public ResponseEntity<List<ArticlePhotoDTO>> list(@PathVariable Long articleId) {
        return ResponseEntity.ok(photoService.getPhotos(articleId));
    }

    @GetMapping("/{photoId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Stream one photo's bytes")
    public ResponseEntity<byte[]> photo(@PathVariable Long articleId, @PathVariable Long photoId) {
        ArticlePhoto photo = photoService.getPhotoWithData(articleId, photoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(photo.getContentType()));
        headers.setContentDisposition(ContentDisposition.inline().build());
        return ResponseEntity.ok().headers(headers).body(photo.getData());
    }

    /** One photo per request: max-request-size is 10 MB for the whole multipart request. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Add one photo to an article's gallery")
    public ResponseEntity<ArticlePhotoDTO> add(@PathVariable Long articleId,
                                               @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(photoService.addPhoto(articleId, file));
    }

    @DeleteMapping("/{photoId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Remove one photo from an article's gallery")
    public ResponseEntity<Void> delete(@PathVariable Long articleId, @PathVariable Long photoId) {
        photoService.deletePhoto(articleId, photoId);
        return ResponseEntity.noContent().build();
    }
}
