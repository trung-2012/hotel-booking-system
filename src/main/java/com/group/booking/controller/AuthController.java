package com.group.booking.controller;

import com.group.booking.dto.request.AuthRequestDTO;
import com.group.booking.model.Role;
import com.group.booking.model.User;
import com.group.booking.repository.UserRepository;
import com.group.booking.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequestDTO dto) {
        //check mail có trùng không
        if(userRepository.findByEmail(dto.getEmail()).isPresent()){
            return ResponseEntity.badRequest().body(Map.of("error", "Email đã tồn tại"));
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElse(null);

        // dùng password encoder
        if(user != null && passwordEncoder.matches(dto.getPassword(), user.getPassword())){
            String token = jwtUtil.generateToken(dto.getEmail());
            return ResponseEntity.ok(Map.of("token", token));
        }
        
        return ResponseEntity.status(401).body(Map.of("error", "Sai email hoặc mật khẩu"));
    }
}