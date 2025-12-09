package poly.edu.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // Nhớ import dòng này

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductId")
    private Integer productId;
    
    @Column(name = "CategoryId", nullable = false)
    private Integer categoryId;
    
    @Column(name = "Name", nullable = false, length = 300)
    private String name;
    
    @Column(name = "Description", length = 500)
    private String description;
    
    @Column(name = "Price", nullable = false)
    private Double price;
    
    @Column(name = "Quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "ImageUrl", length = 1000)
    private String imageUrl;
    
    @Column(name = "Rating")
    private Double rating;
    
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;
    
    // ✅ Thêm PromotionId
    @Column(name = "PromotionId")
    private Integer promotionId;
    
    @ManyToOne
    @JoinColumn(name = "CategoryId", insertable = false, updatable = false) // <--- THÊM insertable và updatable = false
    @JsonIgnoreProperties("products")
    private Category category;
    
    // Relationship with Promotion
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PromotionId", insertable = false, updatable = false)
    private Promotion promotion;
    
    // Helper method để tính giá sau khi giảm
    public Double getDiscountedPrice() {
        if (promotion != null && promotion.getStatus() && 
            promotion.getDiscount() != null && promotion.getDiscount() > 0) {
            return price * (1 - promotion.getDiscount());
        }
        return price;
    }
    
    // Check xem product có promotion hợp lệ không
    public boolean hasActivePromotion() {
        if (promotion == null || !promotion.getStatus()) return false;
        
        java.util.Date now = new java.util.Date();
        if (promotion.getStartDate() != null && promotion.getStartDate().after(now)) return false;
        if (promotion.getEndDate() != null && promotion.getEndDate().before(now)) return false;
        
        return true;
    }
}