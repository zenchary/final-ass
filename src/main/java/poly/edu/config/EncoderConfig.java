package poly.edu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class EncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Sử dụng chuẩn mã hóa mặc định (BCrypt)
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}