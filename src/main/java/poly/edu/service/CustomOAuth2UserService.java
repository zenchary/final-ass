package poly.edu.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import poly.edu.dao.AccountDAO;
import poly.edu.dao.RoleDAO;
import poly.edu.model.Account;
import poly.edu.model.Role;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private RoleDAO roleDAO;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        Optional<Account> accountOptional = accountDAO.findByEmail(email);
        Account account;

        if (accountOptional.isEmpty()) {
            // Tài khoản chưa tồn tại -> Tạo mới
            account = new Account();
            account.setEmail(email);
            account.setUsername(email); // Dùng email làm username
            account.setFullName(name);
            account.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Mật khẩu ngẫu nhiên
            account.setActive(true);
            account.setCreatedAt(LocalDateTime.now());
            account.setAddress("Cập nhật sau");
            account.setPhone("");

            // Gán quyền USER
            Role userRole = roleDAO.findByRoleName("USER").orElse(null);
            if (userRole != null) {
                Set<Role> roles = new HashSet<>();
                roles.add(userRole);
                account.setRoles(roles);
            }
            
            accountDAO.save(account);
        } else {
            // Tài khoản đã tồn tại -> Cập nhật thông tin nếu cần
            account = accountOptional.get();
            if (!account.getActive()) {
                 throw new OAuth2AuthenticationException("Tài khoản của bạn đã bị khóa.");
            }
            account.setFullName(name);
            accountDAO.save(account);
        }

        // Chuyển đổi Role của Account thành GrantedAuthority
        Set<SimpleGrantedAuthority> authorities = account.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName()))
                .collect(Collectors.toSet());

        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "email");
    }
}