package ex.demo;

import lombok.Getter;

@Getter
public class CommentCreateRequest {
    private Long postId;
    private String content;
    private Long parentId; // null이면 최상위 댓글
}

