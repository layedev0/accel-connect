package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.ArticleLikeStatusDTO;
import heritage.africca.accel_connect.service.ArticleLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/articles/{articleId}/like")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Article likes", description = "Endpoints for liking and unliking an article")
public class ArticleLikeController {

    private final ArticleLikeService likeService;

    @GetMapping
    @Operation(summary = "Get the like count and whether the caller liked this article")
    public ResponseEntity<ArticleLikeStatusDTO> status(@PathVariable Long articleId,
                                                       Authentication authentication) {
        return ResponseEntity.ok(likeService.getStatus(articleId, authentication));
    }

    @PostMapping
    @Operation(summary = "Like an article (idempotent)")
    public ResponseEntity<ArticleLikeStatusDTO> like(@PathVariable Long articleId,
                                                     Authentication authentication) {
        return ResponseEntity.ok(likeService.like(articleId, authentication));
    }

    @DeleteMapping
    @Operation(summary = "Remove the caller's like (idempotent)")
    public ResponseEntity<ArticleLikeStatusDTO> unlike(@PathVariable Long articleId,
                                                       Authentication authentication) {
        return ResponseEntity.ok(likeService.unlike(articleId, authentication));
    }
}
