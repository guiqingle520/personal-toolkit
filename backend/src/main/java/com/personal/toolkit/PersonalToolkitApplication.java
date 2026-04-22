package com.personal.toolkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * personal-toolkit 后端应用的 Spring Boot 启动入口。
 */
@SpringBootApplication
@EnableScheduling
public class PersonalToolkitApplication {

    /**
     * 启动 Spring Boot 应用上下文。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PersonalToolkitApplication.class, args);
    }
}
