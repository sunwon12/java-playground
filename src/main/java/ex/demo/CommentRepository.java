package ex.demo;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 커서 기반 페이지네이션 (path 기준)
    List<Comment> findByPostIdAndPathGreaterThanOrderByPathAsc(Long postId, String lastPath, Pageable pageable);

    // 첫 페이지 조회 (lastPath 없이)
    List<Comment> findByPostIdOrderByPathAsc(Long postId, Pageable pageable);

    // 같은 부모 아래 형제들의 최대 position 조회 (동시성 처리)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT COALESCE(MAX(CAST(SUBSTRING(c.path, LENGTH(:prefix) + 2, 5) AS int)), 0)
            FROM Comment c
            WHERE c.postId = :postId
              AND (:prefix = '' AND c.parentId IS NULL
                   OR c.path LIKE CONCAT(:prefix, '/%'))
            """)
    int findMaxPositionByPostIdAndPrefixForUpdate(
            Long postId,
            String prefix
    );

    Optional<Comment> findByIdAndPostId(Long id, Long postId);

    long countByPostId(Long postId);

    // 서브트리 삭제용
    void deleteByPostIdAndPathStartingWith(Long postId, String pathPrefix);
}
