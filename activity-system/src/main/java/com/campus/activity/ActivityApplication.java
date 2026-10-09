package com.campus.activity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园活动管理系统 V1.0 启动类
 *
 * @author 勇闯天涯组
 */
@SpringBootApplication
@MapperScan("com.campus.activity.mapper")
public class ActivityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityApplication.class, args);
        System.out.println("\n========== 校园活动管理系统 V1.0 启动成功 ==========");
        System.out.println("接口文档地址: http://localhost:8080/swagger-ui.html");
        System.out.println("================================================\n");
    }
}
