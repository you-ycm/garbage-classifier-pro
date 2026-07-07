package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.JwtTokenUtil;
import com.example.demo.dto.Result;
import com.example.demo.service.EmailService;
import com.example.demo.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OperationLogService operationLogService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenUtil jwtTokenUtil,
                          UserDetailsService userDetailsService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          OperationLogService operationLogService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.operationLogService = operationLogService;
    }

    // ==================== 登录接口 ====================
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user, HttpServletRequest request) {
        // 提前准备返回的 Map，无论成功失败都用它封装
        Map<String, Object> response = new HashMap<>();

        try {
            String loginAccount = user.getUsername(); // 可能是用户名，也可能是邮箱
            String password = user.getPassword();

            // 1. 动态解析：如果输入包含 @，则视为邮箱；否则视为用户名
            boolean isEmail = loginAccount != null && loginAccount.contains("@");
            String finalUsername = loginAccount;

            // 2. 如果是邮箱登录，先查出这个邮箱对应的用户名
            if (isEmail) {
                Optional<User> userOpt = userRepository.findByEmail(loginAccount);
                if (userOpt.isPresent()) {
                    finalUsername = userOpt.get().getUsername();
                } else {
                    // 🔴 直接返回 401 错误，不抛出异常
                    response.put("code", 401);
                    response.put("message", "该邮箱未绑定任何账号");
                    return response;
                }
            }

            // 3. 执行 Spring Security 标准认证流程
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(finalUsername, password)
            );

            // 4. 认证通过，生成 Token 及返回信息
            UserDetails userDetails = userDetailsService.loadUserByUsername(finalUsername);
            String token = jwtTokenUtil.generateToken(userDetails);

            Optional<User> userOpt = userRepository.findByUsername(finalUsername);
            String email = userOpt.map(User::getEmail).orElse("");

            // 记录登录日志
            operationLogService.logOperation(finalUsername, "登录", "用户登录成功", request);

            response.put("code", 200);
            response.put("token", token);
            response.put("role", userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority())
                    .orElse("ROLE_USER"));
            response.put("username", userDetails.getUsername());
            response.put("email", email);

            return response;
        } catch (Exception e) {
            // 🔴 捕获所有异常，返回 401
            response.put("code", 401);
            response.put("message", "用户名/邮箱或密码错误");
            return response;
        }
    }

    // ==================== 注册接口 ====================
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user, HttpServletRequest request) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return Result.error("用户名已存在");
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                return Result.error("该邮箱已被注册");
            }
        } else {
            user.setEmail(null);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");
        userRepository.save(user);
        return Result.success("注册成功，请登录");
    }

    // ==================== 邮箱验证接口（已废弃） ====================
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok("该功能已关闭，无需验证，请直接登录");
    }

    // ==================== 修改密码接口 ====================
    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return Result.success("密码修改成功");
    }

    // ==================== 绑定邮箱接口 ====================
    @PostMapping("/bind-email")
    public Result<String> bindEmail(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String email = params.get("email");
        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (userRepository.findByEmail(email).isPresent()) {
            return Result.error("该邮箱已被其他账号绑定");
        }
        user.setEmail(email);
        userRepository.save(user);
        return Result.success("邮箱绑定成功");
    }

    // ==================== 忘记密码接口 ====================
    @PostMapping("/forgot-password")
    public Result<String> forgotPassword(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        if (email == null || email.trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Result.error("该邮箱未绑定任何账号");
        }
        User user = userOpt.get();
        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        try {
            emailService.sendResetPasswordEmail(email, newPassword);
            log.info("密码重置邮件已发送至: {}", email);
            return Result.success("重置密码邮件已发送，请检查邮箱");
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage());
            return Result.error("邮件发送失败，请稍后重试");
        }
    }

    // ==================== 新增：修改用户名接口 ====================
    @PutMapping("/update-username")
    public Result<String> updateUsername(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String newUsername = params.get("newUsername");

        // 1. 参数校验
        if (username == null || username.trim().isEmpty()) {
            return Result.error("原用户名不能为空");
        }
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return Result.error("新用户名不能为空");
        }
        String newUsernameTrimmed = newUsername.trim();
        if (newUsernameTrimmed.length() < 2 || newUsernameTrimmed.length() > 20) {
            return Result.error("用户名长度应在 2-20 个字符之间");
        }

        // 2. 检查用户是否存在
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 3. 检查新用户名是否已被占用（排除自己）
        Optional<User> existingUser = userRepository.findByUsername(newUsernameTrimmed);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            return Result.error("该用户名已被占用");
        }

        // 4. 更新用户名
        user.setUsername(newUsernameTrimmed);
        userRepository.save(user);

        log.info("用户 {} 已修改用户名为 {}", username, newUsernameTrimmed);
        return Result.success("用户名修改成功");
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
}