package ex.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-data")
@RequiredArgsConstructor
public class TestDataController {

    private final TestDataGenerator testDataGenerator;

    @PostMapping("/generate")
    public ResponseEntity<String> generateTestData(
            @RequestParam(value = "postId", defaultValue = "1") Long postId,
            @RequestParam(value = "count", defaultValue = "10000") int count) {
        testDataGenerator.generateTestData(postId, count);
        return ResponseEntity.ok("Successfully generated " + count + " comments for post " + postId);
    }
}
