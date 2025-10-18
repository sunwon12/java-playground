# 가상 스레드 (Virtual Threads) 성능 테스트

## 목적

Spring Boot 3.2+에서 도입된 가상 스레드(Virtual Threads)의 성능 효과를 실제로 측정하고 비교합니다.

## 테스트 대상

### 1. 일반 플랫폼 스레드 (Platform Threads)
- **전통적인 방식**: 요청당 1개의 OS 스레드 할당
- **특징**:
  - 스레드 생성/관리 비용이 높음
  - 스레드 풀 크기 제한 (일반적으로 200개)
  - I/O 대기 시 스레드가 블로킹되어 낭비됨

### 2. 가상 스레드 (Virtual Threads) ⚡
- **Java 21의 혁신**: 수백만 개의 경량 스레드 생성 가능
- **특징**:
  - 스레드 생성 비용이 거의 없음
  - I/O 대기 시 자동으로 다른 작업 처리
  - **동일한 블로킹 코드**를 사용하면서 논블로킹 수준의 성능

## 테스트 시나리오

### JMeter 성능 테스트 설정

- **사용자 수**: 500명 동시 사용자
- **램프업 시간**: 30초 (점진적으로 부하 증가)
- **반복 횟수**: 각 사용자당 10회 반복
- **Think Time**: 각 요청 후 1초 대기
- **총 요청 수**: 5,000회

### 테스트 API

**Sleep 테스트**: `GET /api/sleep-test`
- 단순히 `Thread.sleep(100)` 호출
- **I/O 지연**: 고정 100ms
- **응답**: "OK" 문자열
- **DB 의존성**: 없음 (순수 스레드 성능 측정)

### 테스트 데이터

**데이터 불필요** - 순수하게 스레드 블로킹 시간만 측정

### 🚨 I/O 지연 시뮬레이션

**왜 Thread.sleep()을 사용하는가?**

실제 운영 환경에서는 I/O 대기 시간이 발생합니다:
- DB 쿼리 응답: 50~150ms
- 외부 API 호출: 100~300ms
- 파일 I/O: 10~50ms

**시뮬레이션 구현:**
```java
// SleepTestController.java
@GetMapping
public String sleepTest() {
    Thread.sleep(100);  // I/O 대기 시뮬레이션
    return "OK";
}
```

**Thread.sleep()과 가상 스레드:**
- **Platform Thread**: 100ms 동안 OS 스레드가 블로킹됨 (다른 요청 처리 불가)
- **Virtual Thread**: carrier thread에서 unmount되어 다른 가상 스레드 처리 가능

이것이 바로 **가상 스레드의 핵심 장점**입니다! 🚀

## 측정 지표

### 1. 처리량 (Throughput) 🚀
- **요청 수/초**: 초당 처리 가능한 요청 수
- 높을수록 좋음
- **예상**: 가상 스레드가 2~5배 높을 것

### 2. 응답 시간 (Response Time) ⏱️
- **평균 (Average)**: 전체 요청의 평균 응답 시간
- **중앙값 (Median)**: 50% 요청의 응답 시간
- **90th, 95th, 99th Percentile**: 상위 사용자 경험
- 낮을수록 좋음
- **예상**: 가상 스레드가 더 낮고 안정적

### 3. 에러율 (Error %) ❌
- 실패한 요청 비율
- 낮을수록 좋음
- **예상**: 플랫폼 스레드는 스레드 풀 고갈로 에러 발생 가능

## JMeter 사용 방법

### 1. JMeter 설치

**macOS:**
```bash
brew install jmeter
```

