package poly.edu.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import poly.edu.dao.AccountDAO;
import poly.edu.model.Account;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private AccountDAO accountDAO;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // Tìm account trong DB
        Optional<Account> account = accountDAO.findByEmail(email);
        
        if (account.isPresent()) {
            // Lưu vào session giống như cách login thường
            HttpSession session = request.getSession();
            session.setAttribute("account", account.get());
        }

        // Chuyển hướng về trang chủ
        super.setDefaultTargetUrl("/home");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}