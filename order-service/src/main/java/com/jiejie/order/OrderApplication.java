package com.jiejie.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Order 服务入口。
 * 注意：不要扫描整个 {@code com.jiejie.product}，否则会误把 {@link com.jiejie.product.ProductApplication}
 * 注册为配置类，导致启动失败。仅扫描 product 下的 controller、mapper、config 即可。
 */
@SpringBootApplication(scanBasePackages = {
        "com.jiejie.order",
        "com.jiejie.product.controller",
        "com.jiejie.product.mapper",
        "com.jiejie.product.config"
})
@MapperScan(basePackages = {"com.jiejie.order.mapper", "com.jiejie.product.mapper"})
@EnableFeignClients(basePackages = "com.jiejie")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
