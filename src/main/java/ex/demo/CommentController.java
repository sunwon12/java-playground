package ex.demo;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDto> createComment(
            @Valid @RequestBody CommentCreateRequest request) {
        CommentDto comment = commentService.createComment(request);
        return ResponseEntity.ok(comment);
    }

    // 커서 기반 페이지네이션: lastPath 파라미터 사용
    @GetMapping
    public ResponseEntity<List<CommentDto>> getComments(
            @RequestParam Long postId,
            @RequestParam(required = false) String lastPath,
            @RequestParam(defaultValue = "10") int size) {
        List<CommentDto> comments = commentService.getComments(postId, lastPath, size);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCommentCount(@RequestParam Long postId) {
        long count = commentService.getCommentCount(postId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
