package poly.edu.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poly.edu.model.Product;
import poly.edu.model.Review;
import poly.edu.repository.ProductRepository;
import poly.edu.repository.ReviewRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Review> getReviewsByProduct(Integer productId) {
        return reviewRepository.findByProductIdOrderByReviewDateDesc(productId);
    }

    @Transactional
    public void addReview(Integer accountId, Integer productId, String content, Integer rating) {
        // 1. Lưu Review mới
        Review review = new Review();
        review.setAccountId(accountId);
        review.setProductId(productId);
        review.setContent(content);
        review.setRating(rating);
        review.setReviewDate(LocalDateTime.now());
        reviewRepository.save(review);

        // 2. Tính toán lại Rating trung bình cho Product
        updateProductAverageRating(productId);
    }

    private void updateProductAverageRating(Integer productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByReviewDateDesc(productId);
        if (reviews.isEmpty()) return;

        double totalRating = 0;
        for (Review r : reviews) {
            totalRating += r.getRating();
        }
        
        double average = totalRating / reviews.size();
        
        // Làm tròn 1 chữ số thập phân
        average = Math.round(average * 10.0) / 10.0;

        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            product.setRating(average); 
            productRepository.save(product);
        }
    }
}