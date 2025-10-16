package ex.demo;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private static final int MAX_DEPTH = 50;

    @Transactional
    public CommentDto createComment(CommentCreateRequest request) {
        if (request.getParentId() == null) {
            return addRootComment(request.getPostId(), request.getContent());
        } else {
            return addReply(request.getPostId(), request.getParentId(), request.getContent());
        }
    }

    @Transactional
    public CommentDto addRootComment(Long postId, String content) {
        String prefix = "";
        int maxPos = commentRepository.findMaxPositionByPostIdAndPrefixForUpdate(postId, prefix);
        int newPos = maxPos + 1;
        // 루트 댓글: parentId=null, parentPath=null
        Comment comment = new Comment(postId, null, 0, content, null, newPos);
        comment = commentRepository.save(comment);
        return toDto(comment);
    }

    @Transactional
    public CommentDto addReply(Long postId, Long parentId, String content) {
        Comment parent = commentRepository.findByIdAndPostId(parentId, postId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글 없음"));
        if (parent.getDepth() >= MAX_DEPTH) {
            throw new IllegalArgumentException("최대 깊이 초과");
        }
        String prefix = parent.getPath();
        int maxPos = commentRepository.findMaxPositionByPostIdAndPrefixForUpdate(postId, prefix);
        int newPos = maxPos + 1;
        Comment comment = new Comment(postId, parentId, parent.getDepth() + 1, content, prefix, newPos);
        comment = commentRepository.save(comment);
        return toDto(comment);
    }

    // 커서 기반 페이지네이션
    public List<CommentDto> getComments(Long postId, String lastPath, int size) {
        Pageable pageable = PageRequest.of(0, size);
        List<Comment> comments;
        if (lastPath == null || lastPath.isEmpty()) {
            comments = commentRepository.findByPostIdOrderByPathAsc(postId, pageable);
        } else {
            comments = commentRepository.findByPostIdAndPathGreaterThanOrderByPathAsc(postId, lastPath, pageable);
        }
        return comments.stream().map(this::toDto).collect(Collectors.toList());
    }

    public long getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment target = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));
        String prefix = target.getPath() + "/";
        commentRepository.deleteByPostIdAndPathStartingWith(target.getPostId(), prefix);
        commentRepository.delete(target);
    }

    private CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .depth(comment.getDepth())
                .createdAt(comment.getCreatedAt())
                .path(comment.getPath())
                .build();
    }
}
