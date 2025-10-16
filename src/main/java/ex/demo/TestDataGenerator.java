package ex.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestDataGenerator {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 100만건 테스트 데이터 생성 (Materialized Path, 최대 depth 50)
     */
    public void generateTestData(Long postId, int totalCount) {
        int rootCount = Math.max(1, totalCount / 100); // 예: 1만개 루트
        int maxDepth = 50;
        int positionPadding = 5;

        // 각 depth별로 부모 후보를 관리
        List<CommentNode> currentLevel = new ArrayList<>();
        List<CommentNode> nextLevel = new ArrayList<>();

        Random random = new Random(42);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // 1. 루트 댓글 생성 (batch insert with returning IDs)
        log.info("루트 댓글 {} 개 생성 시작", rootCount);
        List<Object[]> rootBatch = new ArrayList<>();
        for (int i = 0; i < rootCount; i++) {
            String path = String.format("%0" + positionPadding + "d", i + 1);
            rootBatch.add(new Object[]{postId, null, 0, "Root " + (i + 1), path, now});
        }

        // 루트 댓글 insert 후 ID 받아오기
        String insertSql = "INSERT INTO comments (post_id, parent_id, depth, content, path, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        int batchSize = 1000;

        for (int i = 0; i < rootBatch.size(); i += batchSize) {
            int end = Math.min(i + batchSize, rootBatch.size());
            List<Object[]> batch = rootBatch.subList(i, end);

            for (Object[] args : batch) {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, (Long) args[0]);
                    ps.setObject(2, args[1]);
                    ps.setInt(3, (Integer) args[2]);
                    ps.setString(4, (String) args[3]);
                    ps.setString(5, (String) args[4]);
                    ps.setTimestamp(6, (Timestamp) args[5]);
                    return ps;
                }, keyHolder);

                Long id = keyHolder.getKey().longValue();
                currentLevel.add(new CommentNode(id, (String) args[4], 0));
            }

            log.info("루트 댓글 {} ~ {} 저장 완료", i + 1, end);
        }

        int created = rootCount;

        // 2. 나머지 댓글 생성 (깊이 50까지)
        int currentDepth = 1;
        while (created < totalCount && !currentLevel.isEmpty() && currentDepth <= maxDepth) {
            log.info("Depth {} 생성 중... (현재 생성: {} / {})", currentDepth, created, totalCount);

            List<Object[]> childBatch = new ArrayList<>();
            Map<Integer, CommentNode> batchIndexToNode = new HashMap<>();
            int batchIndex = 0;

            for (CommentNode parent : currentLevel) {
                if (created >= totalCount) break;

                // 자식 개수 랜덤(1~5개로 증가하여 더 빠르게 100만개 도달)
                int childCount = 1 + random.nextInt(5);
                for (int c = 0; c < childCount && created < totalCount; c++) {
                    int childPos = c + 1;
                    String childPath = parent.path + "/" + String.format("%0" + positionPadding + "d", childPos);

                    childBatch.add(new Object[]{
                            postId,
                            parent.id,
                            parent.depth + 1,
                            "Comment " + (created + 1),
                            childPath,
                            now
                    });

                    batchIndexToNode.put(batchIndex++, new CommentNode(null, childPath, parent.depth + 1));
                    created++;
                }
            }

            // Batch insert with ID retrieval
            batchIndex = 0;
            for (Object[] args : childBatch) {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, (Long) args[0]);
                    ps.setLong(2, (Long) args[1]);
                    ps.setInt(3, (Integer) args[2]);
                    ps.setString(4, (String) args[3]);
                    ps.setString(5, (String) args[4]);
                    ps.setTimestamp(6, (Timestamp) args[5]);
                    return ps;
                }, keyHolder);

                Long id = keyHolder.getKey().longValue();
                CommentNode node = batchIndexToNode.get(batchIndex);
                nextLevel.add(new CommentNode(id, node.path, node.depth));
                batchIndex++;

                if (batchIndex % 10000 == 0) {
                    log.info("댓글 {} 개 저장 완료", created);
                }
            }

            // 다음 레벨로 이동
            currentLevel = new ArrayList<>(nextLevel);
            nextLevel.clear();
            currentDepth++;
        }

        log.info("총 {} 개 댓글 생성 완료!", created);
    }

    // 내부 노드 구조체
    private static class CommentNode {
        final Long id;
        final String path;
        final int depth;

        CommentNode(Long id, String path, int depth) {
            this.id = id;
            this.path = path;
            this.depth = depth;
        }
    }
}
