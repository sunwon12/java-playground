package ex.demo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 추가적인 쿼리 메서드가 필요하면 여기에 정의
}
