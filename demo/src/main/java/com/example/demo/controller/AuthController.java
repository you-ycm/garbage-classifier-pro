package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.JwtTokenUtil;
import com.example.demo.dto.Result;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenUtil jwtTokenUtil,
                          UserDetailsService userDetailsService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== 登录接口 ====================
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        try {
            // 1. 认证用户名和密码
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );

            // 2. 认证通过后，通过 UserDetailsService 获取 UserDetails（包含角色信息）
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

            // 3. 生成 Token
            String token = jwtTokenUtil.generateToken(userDetails);

            // 4. 返回结果给前端
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority())
                    .orElse("ROLE_USER"));
            response.put("username", userDetails.getUsername());

            return response;
        } catch (Exception e) {
            throw new RuntimeException("用户名或密码错误: " + e.getMessage());
        }
    }

    // ==================== 注册接口 ====================
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        // 1. 检查用户名是否已存在
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return Result.error("用户名已存在");
        }

        // 2. 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 3. 默认注册为普通用户
        user.setRole("ROLE_USER");

        // 4. 保存用户
        userRepository.save(user);

        return Result.success("注册成功");
    }

    // ==================== 修改密码接口 ====================
    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        // 1. 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 2. 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("旧密码错误");
        }

        // 3. 加密新密码并保存
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return Result.success("密码修改成功");
    }
}