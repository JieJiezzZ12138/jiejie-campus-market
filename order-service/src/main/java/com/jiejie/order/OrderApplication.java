package com.jiejie.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
/**
 * 1. 扩大组件扫描范围
 * 默认只扫描 com.jiejie.order。
 * 加上 "com.jiejie.product" 后，OrderService 才能发现并注入 Product 相关的 Service 和 Controller。
 */
@ComponentScan(basePackages = {"com.jiejie.order", "com.jiejie.product"})

/**
 * 2. 扩大 Mapper 扫描范围 (最关键的一步)
 * 解决 "Field productMapper required a bean of type... that could not be found" 的报错。
 * 这样 MyBatis 才会把 product 模块下的 Mapper 接口实例化为 Bean。
 */
@MapperScan(basePackages = {"com.jiejie.order.mapper", "com.jiejie.product.mapper"})

/**
 * 3. 启用 Feign 客户端
 * 如果你的 Feign 接口定义在 common 模块或者 product 模块，建议也加上 basePackages 确保被扫描到。
 */
@EnableFeignClients(basePackages = "com.jiejie")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}