package com.example.demo;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用于生成 BCrypt 加密密码的测试类
 * 运行此类的 main 方法，输入你想设置的密码，控制台会输出加密后的字符串
 * 将该字符串存入数据库的 password 字段即可
 */
public class PasswordTest {

    public static void main(String[] args) {
        // 1. 创建 BCrypt 密码编码器
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 2. 在这里输入你想要设置的密码（例如 admin123）
        String rawPassword = "1234567";

        // 3. 生成加密后的密码（每次运行结果都不一样，因为包含随机盐）
        String encodedPassword = encoder.encode(rawPassword);

        // 4. 输出结果
        System.out.println("========== 结果 ==========");
        System.out.println("原始密码: " + rawPassword);
        System.out.println("加密后的密码 (复制这个到数据库): " + encodedPassword);
        System.out.println("==========================");

        // 额外验证：你可以把数据库里的值传进来验证是否匹配
        // boolean matches = encoder.matches("admin123", "$2a$10$...你的数据库里的值...");
        // System.out.println("密码验证结果: " + matches);
    }
}