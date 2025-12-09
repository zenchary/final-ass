package poly.edu.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryId")
    private Long categoryId;

    @Column(name = "Name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "Description", length = 500)
    private String description;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    // Relationship with Products
 // ✅ Ngăn vòng lặp vô hạn
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products;
}



