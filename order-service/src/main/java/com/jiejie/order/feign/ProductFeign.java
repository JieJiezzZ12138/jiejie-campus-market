package com.jiejie.order.feign;

import com.jiejie.common.Result; // 👈 同样使用公共的 Result
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductFeign {
    // 这里的路径必须和 Product 微服务里 Controller 定义的完全一致
    @PostMapping("/product/admin/updateStatus")
    Result updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status);
}