package poly.edu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import poly.edu.dao.AccountDAO;
import poly.edu.model.Account;
import poly.edu.repository.ProductRepository;
import poly.edu.service.AuthService;
import poly.edu.service.OrderService;

import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private AccountDAO accountDAO;
    
    @Autowired
    private AuthService authService;
    
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, RedirectAttributes redirectAttributes) {
        // 1. Kiểm tra đăng nhập
        if (!authService.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("message", "❌ Vui lòng đăng nhập!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/account/login";
        }
        
        // 2. Kiểm tra quyền ADMIN
        if (!authService.hasRole("ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập trang này!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/home";
        }
        
        try {
            // --- THỐNG KÊ TỔNG QUAN ---
            
            // Đếm đơn hàng (Null check an toàn)
            List<?> orders = orderService.getAllOrders();
            long totalOrders = (orders != null) ? orders.size() : 0;
            
            // Đếm sản phẩm
            long totalProducts = productRepository.count();
            
            // Đếm tổng tài khoản
            long totalAccounts = accountDAO.count();
            
            // --- PHÂN LOẠI TÀI KHOẢN (USER vs EMPLOYEE) ---
            List<Account> allAccounts = accountDAO.findAll();
            if (allAccounts == null) allAccounts = new ArrayList<>();

            long totalUsers = allAccounts.stream()
                .filter(acc -> acc.getRoles() != null && acc.getRoles().stream()
                    .anyMatch(role -> "USER".equalsIgnoreCase(role.getRoleName())))
                .count();
            
            long totalEmployees = allAccounts.stream()
                .filter(acc -> acc.getRoles() != null && acc.getRoles().stream()
                    .anyMatch(role -> "EMPLOYEE".equalsIgnoreCase(role.getRoleName())))
                .count();
            
            // --- DOANH THU ---
            Double totalRevenue = orderService.getTotalRevenue();
            if (totalRevenue == null) {
                totalRevenue = 0.0;
            }
            
            // Đưa dữ liệu vào Model
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalAccounts", totalAccounts);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalEmployees", totalEmployees);
            model.addAttribute("totalRevenue", totalRevenue);
            
            // --- THỐNG KÊ TRẠNG THÁI ĐƠN HÀNG ---
            // Giả sử: 1=Pending, 2=Processing, 3=Shipped, 4=Delivered, 5=Cancelled
            long pendingOrders = orderService.countOrdersByStatus(1);
            long processingOrders = orderService.countOrdersByStatus(2);
            long shippedOrders = orderService.countOrdersByStatus(3);
            long deliveredOrders = orderService.countOrdersByStatus(4);
            long cancelledOrders = orderService.countOrdersByStatus(5);
            
            model.addAttribute("pendingOrders", pendingOrders);
            model.addAttribute("processingOrders", processingOrders);
            model.addAttribute("shippedOrders", shippedOrders);
            model.addAttribute("deliveredOrders", deliveredOrders);
            model.addAttribute("cancelledOrders", cancelledOrders);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Nếu lỗi, set giá trị mặc định để dashboard vẫn hiển thị được (dù số liệu sai)
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalProducts", 0);
            model.addAttribute("totalAccounts", 0);
            model.addAttribute("totalUsers", 0);
            model.addAttribute("totalEmployees", 0);
            model.addAttribute("totalRevenue", 0.0);
            
            model.addAttribute("error", "Có lỗi khi tải dữ liệu thống kê: " + e.getMessage());
        }
        
        return "poly/admin/dashboard";
    }
    
    // Redirect /admin về /admin/dashboard cho tiện
    @GetMapping
    public String adminRedirect() {
        return "redirect:/admin/dashboard";
    }
}