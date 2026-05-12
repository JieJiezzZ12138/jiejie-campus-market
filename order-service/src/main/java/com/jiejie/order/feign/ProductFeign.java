package com.jiejie.order.feign;

import com.jiejie.common.Result;
import com.jiejie.order.dto.ProductSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", configuration = FeignAuthForwardConfig.class)
public interface ProductFeign {
    @GetMapping("/product/internal/summary")
    ProductSummary getSummary(@RequestParam("id") Long id);

    @PostMapping("/product/internal/reduce-stock")
    Result reduceStock(@RequestParam("id") Long id, @RequestParam("quantity") Integer quantity);

    @PostMapping("/product/internal/status")
    Result updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status);
}
