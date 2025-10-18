package ex.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 순수하게 Thread.sleep()으로 I/O 대기를 시뮬레이션하는 컨트롤러
 * DB나 비즈니스 로직 없이 가상 스레드 vs 플랫폼 스레드 성능만 측정
 */
@RestController
@RequestMapping("/api/sleep-test")
@Slf4j
public class SleepTestController {

    /**
     * 100ms sleep으로 I/O 대기 시뮬레이션
     * - Platform Thread: 100ms 동안 OS 스레드 블로킹
     * - Virtual Thread: carrier thread에서 unmount되어 다른 작업 처리
     */
    @GetMapping
    public String sleepTest() {
        try {
            Thread.sleep(100);  // I/O 대기 시뮬레이션
            return "OK";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted", e);
            return "ERROR";
        }
    }
}
