package heritage.africca.accel_connect.dto;

/**
 * Metadata for one gallery photo. Intentionally excludes the binary blob so that listing
 * endpoints don't transfer image data unnecessarily — the bytes are served by
 * {@code GET /api/v1/articles/{articleId}/photos/{photoId}}.
 */
public record ArticlePhotoDTO(
        Long id,
        String fileName,
        String contentType,
        Long size,
        Integer position
) {
}
