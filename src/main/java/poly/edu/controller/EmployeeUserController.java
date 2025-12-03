package poly.edu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import poly.edu.dao.AccountDAO;
import poly.edu.dao.RoleDAO;
import poly.edu.model.Account;
import poly.edu.model.Role;
import poly.edu.service.AuthService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/employee")
public class EmployeeUserController {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    /**
     * Hiển thị danh sách người dùng
     * URL: /employee/users
     */
    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String search,
                           @RequestParam(required = false) String status,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("message", "❌ Vui lòng đăng nhập!");
            return "redirect:/account/login";
        }
        
        if (!authService.hasAnyRole("EMPLOYEE", "ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/home";
        }
        
        try {
            // Lấy tất cả tài khoản
            List<Account> allAccounts = accountDAO.findAll();
            
            // Lọc chỉ lấy những tài khoản có role USER
            List<Account> users = allAccounts.stream()
                .filter(acc -> acc.getRoles().stream()
                    .anyMatch(role -> role.getRoleName().equals("USER")))
                .collect(Collectors.toList());
            
            // Lọc theo tìm kiếm
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                users = users.stream()
                    .filter(user -> 
                        user.getUsername().toLowerCase().contains(searchLower) ||
                        (user.getFullName() != null && user.getFullName().toLowerCase().contains(searchLower)) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower)) ||
                        (user.getPhone() != null && user.getPhone().toLowerCase().contains(searchLower))
                    )
                    .collect(Collectors.toList());
            }
            
            // Lọc theo trạng thái
            if (status != null && !status.isEmpty()) {
                if (status.equals("active")) {
                    users = users.stream()
                        .filter(user -> user.getActive())
                        .collect(Collectors.toList());
                } else if (status.equals("inactive")) {
                    users = users.stream()
                        .filter(user -> !user.getActive())
                        .collect(Collectors.toList());
                }
            }
            
            model.addAttribute("users", users);
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            
            return "poly/employee/users";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "❌ Có lỗi xảy ra: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/employee/dashboard";
        }
    }

    /**
     * Hiển thị form thêm người dùng mới
     * URL: /employee/users/add
     */
    @GetMapping("/users/add")
    public String showAddForm(Model model, RedirectAttributes redirectAttributes) {
        if (!authService.isAuthenticated() || !authService.hasAnyRole("EMPLOYEE", "ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        model.addAttribute("user", new Account());
        return "poly/employee/users_add";
    }

    /**
     * Xử lý thêm người dùng mới
     * URL: POST /employee/users/save
     */
    @PostMapping("/users/save")
    public String addUser(@ModelAttribute("user") Account user,
                         @RequestParam String password,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated() || !authService.hasAnyRole("EMPLOYEE", "ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        try {
            // Kiểm tra username đã tồn tại
            if (accountDAO.findByUsername(user.getUsername()).isPresent()) {
                model.addAttribute("message", "❌ Tên đăng nhập đã tồn tại!");
                model.addAttribute("messageType", "error");
                model.addAttribute("user", user);
                return "poly/employee/users_add";
            }

            // Kiểm tra email đã tồn tại
            if (accountDAO.findByEmail(user.getEmail()).isPresent()) {
                model.addAttribute("message", "❌ Email đã được sử dụng!");
                model.addAttribute("messageType", "error");
                model.addAttribute("user", user);
                return "poly/employee/users_add";
            }

            // Mã hóa password
            user.setPassword(passwordEncoder.encode(password));
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());

            // Gán role USER
            Optional<Role> userRole = roleDAO.findByRoleName("USER");
            if (userRole.isPresent()) {
                Set<Role> roles = new HashSet<>();
                roles.add(userRole.get());
                user.setRoles(roles);
            } else {
                model.addAttribute("message", "❌ Không tìm thấy role USER trong hệ thống!");
                model.addAttribute("messageType", "error");
                model.addAttribute("user", user);
                return "poly/employee/users_add";
            }

            accountDAO.save(user);

            redirectAttributes.addFlashAttribute("message", "✅ Thêm người dùng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/employee/users";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "❌ Lỗi khi thêm người dùng: " + e.getMessage());
            model.addAttribute("messageType", "error");
            model.addAttribute("user", user);
            return "poly/employee/users_add";
        }
    }

    /**
     * Hiển thị form chỉnh sửa người dùng
     * URL: /employee/users/edit/{id}
     */
    @GetMapping("/users/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, 
                              Model model, 
                              RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated() || !authService.hasAnyRole("EMPLOYEE", "ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        try {
            Optional<Account> user = accountDAO.findById(id);
            
            if (user.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "❌ Không tìm thấy người dùng!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/employee/users";
            }

            model.addAttribute("user", user.get());
            return "poly/employee/users_edit";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "❌ Có lỗi xảy ra: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/employee/users";
        }
    }

    /**
     * Xử lý cập nhật người dùng
     * URL: POST /employee/users/update/{id}
     */
    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable("id") int id,
                            @ModelAttribute("user") Account formUser,
                            @RequestParam(required = false) String password,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated() || !authService.hasAnyRole("EMPLOYEE", "ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        try {
            Optional<Account> existingOpt = accountDAO.findById(id);
            
            if (existingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "❌ Không tìm thấy người dùng!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/employee/users";
            }

            Account existing = existingOpt.get();

            // Kiểm tra email trùng
            Optional<Account> emailCheck = accountDAO.findByEmail(formUser.getEmail());
            if (emailCheck.isPresent() && emailCheck.get().getAccountId() != id) {
                model.addAttribute("message", "❌ Email đã được sử dụng bởi tài khoản khác!");
                model.addAttribute("messageType", "error");
                model.addAttribute("user", formUser);
                return "poly/employee/users_edit";
            }

            // Cập nhật thông tin
            existing.setEmail(formUser.getEmail());
            existing.setFullName(formUser.getFullName());
            existing.setPhone(formUser.getPhone());
            existing.setAddress(formUser.getAddress());
            existing.setActive(formUser.getActive());

            // Nếu có password mới thì mã hóa và cập nhật
            if (password != null && !password.trim().isEmpty()) {
                existing.setPassword(passwordEncoder.encode(password));
            }

            accountDAO.save(existing);

            redirectAttributes.addFlashAttribute("message", "✅ Cập nhật người dùng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/employee/users";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "❌ Lỗi khi cập nhật: " + e.getMessage());
            model.addAttribute("messageType", "error");
            model.addAttribute("user", formUser);
            return "poly/employee/users_edit";
        }
    }

    /**
     * Vô hiệu hóa người dùng
     * URL: POST /employee/users/delete/{id}
     */
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") int id, 
                            RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated() || !authService.hasAnyRole("EMPLOYEE", "ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        try {
            Optional<Account> user = accountDAO.findById(id);
            
            if (user.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "❌ Không tìm thấy người dùng!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/employee/users";
            }

            // Vô hiệu hóa tài khoản thay vì xóa
            Account usr = user.get();
            usr.setActive(false);
            accountDAO.save(usr);

            redirectAttributes.addFlashAttribute("message", "✅ Vô hiệu hóa người dùng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "❌ Lỗi khi vô hiệu hóa người dùng: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/employee/users";
    }

    /**
     * Kích hoạt lại người dùng
     * URL: POST /employee/users/activate/{id}
     */
    @PostMapping("/users/activate/{id}")
    public String activateUser(@PathVariable("id") int id, 
                              RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated() || !authService.hasAnyRole("EMPLOYEE", "ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        try {
            Optional<Account> user = accountDAO.findById(id);
            
            if (user.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "❌ Không tìm thấy người dùng!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/employee/users";
            }

            Account usr = user.get();
            usr.setActive(true);
            accountDAO.save(usr);

            redirectAttributes.addFlashAttribute("message", "✅ Kích hoạt người dùng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "❌ Lỗi khi kích hoạt người dùng: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/employee/users";
    }
}