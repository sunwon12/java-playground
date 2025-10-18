# Virtual Threads Performance Test 🚀

Java 21의 가상 스레드(Virtual Threads)와 전통적인 플랫폼 스레드(Platform Threads)의 성능을 실제로 비교하는 프로젝트입니다.

## 핵심 포인트

**코드는 완전히 동일하고, 설정 하나만 바꿔서 테스트합니다!**

```yaml
# application-virtual.yml
spring:
  threads:
    virtual:
      enabled: true  # 이것만으로 끝!
```

## 빠른 시작

### 1. 일반 플랫폼 스레드 테스트

```bash
# 애플리케이션 실행
./gradlew bootRun

# 엔드포인트 동작 확인 (다른 터미널)
curl http://localhost:8080/api/sleep-test
# 출력: OK (100ms 소요)

# JMeter로 성능 테스트
jmeter
# File → Open → jmeter/Spring-MVC-Performance-Test.jmx
# Run → Start

# 결과 확인 후 종료
Ctrl+C
```

### 2. 가상 스레드 테스트 ⚡

```bash
# 가상 스레드로 실행
./gradlew bootRun --args='--spring.profiles.active=virtual'

# 동일한 JMeter 테스트 실행

# 결과 비교!
```

### 3. 스레드 타입 확인

```bash
# 플랫폼 스레드
curl http://localhost:8080/api/thread-info
# 출력: Thread Type: 🐌 Platform Thread

# 가상 스레드
curl http://localhost:8080/api/thread-info
# 출력: Thread Type: 🚀 Virtual Thread
```

## 주요 기능

### 순수 I/O 지연 시뮬레이션 ⏱️

`Thread.sleep(100)`으로 실제 운영 환경의 I/O 대기 시간을 시뮬레이션합니다:

- **시뮬레이션 시간**: 고정 100ms
- **테스트 대상**: `/api/sleep-test` 엔드포인트
- **DB 의존성**: 없음 (순수 스레드 성능 테스트)

이 지연 시간 동안:
- **플랫폼 스레드**: 100ms 동안 OS 스레드가 블로킹되어 낭비됨 😴
- **가상 스레드**: carrier thread에서 unmount되어 다른 작업 처리 🚀

## 예상 결과

| 지표 | 플랫폼 스레드 | 가상 스레드 | 차이 |
|------|-------------|------------|------|
| **Throughput** | ~500 req/sec | ~5,000 req/sec | **10배 향상** 🚀 |
| **Response Time (Avg)** | ~500ms | ~100ms | **5배 빠름** ⚡ |
| **P95 Response Time** | ~1,000ms | ~110ms | **9배 빠름** |
| **Error Rate** | 0~5% | 0% | **안정성 향상** ✅ |
| **Platform Thread Count** | 200 (고갈) | 8~16 (CPU 코어 수) | **리소스 절약** 💾 |
| **Virtual Thread Count** | - | 5,000+ | **무제한 동시성** ♾️ |

## 기술 스택

- Java 21
- Spring Boot 3.4.4
- Spring MVC (동일한 코드)
- MySQL 8.0 (선택 사항, 테스트에 미사용)
- Apache JMeter
- Lombok

## 프로젝트 구조

```
├── src/main/java/ex/demo/
│   ├── SleepTestController.java         # Thread.sleep(100) 테스트 API ⏱️
│   ├── ThreadInfoController.java        # 스레드 타입 확인
│   ├── Comment*.java                    # 댓글 기능 (참고용)
│   └── SimulatedDelayService.java       # 지연 서비스 (참고용)
├── src/main/resources/
│   ├── application.yml                  # 플랫폼 스레드 (기본)
│   └── application-virtual.yml          # 가상 스레드
├── jmeter/
│   └── Spring-MVC-Performance-Test.jmx  # JMeter 테스트 플랜
└── docs/
    ├── performance-test.md              # 상세 가이드
    └── test-results-template.md         # 결과 기록 템플릿
```

## 테스트 시나리오

- **동시 사용자**: 500명
- **램프업 시간**: 30초
- **반복 횟수**: 10회
- **Think Time**: 1초
- **총 요청 수**: 5,000회
- **각 요청 처리 시간**: 100ms (Thread.sleep)

## 가상 스레드란?

**Java 21의 혁신적인 기능**으로, 수백만 개의 경량 스레드를 만들 수 있습니다.

### 전통적인 플랫폼 스레드

```
요청 1 → OS 스레드 1 (1MB 메모리) → DB 대기 중 😴
요청 2 → OS 스레드 2 (1MB 메모리) → DB 대기 중 😴
...
요청 200 → OS 스레드 200 (1MB 메모리) → DB 대기 중 😴
요청 201 → ❌ 스레드 풀 고갈! 대기...
```

### 가상 스레드

```
요청 1 → 가상 스레드 1 → DB 대기 시 OS 스레드 반납
                      → OS 스레드는 다른 가상 스레드 처리 🚀
요청 2 → 가상 스레드 2 → DB 대기 시 OS 스레드 반납
...
요청 100만 → 가상 스레드 100만 → 모두 처리 가능! ✅
```

## 가상 스레드의 장점

1. **높은 처리량**: I/O 대기 시간을 낭비하지 않음
2. **낮은 리소스 사용**: OS 스레드 10~20개로 수만 개의 요청 처리
3. **코드 변경 불필요**: 기존 블로킹 코드 그대로 사용
4. **학습 비용 낮음**: Reactive 프로그래밍 불필요
5. **안정성 향상**: 스레드 풀 고갈 없음

## 언제 사용해야 하는가?

### ✅ 가상 스레드 사용 추천

- **I/O Bound 작업**: DB 쿼리, 외부 API 호출, 파일 I/O
- **높은 동시성**: 마이크로서비스, 실시간 채팅, 알림 시스템
- **블로킹 코드 유지**: 기존 코드 마이그레이션 부담 최소화

### ❌ 가상 스레드 효과 없음

- **CPU Bound 작업**: 복잡한 계산, 암호화, 이미지 처리
- **낮은 동시성**: 동시 요청이 적을 때 (< 100)
- **메모리 Bound**: 대용량 데이터 처리

## 주의사항

### Synchronized 블록 피하기

```java
// ❌ 가상 스레드에서 피해야 할 코드
synchronized (lock) {
    // I/O 작업
    db.query();
}

// ✅ 대신 이렇게
Lock lock = new ReentrantLock();
lock.lock();
try {
    db.query();
} finally {
    lock.unlock();
}
```

## 참고 자료

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Boot 3.2+ Virtual Threads](https://spring.io/blog/2023/09/09/all-together-now-spring-boot-3-2-graalvm-native-images-java-21-and-virtual)
- [상세 테스트 가이드](docs/performance-test.md)

## 라이선스

MIT License
