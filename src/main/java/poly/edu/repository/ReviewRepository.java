package poly.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poly.edu.model.Review;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    // Lấy danh sách review của 1 sản phẩm, mới nhất lên đầu
    List<Review> findByProductIdOrderByReviewDateDesc(Integer productId);
}