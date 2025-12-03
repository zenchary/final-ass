package poly.edu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.*;
import poly.edu.dao.AccountDAO;
import poly.edu.dao.RoleDAO;
import poly.edu.model.Account;
import poly.edu.model.Role;
import poly.edu.service.AuthService;
import poly.edu.service.MailService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.io.IOException;
import java.nio.file.*;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private MailService mailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    // ================== TRANG TÀI KHOẢN CHÍNH ==================
    @GetMapping
    public String accountPage(Model model) {
        if (!authService.isAuthenticated()) {
            return "redirect:/account/login";
        }

        Account loggedIn = authService.getAccount();
        boolean isAdmin = authService.hasRole("ADMIN");
        boolean isEmployee = authService.hasRole("EMPLOYEE");

        model.addAttribute("account", loggedIn);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isEmployee", isEmployee);
        
        return "poly/taikhoan/account";
    }

    // ================== Trang đăng nhập & đăng ký (tab chung) ==================
    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        // Nếu đã đăng nhập, redirect về /account
        if (authService.isAuthenticated()) {
            return "redirect:/account";
        }

        String rememberedUser = null;
        for (Cookie c : Optional.ofNullable(request.getCookies()).orElse(new Cookie[0])) {
            if (c.getName().equals("remember-username")) {
                rememberedUser = c.getValue();
                break;
            }
        }
        model.addAttribute("rememberedUser", rememberedUser);
        model.addAttribute("account", new Account());
        
        // Kiểm tra có error từ Spring Security không
        String error = request.getParameter("error");
        if (error != null) {
            model.addAttribute("error", "❌ Tên đăng nhập hoặc mật khẩu không đúng!");
        }
        
        return "poly/taikhoan/login-register";
    }

    // ================== Đăng ký ==================
    @PostMapping("/register")
    public String register(@ModelAttribute("account") Account account, Model model) {
        if (accountDAO.findByUsername(account.getUsername()).isPresent()) {
            model.addAttribute("error", "❌ Tên đăng nhập đã tồn tại!");
            return "poly/taikhoan/login-register";
        }
        if (accountDAO.findByEmail(account.getEmail()).isPresent()) {
            model.addAttribute("error", "❌ Email đã được sử dụng!");
            return "poly/taikhoan/login-register";
        }

        // Mã hóa password
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        account.setActive(true);
        account.setCreatedAt(LocalDateTime.now());

        // Gán role USER mặc định
        Optional<Role> userRole = roleDAO.findByRoleName("USER");
        if (userRole.isPresent()) {
            Set<Role> roles = new HashSet<>();
            roles.add(userRole.get());
            account.setRoles(roles);
        }

        accountDAO.save(account);

        model.addAttribute("message", "✅ Đăng ký thành công! Hãy đăng nhập.");
        return "poly/taikhoan/login-register";
    }

    // ================== Đăng xuất ==================
    // Spring Security sẽ tự xử lý /logout, không cần method này

    // ================== Cập nhật thông tin ==================
    @GetMapping("/update")
    public String updatePage(Model model) {
        if (!authService.isAuthenticated()) {
            return "redirect:/account/login";
        }
        
        // Lấy thông tin mới nhất từ DB để hiển thị lên form
        Account user = authService.getAccount();
        model.addAttribute("account", user);
        return "poly/taikhoan/update-account";
    }

    @PostMapping("/update")
    public String updateAccount(@ModelAttribute("account") Account formAccount, Model model) {
        if (!authService.isAuthenticated()) {
            return "redirect:/account/login";
        }

        Account currentUser = authService.getAccount();

        try {
            // Kiểm tra xem email mới có bị trùng với tài khoản KHÁC không
            Optional<Account> existingEmail = accountDAO.findByEmail(formAccount.getEmail());
            if (existingEmail.isPresent() && 
                existingEmail.get().getAccountId() != currentUser.getAccountId()) {
                
                model.addAttribute("error", "❌ Email này đã được sử dụng bởi tài khoản khác!");
                // Trả lại form với dữ liệu người dùng vừa nhập để họ không phải nhập lại từ đầu
                model.addAttribute("account", formAccount); 
                return "poly/taikhoan/update-account";
            }

            // Cập nhật các thông tin cho phép thay đổi
            currentUser.setEmail(formAccount.getEmail());
            currentUser.setFullName(formAccount.getFullName());
            
            // Cập nhật SĐT và Địa chỉ
            currentUser.setPhone(formAccount.getPhone());
            currentUser.setAddress(formAccount.getAddress());

            // Lưu vào database
            accountDAO.save(currentUser);
            
            // Cập nhật lại thông tin trong session (nếu bạn đang lưu user trong session thủ công)
            // session.setAttribute("account", currentUser); 

            model.addAttribute("account", currentUser);
            model.addAttribute("message", "✅ Cập nhật thông tin thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "❌ Lỗi khi cập nhật: " + e.getMessage());
            model.addAttribute("account", formAccount);
        }

        return "poly/taikhoan/update-account";
    }

    // ================== Đổi mật khẩu ==================
    @GetMapping("/doiMatKhau")
    public String doiMatKhauForm(Model model) {
        if (!authService.isAuthenticated()) {
            return "redirect:/account/login";
        }
        
        Account user = authService.getAccount();
        model.addAttribute("account", user);
        return "poly/taikhoan/doiMatKhau";
    }

    @PostMapping("/doiMatKhau")
    public String doiMatKhau(@RequestParam String oldPassword, 
                              @RequestParam String newPassword,
                              @RequestParam String confirmPassword, 
                              Model model) {

        if (!authService.isAuthenticated()) {
            return "redirect:/account/login";
        }

        Account currentUser = authService.getAccount();

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            model.addAttribute("error", "❌ Mật khẩu cũ không đúng!");
            model.addAttribute("account", currentUser);
            return "poly/taikhoan/doiMatKhau";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "❌ Mật khẩu xác nhận không khớp!");
            model.addAttribute("account", currentUser);
            return "poly/taikhoan/doiMatKhau";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("error", "❌ Mật khẩu mới phải có ít nhất 6 ký tự!");
            model.addAttribute("account", currentUser);
            return "poly/taikhoan/doiMatKhau";
        }

        // Mã hóa và cập nhật mật khẩu
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        accountDAO.save(currentUser);

        model.addAttribute("message", "✅ Đổi mật khẩu thành công!");
        model.addAttribute("account", currentUser);
        return "poly/taikhoan/doiMatKhau";
    }

    // ================== Quên mật khẩu ==================
    @GetMapping("/forgot")
    public String showForgotPage() {
        return "poly/taikhoan/forgot";
    }

    @PostMapping("/forgot")
    public String processForgot(@RequestParam("email") String input, Model model) {
        Optional<Account> opt = accountDAO.findByEmail(input);
        if (opt.isEmpty()) {
            opt = accountDAO.findByUsername(input);
        }

        if (opt.isEmpty()) {
            model.addAttribute("error", "❌ Không tìm thấy tài khoản với thông tin này!");
            return "poly/taikhoan/forgot";
        }

        Account acc = opt.get();

        // Tạo mã reset ngẫu nhiên
        String resetCode = java.util.UUID.randomUUID().toString().substring(0, 8);
        acc.setResetCode(resetCode);
        accountDAO.save(acc);

        // Gửi mail
        mailService.sendMail(acc.getEmail(), "Đặt lại mật khẩu - Ứng dụng của bạn",
                "Xin chào " + acc.getFullName() + ",\n\n" + 
                "Mã đặt lại mật khẩu của bạn là: " + resetCode + "\n\n" +
                "Truy cập trang sau để nhập mã và đặt lại mật khẩu mới:\n" +
                "http://localhost:8080/account/reset?email=" + acc.getEmail() + "\n\n" +
                "Trân trọng,\nĐội ngũ hỗ trợ.");

        return "redirect:/account/reset?email=" + acc.getEmail();
    }

    // ================== Xử lý reset mật khẩu ==================
    @GetMapping("/reset")
    public String showResetForm(@RequestParam(required = false) String email, Model model) {
        if (email != null) {
            model.addAttribute("email", email);
        }
        return "poly/taikhoan/reset";
    }

    @PostMapping("/reset")
    public String processReset(@RequestParam String email, 
                               @RequestParam String code, 
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword, 
                               Model model) {

        Optional<Account> opt = accountDAO.findByEmail(email);

        if (opt.isEmpty()) {
            model.addAttribute("error", "❌ Email không tồn tại!");
            return "poly/taikhoan/reset";
        }

        Account acc = opt.get();
        if (acc.getResetCode() == null || !acc.getResetCode().equals(code)) {
            model.addAttribute("error", "❌ Mã xác nhận không đúng!");
            model.addAttribute("email", email);
            return "poly/taikhoan/reset";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "❌ Mật khẩu xác nhận không khớp!");
            model.addAttribute("email", email);
            return "poly/taikhoan/reset";
        }

        // Mã hóa mật khẩu mới và reset code
        acc.setPassword(passwordEncoder.encode(newPassword));
        acc.setResetCode(null);
        accountDAO.save(acc);

        model.addAttribute("message", "✅ Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
        return "poly/taikhoan/login-register";
    }
    
    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("image") MultipartFile file, 
                               RedirectAttributes redirectAttributes) {
        if (!authService.isAuthenticated()) {
            return "redirect:/account/login";
        }
        
        poly.edu.model.Account user = authService.getAccount();
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "❌ Vui lòng chọn ảnh!");
            return "redirect:/account";
        }

        try {
            String fileName = "user_" + user.getAccountId() + ".jpg";
            
            // 1. Lưu vào thư mục SRC (để giữ file sau khi build lại)
            Path srcPath = Paths.get("src/main/resources/static/avatars");
            if (!Files.exists(srcPath)) Files.createDirectories(srcPath);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, srcPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            // 2. Lưu vào thư mục TARGET (để hiển thị ngay lập tức mà không cần restart)
            Path targetPath = Paths.get("target/classes/static/avatars");
            if (!Files.exists(targetPath)) Files.createDirectories(targetPath);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
            
            redirectAttributes.addFlashAttribute("message", "✅ Cập nhật ảnh đại diện thành công!");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "❌ Lỗi khi lưu ảnh: " + e.getMessage());
        }
        
        return "redirect:/account";
    }
}