package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        // 强制在内存中生成一个 MySQL 兼容的 H2 数据库
        // 这样完全不需要额外的 application-test.yml 文件
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=true"
})
class DemoApplicationTests {

    /**
     * 这个测试会利用 H2 内存数据库启动整个 Spring 环境。
     * 100% 能通过，完全不依赖真实 MySQL，也不受外部配置文件影响。
     */
    @Test
    void contextLoads() {
        // 如果没有抛出异常，说明环境启动成功
    }
}