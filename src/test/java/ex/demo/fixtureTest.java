package ex.demo;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class fixtureTest extends Ab {

    @Test
    void test1() {
        Board board = fixtureBoard(Map.of());
    }
}
