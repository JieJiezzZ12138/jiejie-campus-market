package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.order.entity.Cart;
import com.jiejie.order.mapper.CartMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 购物车管理控制器 (已修复 405 移除报错)
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartMapper cartMapper;

    /**
     * 1. 查询购物车列表
     * 对应前端：GET /cart/list
     */
    @GetMapping("/list")
    public Result getCartList(HttpServletRequest request) {
        String currentUsername = (String) request.getAttribute("currentUsername");
        if (currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        System.out.println("查询用户 [" + currentUsername + "] 的购物车列表");
        try {
            List<Cart> list = cartMapper.selectByUsername(currentUsername);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 2. 加入购物车
     * 对应前端：POST /cart/add?productId=xxx
     */
    @PostMapping("/add")
    public Result addToCart(@RequestParam("productId") Long productId, HttpServletRequest request) {
        String currentUsername = (String) request.getAttribute("currentUsername");
        if (currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        System.out.println("加入购物车：用户=" + currentUsername + ", 商品ID=" + productId);
        try {
            // 检查商品是否已在购物车中
            Cart existCart = cartMapper.findExistItem(currentUsername, productId);
            if (existCart != null) {
                // 已存在则数量 +1
                cartMapper.updateQuantity(existCart.getId(), 1);
            } else {
                // 不存在则新增记录
                Cart cart = new Cart();
                cart.setUsername(currentUsername);
                cart.setProductId(productId);
                cart.setQuantity(1);
                cartMapper.insert(cart);
            }
            return Result.success("成功加入购物车");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 3. 移除购物车商品 (修复 405 移除报错的关键)
     * 这里使用 RequestMethod.POST 配合 RequestMethod.GET，双重保险解决 405 问题
     */
    @RequestMapping(value = "/remove", method = {RequestMethod.GET, RequestMethod.POST})
    public Result removeCartItem(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "productId", required = false) Long productId,
            HttpServletRequest request) {
        String currentUsername = (String) request.getAttribute("currentUsername");
        if (currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        // 1. 逻辑判断：如果 id 为空，就尝试用 productId
        Long targetId = (id != null) ? id : productId;
        System.out.println("收到移除请求：接收到的参数 id=" + id + ", productId=" + productId + ", 用户=" + currentUsername);
        if (targetId == null) {
            return Result.error("删除失败：未接收到有效的 ID 参数");
        }
        System.out.println("👉 准备执行删除，目标记录主键 ID =" + targetId);
        // 2. 核心操作：按主键 ID 删除
        int rows = cartMapper.deleteById(targetId);
        if (rows > 0) {
            return Result.success("移除成功");
        } else {
            // 如果删不掉，说明数据库里确实没有 ID 为 targetId 的那一行记录
            return Result.error("移除失败：数据库中不存在 ID 为 " + targetId + " 的记录");
        }
    }

    /**
     * 4. 根据主键 ID 删除 (保留备用)
     */
    @PostMapping("/deleteById")
    public Result deleteCart(@RequestParam("id") Long id) {
        cartMapper.deleteById(id);
        return Result.success("删除成功");
    }

    /**
     * 5. 清空购物车
     * 对应前端：POST /cart/clear
     */
    @PostMapping("/clear")
    public Result clearCart(HttpServletRequest request) {
        String currentUsername = (String) request.getAttribute("currentUsername");
        if (currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        System.out.println("清空用户 [" + currentUsername + "] 的购物车");
        cartMapper.deleteByUsername(currentUsername);
        return Result.success("购物车已清空");
    }
}