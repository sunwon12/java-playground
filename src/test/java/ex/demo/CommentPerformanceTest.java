package ex.demo;

import static org.assertj.core.api.Assertions.assertThat;


import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentPerformanceTest {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CommentService commentService;
    @Autowired
    private TestDataGenerator testDataGenerator;

    private static Long testPostId;
    private static final int TOTAL_COMMENTS = 1_000_000;
    private static final int PAGE_SIZE = 10;
    private static final long MAX_RESPONSE_TIME_MS = 300;

    @Test
    @Order(1)
    @DisplayName("1단계: 게시글 생성")
    void createPost() {
        Post post = new Post("테스트 게시글", "100만 댓글 성능 테스트용 게시글");
        Post savedPost = postRepository.save(post);
        testPostId = savedPost.getId();

        log.info("게시글 생성 완료: ID = {}", testPostId);
        assertThat(testPostId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("2단계: 100만건 댓글 데이터 생성")
    void generateTestData() {
        assertThat(testPostId).isNotNull();

        long startTime = System.currentTimeMillis();
        testDataGenerator.generateTestData(testPostId, TOTAL_COMMENTS);
        long endTime = System.currentTimeMillis();

        long count = commentRepository.countByPostId(testPostId);
        log.info("생성된 댓글 개수: {} (소요 시간: {}ms)", count, endTime - startTime);

        assertThat(count).isEqualTo(TOTAL_COMMENTS);
    }

    @Test
    @Order(3)
    @DisplayName("3단계: 첫 페이지 조회 성능 테스트 (목표: 300ms 이내)")
    void testFirstPagePerformance() {
        assertThat(testPostId).isNotNull();

        // 워밍업
        commentService.getComments(testPostId, null, PAGE_SIZE);

        long startTime = System.currentTimeMillis();
        List<CommentDto> firstPage = commentService.getComments(testPostId, null, PAGE_SIZE);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        log.info("=== 첫 페이지 조회 성능 ===");
        log.info("응답 시간: {}ms (목표: {}ms 이내)", responseTime, MAX_RESPONSE_TIME_MS);
        log.info("조회된 댓글 수: {}", firstPage.size());

        // 계층 구조 출력
        log.info("\n=== 첫 페이지 댓글 구조 (DFS 순서) ===");
        firstPage.forEach(comment -> {
            String indent = "  ".repeat(comment.getDepth());
            log.info("{}댓글 {} (depth={}, path={}): {}",
                    indent, comment.getId(), comment.getDepth(),
                    comment.getPath(), comment.getContent());
        });

        assertThat(responseTime).isLessThanOrEqualTo(MAX_RESPONSE_TIME_MS);
        assertThat(firstPage).hasSize(PAGE_SIZE);
    }

    @Test
    @Order(4)
    @DisplayName("4단계: 중간 페이지 조회 성능 테스트")
    void testMiddlePagePerformance() {
        assertThat(testPostId).isNotNull();

        // 중간 커서 구하기 (예: 50만번째 댓글의 path)
        String lastPath = getCursorAtIndex(testPostId, 500_000);

        long startTime = System.currentTimeMillis();
        List<CommentDto> page = commentService.getComments(testPostId, lastPath, PAGE_SIZE);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        log.info("=== 중간 페이지 조회 성능 (커서: {}) ===", lastPath);
        log.info("응답 시간: {}ms (목표: {}ms 이내)", responseTime, MAX_RESPONSE_TIME_MS);
        log.info("조회된 댓글 수: {}", page.size());

        assertThat(responseTime).isLessThanOrEqualTo(MAX_RESPONSE_TIME_MS);
        assertThat(page).hasSize(PAGE_SIZE);
    }

    @Test
    @Order(5)
    @DisplayName("5단계: 마지막 페이지 조회 성능 테스트")
    void testLastPagePerformance() {
        assertThat(testPostId).isNotNull();

        // 마지막 커서 구하기 (999,990번째 댓글의 path)
        String lastPath = getCursorAtIndex(testPostId, TOTAL_COMMENTS - PAGE_SIZE);

        long startTime = System.currentTimeMillis();
        List<CommentDto> page = commentService.getComments(testPostId, lastPath, PAGE_SIZE);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        log.info("=== 마지막 페이지 조회 성능 (커서: {}) ===", lastPath);
        log.info("응답 시간: {}ms (목표: {}ms 이내)", responseTime, MAX_RESPONSE_TIME_MS);
        log.info("조회된 댓글 수: {}", page.size());

        // 계층 구조 출력
        log.info("\n=== 첫 페이지 댓글 구조 (DFS 순서) ===");
        page.forEach(comment -> {
            String indent = "  ".repeat(comment.getDepth());
            log.info("{}댓글 {} (depth={}, path={}): {}",
                    indent, comment.getId(), comment.getDepth(),
                    comment.getPath(), comment.getContent());
        });

        assertThat(responseTime).isLessThanOrEqualTo(MAX_RESPONSE_TIME_MS);
        assertThat(page).isNotEmpty();
    }

    @Test
    @Order(6)
    @DisplayName("7단계: DFS 순서 검증 (path 오름차순)")
    void testDfsOrder() {
        assertThat(testPostId).isNotNull();

        List<CommentDto> page = commentService.getComments(testPostId, null, 100);

        String prevPath = null;
        for (CommentDto comment : page) {
            if (prevPath != null) {
                assertThat(comment.getPath()).isGreaterThan(prevPath);
            }
            prevPath = comment.getPath();
        }

        log.info("DFS 순서 검증 완료: path 값이 올바르게 오름차순으로 정렬됨");
    }

    // 커서 위치의 path를 구하는 유틸 (테스트용)
    private String getCursorAtIndex(Long postId, int index) {
        // 실제 환경에서는 인덱스 기반 커서 구하는 쿼리를 최적화 필요
        List<Comment> comments = commentRepository.findByPostIdOrderByPathAsc(postId, org.springframework.data.domain.PageRequest.of(index / PAGE_SIZE, PAGE_SIZE));
        if (comments.isEmpty()) return null;
        return comments.get(0).getPath();
    }
}
