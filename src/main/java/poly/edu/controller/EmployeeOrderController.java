package poly.edu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import poly.edu.model.Order;
import poly.edu.model.OrderDetail;
import poly.edu.service.AuthService;
import poly.edu.service.OrderService;

import java.util.List;

@Controller
@RequestMapping("/employee/orders")
public class EmployeeOrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AuthService authService;
    
    // Danh sách đơn hàng
    @GetMapping
    public String listOrders(Model model, 
                            @RequestParam(required = false) Integer statusFilter,
                            RedirectAttributes redirectAttributes) {
        if (!authService.hasAnyRole("EMPLOYEE", "ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Không có quyền truy cập!");
            return "redirect:/home";
        }
        
        List<Order> orders;
        if (statusFilter != null && statusFilter > 0) {
            orders = orderService.getOrdersByStatusId(statusFilter);
        } else {
            orders = orderService.getAllOrders();
        }
        
        // Thống kê nhanh
        model.addAttribute("orders", orders);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("pendingCount", orderService.countOrdersByStatus(1));
        model.addAttribute("processingCount", orderService.countOrdersByStatus(2));
        model.addAttribute("shippedCount", orderService.countOrdersByStatus(3));
        model.addAttribute("deliveredCount", orderService.countOrdersByStatus(4));
        
        return "poly/employee/orders";
    }
    
    // Chi tiết đơn hàng (Dùng chung view với Admin hoặc tạo mới nếu muốn khác biệt)
    // Ở đây mình sẽ tái sử dụng view admin nhưng bạn có thể copy ra employee nếu cần sửa đổi
    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Integer orderId, Model model, RedirectAttributes redirectAttributes) {
        if (!authService.hasAnyRole("EMPLOYEE", "ADMIN")) {
            return "redirect:/home";
        }
        
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            redirectAttributes.addFlashAttribute("message", "❌ Không tìm thấy đơn hàng!");
            return "redirect:/employee/orders";
        }
        
        List<OrderDetail> orderDetails = orderService.getOrderDetails(orderId);
        model.addAttribute("order", order);
        model.addAttribute("orderDetails", orderDetails);
        
        return "poly/admin/order-detail"; // Tái sử dụng view chi tiết của Admin cho đẹp
    }
    
    // Cập nhật trạng thái
    @PostMapping("/{orderId}/update-status")
    public String updateStatus(@PathVariable Integer orderId, @RequestParam Integer newStatus, RedirectAttributes redirectAttributes) {
        if (!authService.hasAnyRole("EMPLOYEE", "ADMIN")) return "redirect:/home";
        
        if(orderService.updateOrderStatus(orderId, newStatus)) {
            redirectAttributes.addFlashAttribute("message", "✅ Cập nhật trạng thái thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } else {
            redirectAttributes.addFlashAttribute("message", "❌ Cập nhật thất bại!");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/employee/orders";
    }
}