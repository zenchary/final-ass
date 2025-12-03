package poly.edu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import poly.edu.model.Category;
import poly.edu.repository.CategoryRepository;
import poly.edu.service.AuthService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminCategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthService authService;

    /**
     * Hiển thị danh sách loại sản phẩm
     * URL: /admin/categories
     */
    @GetMapping("/categories")
    public String listCategories(@RequestParam(required = false) String search,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("message", "❌ Vui lòng đăng nhập!");
            return "redirect:/account/login";
        }
        
        if (!authService.hasRole("ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/home";
        }
        
        try {
            List<Category> categories;
            
            if (search != null && !search.trim().isEmpty()) {
                // Tìm kiếm theo tên hoặc mô tả
                String searchLower = search.toLowerCase();
                categories = categoryRepository.findAll().stream()
                    .filter(cat -> 
                        cat.getName().toLowerCase().contains(searchLower) ||
                        (cat.getDescription() != null && cat.getDescription().toLowerCase().contains(searchLower))
                    )
                    .toList();
            } else {
                categories = categoryRepository.findAllByOrderByNameAsc();
            }
            
            // --- ĐOẠN CODE MỚI THÊM ĐỂ TÍNH TOÁN THỐNG KÊ ---
            int totalProducts = 0;
            int activeCategories = 0;
            
            if (categories != null) {
                for (Category cat : categories) {
                    if (cat.getProducts() != null) {
                        int size = cat.getProducts().size();
                        totalProducts += size;
                        if (size > 0) {
                            activeCategories++;
                        }
                    }
                }
            }
            
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("activeCategories", activeCategories);
            // ------------------------------------------------
            
            model.addAttribute("categories", categories);
            model.addAttribute("search", search);
            
            return "poly/category/categories";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "❌ Có lỗi xảy ra: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/admin/dashboard";
        }
    }

    // ... (Giữ nguyên các method khác: add, save, edit, update, delete) ...
    // Copy lại toàn bộ các method khác bên dưới y nguyên như cũ
    
    /**
     * Hiển thị form thêm loại sản phẩm mới
     */
    @GetMapping("/categories/add")
    public String showAddForm(Model model, RedirectAttributes redirectAttributes) {
        if (!authService.isAuthenticated() || !authService.hasRole("ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        model.addAttribute("category", new Category());
        return "poly/category/categories_add";
    }

    /**
     * Xử lý thêm loại sản phẩm mới
     */
    @PostMapping("/categories/save")
    public String addCategory(@ModelAttribute("category") Category category,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated() || !authService.hasRole("ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        try {
            Optional<Category> existingCategory = categoryRepository.findByName(category.getName());
            if (existingCategory.isPresent()) {
                model.addAttribute("message", "❌ Tên loại sản phẩm đã tồn tại!");
                model.addAttribute("messageType", "error");
                model.addAttribute("category", category);
                return "poly/category/categories_add";
            }

            category.setCreatedAt(LocalDateTime.now());
            categoryRepository.save(category);

            redirectAttributes.addFlashAttribute("message", "✅ Thêm loại sản phẩm thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/admin/categories";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "❌ Lỗi khi thêm loại sản phẩm: " + e.getMessage());
            model.addAttribute("messageType", "error");
            model.addAttribute("category", category);
            return "poly/category/categories_add";
        }
    }

    @GetMapping("/categories/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, 
                              Model model, 
                              RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated() || !authService.hasRole("ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        try {
            Optional<Category> category = categoryRepository.findById(id);
            if (category.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "❌ Không tìm thấy loại sản phẩm!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/admin/categories";
            }
            model.addAttribute("category", category.get());
            return "poly/category/categories_edit";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "❌ Có lỗi xảy ra: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/categories/update/{id}")
    public String updateCategory(@PathVariable("id") int id,
                                @ModelAttribute("category") Category formCategory,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated() || !authService.hasRole("ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        try {
            Optional<Category> existingOpt = categoryRepository.findById(id);
            if (existingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "❌ Không tìm thấy loại sản phẩm!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/admin/categories";
            }

            Category existing = existingOpt.get();
            Optional<Category> nameCheck = categoryRepository.findByName(formCategory.getName());
            if (nameCheck.isPresent() && nameCheck.get().getCategoryId() != id) {
                model.addAttribute("message", "❌ Tên loại sản phẩm đã tồn tại!");
                model.addAttribute("messageType", "error");
                model.addAttribute("category", formCategory);
                return "poly/category/categories_edit";
            }

            existing.setName(formCategory.getName());
            existing.setDescription(formCategory.getDescription());
            categoryRepository.save(existing);

            redirectAttributes.addFlashAttribute("message", "✅ Cập nhật loại sản phẩm thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/admin/categories";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "❌ Lỗi khi cập nhật: " + e.getMessage());
            model.addAttribute("messageType", "error");
            model.addAttribute("category", formCategory);
            return "poly/category/categories_edit";
        }
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") int id, 
                                RedirectAttributes redirectAttributes) {
        
        if (!authService.isAuthenticated() || !authService.hasRole("ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "❌ Bạn không có quyền truy cập!");
            return "redirect:/account/login";
        }

        try {
            Optional<Category> category = categoryRepository.findById(id);
            if (category.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "❌ Không tìm thấy loại sản phẩm!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/admin/categories";
            }

            Category cat = category.get();
            if (cat.getProducts() != null && !cat.getProducts().isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "❌ Không thể xóa loại sản phẩm này vì còn " + cat.getProducts().size() + " sản phẩm!");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/admin/categories";
            }

            categoryRepository.delete(cat);
            redirectAttributes.addFlashAttribute("message", "✅ Xóa loại sản phẩm thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "❌ Lỗi khi xóa loại sản phẩm: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/categories";
    }
}