**Windows/Linux:**
- [Apache JMeter 공식 사이트](https://jmeter.apache.org/download_jmeter.cgi)에서 다운로드

### 2. 테스트 실행 프로세스

#### 2-1. 일반 플랫폼 스레드 테스트

**Step 1**: Spring Boot 실행 (기본 설정)
```bash
./gradlew clean build -x test
./gradlew bootRun
```

**Step 2**: 엔드포인트 동작 확인
```bash
curl http://localhost:8080/api/sleep-test
# 출력: OK (약 100ms 소요)
```

**Step 3**: JMeter 실행
```bash
jmeter
```

**Step 4**: 테스트 플랜 열기
- `File` → `Open` → `jmeter/Spring-MVC-Performance-Test.jmx`

**Step 5**: 테스트 실행
- 초록색 ▶ 버튼 클릭
- Summary Report에서 결과 확인
- 스크린샷 저장 또는 CSV 저장

**Step 6**: 애플리케이션 종료
- Ctrl+C로 Spring Boot 종료

---

#### 2-2. 가상 스레드 테스트

**Step 1**: Spring Boot 실행 (가상 스레드 활성화)
```bash
./gradlew bootRun --args='--spring.profiles.active=virtual'
```

**Step 2**: 스레드 타입 확인
```bash
curl http://localhost:8080/api/thread-info
# 출력: Thread Type: 🚀 Virtual Thread
```

**Step 3**: 동일한 JMeter 테스트 실행
- 동일한 JMeter 파일로 테스트

**Step 4**: 결과 비교
- 일반 스레드 vs 가상 스레드 결과를 나란히 비교

### 3. 가상 스레드 활성화 확인

Spring Boot 실행 시 로그에서 확인:
```
Using virtual threads for embedded web server
```

또는 애플리케이션 실행 중:
```bash
curl http://localhost:8080/actuator/info
```

## 결과 비교 방법

### Summary Report에서 확인할 주요 지표

| 지표 | 일반 스레드 | 가상 스레드 | 개선율 |
|------|------------|------------|--------|
| **Throughput (req/sec)** | | | |
| **Average (ms)** | | | |
| **Median (ms)** | | | |
| **90% Line (ms)** | | | |
| **95% Line (ms)** | | | |
| **99% Line (ms)** | | | |
| **Error %** | | | |

### 비교 포인트

1. **Throughput (처리량)**
   - 가상 스레드가 2~5배 높을 것으로 예상
   - 동시성이 높을수록 차이가 더 크게 남

2. **Response Time (응답 시간)**
   - 가상 스레드가 더 낮고 안정적
   - 특히 95%, 99% percentile에서 큰 차이

3. **Error Rate (에러율)**
   - 플랫폼 스레드: 스레드 풀 고갈 시 에러 발생 가능
   - 가상 스레드: 거의 에러 없음

## 예상 결과

### 플랫폼 스레드의 한계 (500 동시 사용자, 100ms sleep)
```
✗ 스레드 풀 크기: 200개 (고정)
✗ 최대 이론 처리량: 200 threads / 0.1초 = 2,000 req/sec
✗ 실제 처리량: ~500 req/sec (컨텍스트 스위칭, 대기 등)
✗ 평균 응답 시간: ~500ms (대기 시간 포함)
✗ P95 응답 시간: ~1,000ms (심한 대기)
```

### 가상 스레드의 장점 (500 동시 사용자, 100ms sleep)
```
✓ Carrier thread 수: 8~16개 (CPU 코어 수만큼만)
✓ Virtual thread 수: 5,000+ (거의 무제한)
✓ 처리량: ~5,000 req/sec (10배 향상!)
✓ 평균 응답 시간: ~100ms (sleep 시간만)
✓ P95 응답 시간: ~110ms (안정적)
✓ Thread.sleep() 중에도 carrier thread는 다른 가상 스레드 처리
```

## 추가 모니터링 (권장)

### 1. JVM 모니터링
**VisualVM 또는 JConsole 사용**

플랫폼 스레드:
```
Threads: 200+ (스레드 풀 크기만큼)
CPU: 높음
Memory: 높음 (스레드당 1MB)
```

가상 스레드:
```
Platform Threads: 10~20개 (CPU 코어 수만큼만)
Virtual Threads: 수만 개
CPU: 낮음
Memory: 낮음
```

### 2. 시스템 리소스
- **macOS**: Activity Monitor
- **Linux**: `htop` 또는 `top`
- **Windows**: Task Manager

## 프로젝트 구조

```
├── jmeter/
│   └── Spring-MVC-Performance-Test.jmx     # 동일한 테스트로 양쪽 테스트
├── src/main/
│   ├── java/ex/demo/
│   │   ├── SleepTestController.java         # Thread.sleep(100) 테스트 API ⏱️
│   │   ├── ThreadInfoController.java        # 스레드 타입 확인 API
│   │   ├── Comment*.java                    # 댓글 기능 (참고용)
│   │   ├── SimulatedDelayService.java       # 지연 서비스 (참고용)
│   │   └── TestDataController.java          # 데이터 생성 (참고용)
│   └── resources/
│       ├── application.yml                  # 기본 설정 (플랫폼 스레드)
│       └── application-virtual.yml          # 가상 스레드 활성화
└── docs/
    ├── performance-test.md                  # 이 파일
    └── test-results-template.md             # 결과 기록 템플릿
```

## 가상 스레드가 유리한 경우

1. **I/O Bound 작업이 많을 때**
   - 데이터베이스 쿼리
   - 외부 API 호출
   - 파일 읽기/쓰기

2. **높은 동시성이 필요할 때**
   - 마이크로서비스 간 통신
   - 실시간 채팅, 알림
   - 대용량 배치 처리

3. **기존 블로킹 코드를 유지하고 싶을 때**
   - Reactive 프로그래밍 전환 부담 없음
   - 학습 비용 최소화
   - 디버깅 용이

## 주의사항

### 가상 스레드가 도움이 안 되는 경우
- **CPU Bound 작업**: 복잡한 계산, 암호화 등
- **메모리 Bound 작업**: 대용량 데이터 처리
- **낮은 동시성**: 동시 요청이 적을 때는 차이가 미미함

### Synchronized 블록 주의
```java
// ❌ 가상 스레드에서 피해야 할 코드
synchronized (lock) {
    // I/O 작업
}

// ✅ 대신 이렇게
Lock lock = new ReentrantLock();
lock.lock();
try {
    // I/O 작업
} finally {
    lock.unlock();
}
```

## 참고 자료

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Boot 3.2+ Virtual Threads Support](https://spring.io/blog/2023/09/09/all-together-now-spring-boot-3-2-graalvm-native-images-java-21-and-virtual)
- [Apache JMeter Documentation](https://jmeter.apache.org/usermanual/index.html)

## 문제 해결

### "Virtual threads are not available"
- Java 21 이상인지 확인: `java -version`
- Spring Boot 3.2 이상인지 확인
