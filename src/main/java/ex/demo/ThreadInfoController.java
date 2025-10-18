package ex.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/thread-info")
public class ThreadInfoController {

    @GetMapping
    public String getThreadInfo() {
        Thread currentThread = Thread.currentThread();

        String threadType = currentThread.isVirtual() ? "🚀 Virtual Thread" : "🐌 Platform Thread";
        String threadName = currentThread.getName();

        return String.format("""
            Thread Type: %s
            Thread Name: %s
            Thread ID: %d
            Thread Class: %s
            """,
            threadType,
            threadName,
            currentThread.threadId(),
            currentThread.getClass().getName()
        );
    }
}
