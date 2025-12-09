package poly.edu.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import poly.edu.model.Product;
import poly.edu.service.ProductService;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HomeRestController {

    @Autowired
    private ProductService productService;

    @GetMapping("/home")
    public ResponseEntity<?> getHomeData(@RequestParam(defaultValue = "0") int page) {
        // Lấy 12 sản phẩm mỗi trang, giống như Controller cũ
        Page<Product> products = productService.getAllProducts(page, 12);
        
        // Trả về JSON gồm list sản phẩm và thông tin phân trang
        Map<String, Object> response = new HashMap<>();
        response.put("products", products.getContent());
        response.put("currentPage", page);
        response.put("totalPages", products.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
}