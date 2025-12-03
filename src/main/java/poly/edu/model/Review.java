package poly.edu.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Review")
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReviewId")
    private Integer reviewId;
    
    @Column(name = "Content", length = 500)
    private String content;
    
    @Column(name = "Rating")
    private Integer rating; // 1 đến 5 sao
    
    @Column(name = "ReviewDate")
    private LocalDateTime reviewDate;
    
    @Column(name = "ProductId")
    private Integer productId;
    
    @Column(name = "AccountId")
    private Integer accountId;
    
    // Quan hệ với Product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;
    
    // Quan hệ với Account (Người bình luận)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountId", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account account;
}