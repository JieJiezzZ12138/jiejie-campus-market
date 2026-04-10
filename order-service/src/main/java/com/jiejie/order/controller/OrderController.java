package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.order.entity.Cart;
import com.jiejie.order.entity.Order;
import com.jiejie.order.mapper.CartMapper;
import com.jiejie.order.mapper.OrderMapper;
import com.jiejie.product.mapper.ProductMapper;
import jakarta.servlet.http.HttpServletRequest; // 👈 必须引入请求对象，用来拿 Token 里的身份
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 1. 马上结算接口 (彻底告别参数伪造！)
     * 移除了 @RequestParam("username")，直接从 request 中安全获取
     */
    @PostMapping("/checkout")
    @Transactional
    public Result checkout(HttpServletRequest request,
                           @RequestParam("totalAmount") Double totalAmount) {

        // 👉 核心安全升级：从拦截器口袋里拿出绝对真实的身份
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        String currentUsername = (String) request.getAttribute("currentUsername");

        if (currentUserId == null || currentUsername == null) {
            return Result.error("未获取到安全登录状态，请重新登录");
        }

        System.out.println("收到结算请求，真实用户: " + currentUsername + ", 总计金额: " + totalAmount);

        // 使用真实 username 查询他自己的购物车
        List<Cart> cartList = cartMapper.selectByUsername(currentUsername);
        if (cartList == null || cartList.isEmpty()) {
            return Result.error("购物车是空的，无法结算");
        }

        String batchNo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        for (Cart cartItem : cartList) {
            Order order = new Order();
            order.setOrderNo("ORD" + batchNo + System.currentTimeMillis() % 1000);

            // 👉 核心修复：终于可以把写死的 2L 换成真实买家 ID 了！
            order.setBuyerId(currentUserId);

            order.setProductId(cartItem.getProductId());
            order.setBuyCount(cartItem.getQuantity());

            BigDecimal itemTotal = BigDecimal.valueOf(cartItem.getPrice())
                    .multiply(new BigDecimal(cartItem.getQuantity()));
            order.setTotalAmount(itemTotal);

            // 0-待支付/已下单
            order.setOrderStatus(0);

            orderMapper.createOrder(order);

            // 下架商品
            productMapper.updateStatus(cartItem.getProductId(), 2);
        }

        // 结算完成后，清空该用户的购物车
        cartMapper.deleteByUsername(currentUsername);

        return Result.success("下单成功，请在订单中心完成支付！");
    }

    /**
     * 2. 查询我的订单 (安全升级版)
     */
    @GetMapping("/list")
    public Result getOrderList(HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("未获取到登录状态");
        }

        System.out.println("查询订单列表，安全用户ID: " + currentUserId);

        // 👉 核心修复：用真实的用户 ID 去查他自己的订单
        List<Order> orders = orderMapper.findByUserId(currentUserId);

        return Result.success(orders);
    }

    /**
     * 3. 【全新】模拟支付接口
     * 真实场景下这里会对接支付宝/微信支付接口，咱们这里先直接更新数据库状态
     */
    @PostMapping("/pay")
    public Result payOrder(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("请先登录后再支付");
        }

        try {
            // 将订单状态更新为 1 (1 代表已支付/待发货)
            orderMapper.updateStatus(orderId, 1);
            return Result.success("支付成功！卖家将尽快为您发货。");
        } catch (Exception e) {
            return Result.error("支付系统异常：" + e.getMessage());
        }
    }
}