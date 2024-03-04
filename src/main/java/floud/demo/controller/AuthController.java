package floud.demo.controller;

import floud.demo.common.response.ApiResponse;
import floud.demo.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/callback/google")
    public ApiResponse<?> successGoogleLogin(@RequestParam("code") String code) {
        return authService.getGoogleAccessToken(code);
    }

    @GetMapping("/login")
    public RedirectView redirectToGoogle() {
        return authService.redirectToGoogle();
    }

}
