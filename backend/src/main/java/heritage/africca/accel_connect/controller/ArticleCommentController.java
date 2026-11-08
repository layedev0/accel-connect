package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.ArticleCommentDTO;
import heritage.africca.accel_connect.dto.ArticleCommentRequest;
import heritage.africca.accel_connect.service.ArticleCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/articles/{articleId}/comments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Article comments", description = "Endpoints for reading and posting comments on an article")
public class ArticleCommentController {

    private final ArticleCommentService commentService;

    @GetMapping
    @Operation(summary = "List an article's comments, newest first")
    public ResponseEntity<List<ArticleCommentDTO>> list(@PathVariable Long articleId,
                                                        Authentication authentication) {
        return ResponseEntity.ok(commentService.getComments(articleId, authentication));
    }

    @PostMapping
    @Operation(summary = "Post a comment on an article")
    public ResponseEntity<ArticleCommentDTO> add(@PathVariable Long articleId,
                                                 @Valid @RequestBody ArticleCommentRequest request,
                                                 Authentication authentication) {
        ArticleCommentDTO created = commentService.addComment(articleId, request.getContent(), authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment (its author, or a moderator)")
    public ResponseEntity<Void> delete(@PathVariable Long articleId,
                                       @PathVariable Long commentId,
                                       Authentication authentication) {
        commentService.deleteComment(articleId, commentId, authentication);
        return ResponseEntity.noContent().build();
    }
}
