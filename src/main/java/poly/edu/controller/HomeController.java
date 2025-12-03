package poly.edu.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping; 
import org.springframework.web.servlet.mvc.support.RedirectAttributes; 

import poly.edu.model.Account;
import poly.edu.model.Category;
import poly.edu.model.Product;
import poly.edu.repository.ProductRepository;
import poly.edu.repository.CartRepository;
import poly.edu.repository.CategoryRepository;
import poly.edu.service.CategoryService;
import poly.edu.service.ProductService;
import poly.edu.model.Review;          
import poly.edu.service.ReviewService; 

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {
	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
    private ReviewService reviewService;
	
	/**
	* Helper method để thêm attributes chung cho navbar
	*/
	private void addCommonAttributes(Model model, HttpSession session) {

	// Thêm thông tin user và cart count
	Account account = (Account) session.getAttribute("account");
	if (account != null) {
	model.addAttribute("account", account);

	// Đếm số lượng items trong cart
	int cartCount = cartRepository.findByAccountId(account.getAccountId()).size();
	model.addAttribute("cartCount", cartCount);
	} else {
	model.addAttribute("cartCount", 0);
	}
	}

	/**
	* Trang chủ - hiển thị tất cả sản phẩm
	*/
    @Autowired
    private ProductService productService;
    
    @Autowired
    private CategoryService categoryService;
    
    
    @Autowired
    private CartRepository cartRepository;
    
    /**
     * Helper method để thêm cart count cho navbar
     */
    private void addCartCount(Model model, HttpSession session) {
        Account account = (Account) session.getAttribute("account");
        if (account != null) {
            int cartCount = cartRepository.findByAccountId(account.getAccountId()).size();
            model.addAttribute("cartCount", cartCount);
            model.addAttribute("account", account);
        } else {
            model.addAttribute("cartCount", 0);
        }
    }
    
    /**
    * Trang Promotions - chỉ hiển thị sản phẩm có khuyến mãi
    */
    @GetMapping("/promotions")
    public String promotions(@RequestParam(defaultValue = "0") int page, Model model) {
    int pageSize = 12;
    Pageable pageable = PageRequest.of(page, pageSize);

    // Lấy sản phẩm có promotionId không null
    Page<Product> productPage = productRepository.findProductsWithPromotion(pageable);

    model.addAttribute("products", productPage.getContent());
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", productPage.getTotalPages());
    model.addAttribute("totalProducts", productPage.getTotalElements());
    model.addAttribute("isPromotionPage", true);

    return "poly/promotions";
    }
    
    /**
     * Trang chủ - hiển thị tất cả sản phẩm
     */
    @GetMapping({"/", "/home"})
    public String home(Model model, 
                       @RequestParam(defaultValue = "0") int page,
                       HttpSession session) {
        try {
            Pageable pageable = PageRequest.of(page, 12);
            Page<Product> products = productRepository.findAll(pageable);

            List<Category> categories = categoryService.getCategoriesWithProducts();

            model.addAttribute("products", products.getContent());
            model.addAttribute("categories", categories);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", products.getTotalPages());
            model.addAttribute("totalProducts", products.getTotalElements());
            
            addCartCount(model, session);

            return "poly/index";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("products", new ArrayList<>());
            model.addAttribute("categories", new ArrayList<>());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalProducts", 0);
            return "poly/index";
        }
    }

    
    /**
     * Chi tiết sản phẩm (Đã cập nhật để lấy review)
     */
    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Integer id, 
                               Model model,
                               HttpSession session) {
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            
            // --- CODE THÊM MỚI: Lấy danh sách review ---
            List<Review> reviews = reviewService.getReviewsByProduct(id);
            model.addAttribute("reviews", reviews);
            // -------------------------------------------

            List<Category> categories = categoryService.getCategoriesWithProducts();
            model.addAttribute("categories", categories);
            model.addAttribute("product", product);
            
            addCartCount(model, session);

            return "poly/productdesc";
        } else {
            return "poly/under-construction";
        }
    }

    /**
     * Xử lý gửi đánh giá (Mới thêm)
     */
    @PostMapping("/product/review")
    public String submitReview(@RequestParam Integer productId,
                               @RequestParam String content,
                               @RequestParam Integer rating,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        
        Account account = (Account) session.getAttribute("account");
        if (account == null) {
            redirectAttributes.addFlashAttribute("message", "❌ Vui lòng đăng nhập để đánh giá!");
            return "redirect:/account/login";
        }

        try {
            reviewService.addReview(account.getAccountId(), productId, content, rating);
            redirectAttributes.addFlashAttribute("message", "✅ Cảm ơn bạn đã đánh giá!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "❌ Lỗi khi gửi đánh giá.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/product/" + productId;
    }


    /**
     * Xem sản phẩm theo category
     */
    @GetMapping("/category/{name}")
    public String viewCategory(@PathVariable String name,
                              @RequestParam(defaultValue = "0") int page,
                              Model model,
                              HttpSession session) {

        Category category = categoryService.getCategoryByName(name)
            .orElseThrow(() -> new RuntimeException("Category not found"));

        Pageable pageable = PageRequest.of(page, 12);
        Page<Product> products = productRepository.findByCategoryId(category.getCategoryId(), pageable);

        List<Category> categories = categoryService.getCategoriesWithProducts();
        model.addAttribute("categories", categories);
        model.addAttribute("categoryName", category.getName());
        model.addAttribute("products", products.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        
        addCartCount(model, session);

        return "poly/category";
    }

    /**
     * Tìm kiếm sản phẩm
     */
    @GetMapping("/search")
    public String search(@RequestParam("q") String keyword, 
                        Model model, 
                        @RequestParam(defaultValue = "0") int page,
                        HttpSession session) {
        if (keyword == null || keyword.trim().isEmpty()) {
            Page<Product> featuredProducts = productService.getFeaturedProducts(page, 12);
            model.addAttribute("products", featuredProducts.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", featuredProducts.getTotalPages());
            model.addAttribute("totalProducts", featuredProducts.getTotalElements());
        } else {
            Page<Product> searchResults = productService.searchProducts(keyword, page, 12);
            model.addAttribute("products", searchResults.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", searchResults.getTotalPages());
            model.addAttribute("totalProducts", searchResults.getTotalElements());
        }

        List<Category> categories = categoryService.getCategoriesWithProducts();
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);
        
        addCartCount(model, session);

        return "poly/search";
    }

    /**
     * Trang About
     */
    @GetMapping("/about")
    public String aboutPage(HttpSession session, Model model) {
        List<Category> categories = categoryService.getCategoriesWithProducts();
        model.addAttribute("categories", categories);
        addCartCount(model, session);
        return "poly/about";
    }
    
    /**
     * Trang Terms
     */
    @GetMapping("/terms")
    public String terms(HttpSession session, Model model) {
        List<Category> categories = categoryService.getCategoriesWithProducts();
        model.addAttribute("categories", categories);
        addCartCount(model, session);
        return "poly/terms";
    }

    /**
     * Trang Privacy
     */
    @GetMapping("/privacy")
    public String privacy(HttpSession session, Model model) {
        List<Category> categories = categoryService.getCategoriesWithProducts();
        model.addAttribute("categories", categories);
        addCartCount(model, session);
        return "poly/privacy";
    }
    
    /**
     * Trang Under Construction
     */
    @GetMapping("/under-construction")
    public String underConstruction() {
        return "poly/under-construction";
    }
    
    /**
     * Test database connection
     */
    @GetMapping("/test-db")
    public String testDatabase(Model model) {
        try {
            List<Product> allProducts = productRepository.findAll();
            model.addAttribute("message", "Database connection successful! Found " + allProducts.size() + " products.");
            model.addAttribute("products", allProducts);
            return "poly/test-db";
        } catch (Exception e) {
            model.addAttribute("message", "Database connection failed: " + e.getMessage());
            model.addAttribute("products", new ArrayList<>());
            return "poly/test-db";
        }
    }
}