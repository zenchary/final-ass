package poly.edu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration; // Mới thêm
import org.springframework.web.cors.CorsConfigurationSource; // Mới thêm
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // Mới thêm

import poly.edu.config.OAuth2LoginSuccessHandler;
import poly.edu.service.CustomOAuth2UserService;
import poly.edu.service.DaoUserDetailsManager;

import java.util.Arrays; // Mới thêm

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
        // Cấu hình CORS và tắt CSRF
        http.cors(c -> c.configurationSource(corsConfigurationSource())) // Mới thêm
            .csrf(c -> c.disable());

        http.authorizeHttpRequests(req -> {
            req.requestMatchers("/api/**").permitAll(); // Cho phép API không cần đăng nhập
            req.requestMatchers("/admin/**").hasRole("ADMIN");
            req.requestMatchers("/employee/**").hasAnyRole("EMPLOYEE", "ADMIN");
            req.requestMatchers("/account", "/account/update", "/account/doiMatKhau",
                              "/profile/**", "/orders/**", "/cart/**").authenticated();
            
            req.requestMatchers("/", "/home", "/product/**", "/category/**", 
                              "/search", "/about", "/terms", "/privacy",
                              "/promotions", "/test-db", "/under-construction",
                              "/account/login", "/account/register", 
                              "/account/forgot", "/account/reset",
                              "/css/**", "/js/**", "/images/**", "/assets/**", "/photos/**", "/avatars/**").permitAll();
            
            req.anyRequest().permitAll();
        });

        // ... (Giữ nguyên phần formLogin và oauth2Login cũ của bạn) ...
        http.formLogin(login -> login
            .loginPage("/account/login")
            .loginProcessingUrl("/account/login")
            .usernameParameter("username")
            .passwordParameter("password")
            .defaultSuccessUrl("/home", true)
            .failureUrl("/account/login?error=true")
            .permitAll()
        );

        http.oauth2Login(oauth2 -> oauth2
            .loginPage("/account/login")
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            .successHandler(oAuth2LoginSuccessHandler)
        );

        http.logout(logout -> logout
            .logoutUrl("/account/logout")
            .logoutSuccessUrl("/")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        );

        return http.build();
    }

    // Bean cấu hình CORS cho phép Vue (port 5173) gọi sang
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}