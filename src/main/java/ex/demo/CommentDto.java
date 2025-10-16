package ex.demo;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class CommentDto {
    private Long id;
    private Long postId;
    private String content;
    private Long parentId;
    private int depth;
    private String path; // Materialized Path
    private LocalDateTime createdAt;
}

