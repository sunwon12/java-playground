package ex.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long postId;
    private Long parentId;
    private int depth;
    private String content;
    private LocalDateTime createdAt;

    // Materialized Path
    @Column(nullable = false, length = 255)
    private String path;

    public Comment(Long postId, Long parentId, int depth, String content, String parentPath, int position) {
        this.postId = postId;
        this.parentId = parentId;
        this.depth = depth;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.path = (parentPath == null ? "" : parentPath + "/") + String.format("%05d", position);
    }
}
