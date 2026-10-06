package com.group.booking.controller;

import com.group.booking.model.User;
import com.group.booking.repository.UserRepository;
import com.group.booking.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        // Đơn giản hóa: Không kiểm tra trùng email để code ngắn gọn
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);
        
        // So sánh mật khẩu trực tiếp (Không dùng hash cho đơn giản)
        if (user != null && user.getPassword().equals(loginRequest.getPassword())) {
            String token = jwtUtil.generateToken(user.getEmail());
            return ResponseEntity.ok(Map.of("token", token));
        }
        
        return ResponseEntity.status(401).body(Map.of("error", "Sai email hoặc mật khẩu"));
    }
}