package poly.edu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import poly.edu.config.OAuth2LoginSuccessHandler;
import poly.edu.service.CustomOAuth2UserService;
import poly.edu.service.DaoUserDetailsManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public UserDetailsService userDetailsService() {
        return new DaoUserDetailsManager();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Bỏ cấu hình mặc định CSRF và CORS
        http.csrf(config -> config.disable())
            .cors(config -> config.disable());

        // Phân quyền sử dụng
        http.authorizeHttpRequests(req -> {
            req.requestMatchers("/admin/**").hasRole("ADMIN");
            req.requestMatchers("/employee/**").hasAnyRole("EMPLOYEE", "ADMIN");
            req.requestMatchers("/account", "/account/update", "/account/doiMatKhau",
                              "/profile/**", "/orders/**", "/cart/**").authenticated();
            
            // Cho phép truy cập public
            req.requestMatchers("/", "/home", "/product/**", "/category/**", 
                              "/search", "/about", "/terms", "/privacy",
                              "/promotions", "/test-db", "/under-construction",
                              "/account/login", "/account/register", 
                              "/account/forgot", "/account/reset",
                              "/css/**", "/js/**", "/images/**", "/assets/**", "/photos/**").permitAll();
            
            req.anyRequest().permitAll();
        });

        http.exceptionHandling(denied -> 
            denied.accessDeniedPage("/unauthorized.html")
        );

        // Form đăng nhập thường
        http.formLogin(login -> login
            .loginPage("/account/login")
            .loginProcessingUrl("/account/login")
            .usernameParameter("username")
            .passwordParameter("password")
            .defaultSuccessUrl("/home", true)
            .failureUrl("/account/login?error=true")
            .permitAll()
        );

        // ==== THÊM CẤU HÌNH OAUTH2 (GOOGLE) TẠI ĐÂY ====
        http.oauth2Login(oauth2 -> oauth2
            .loginPage("/account/login") // Dùng chung trang login
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService) // Service xử lý dữ liệu User từ Google
            )
            .successHandler(oAuth2LoginSuccessHandler) // Handler lưu session sau khi login thành công
        );

        // Đăng xuất
        http.logout(logout -> logout
            .logoutUrl("/account/logout")
            .logoutSuccessUrl("/")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        );

        return http.build();
    }
}