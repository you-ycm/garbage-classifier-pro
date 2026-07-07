package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        // 打印配置文件位置（调试用）
        System.out.println("Working Directory: " + System.getProperty("user.dir"));

        SpringApplication.run(DemoApplication.class, args);

        System.out.println("========================================");
        System.out.println("垃圾分类助手后端启动成功！");
        System.out.println("API地址: http://192.168.58.128:8080/api");
        System.out.println("健康检查: http://192.168.58.128:8080/api/health");
        System.out.println("========================================");
    }
}