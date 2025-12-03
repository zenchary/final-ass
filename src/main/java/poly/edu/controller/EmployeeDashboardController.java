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

@Controller
@RequestMapping("/employee")
public class EmployeeDashboardController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private AccountDAO accountDAO;
    
    @Autowired
    private AuthService authService;
    
    /**
     * Employee Dashboard Home
     * URL: /employee/dashboard
     */
    @GetMapping("/dashboard")
    public String employeeDashboard(Model model, RedirectAttributes redirectAttributes) {
        // Check if user is logged in
        if (!authService.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("message", "❌ Vui lòng đăng nhập!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/account/login";
        }
        
        // Check if user has EMPLOYEE or ADMIN role
        if (!authService.hasAnyRole("EMPLOYEE", "ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập trang này!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/home";
        }
        
        try {
            // Get statistics
            long totalOrders = orderService.getAllOrders().size();
            long totalProducts = productRepository.count();
            long totalAccounts = accountDAO.count();
            
            // Đếm số lượng Users và Employees
            List<Account> allAccounts = accountDAO.findAll();
            long totalUsers = allAccounts.stream()
                .filter(acc -> acc.getRoles().stream()
                    .anyMatch(role -> role.getRoleName().equals("USER")))
                .count();
            
            long totalEmployees = allAccounts.stream()
                .filter(acc -> acc.getRoles().stream()
                    .anyMatch(role -> role.getRoleName().equals("EMPLOYEE")))
                .count();
            
            Double totalRevenue = orderService.getTotalRevenue();
            if (totalRevenue == null) {
                totalRevenue = 0.0;
            }
            
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalAccounts", totalAccounts);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalEmployees", totalEmployees);
            model.addAttribute("totalRevenue", totalRevenue);
            
            // Order statistics by status
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
            
            // Check if user is admin or employee
            boolean isAdmin = authService.hasRole("ADMIN");
            model.addAttribute("isAdmin", isAdmin);
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalProducts", 0);
            model.addAttribute("totalAccounts", 0);
            model.addAttribute("totalUsers", 0);
            model.addAttribute("totalEmployees", 0);
            model.addAttribute("totalRevenue", 0.0);
        }
        
        return "poly/employee/dashboard";
    }
    
    /**
     * Redirect /employee to /employee/dashboard
     */
    @GetMapping
    public String employeeRedirect() {
        return "redirect:/employee/dashboard";
    }
}