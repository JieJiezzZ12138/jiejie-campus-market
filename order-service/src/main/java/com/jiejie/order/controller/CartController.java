package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.order.entity.Cart;
import com.jiejie.order.mapper.CartMapper;
import com.jiejie.order.security.AuthContext;
import com.jiejie.product.entity.Product;
import com.jiejie.product.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 1. 查询购物车列表
     */
    @GetMapping("/list")
    public Result getCartList(HttpServletRequest request) {
        String currentUsername = AuthContext.currentUsername(request);
        if (currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        try {
            List<Cart> list = cartMapper.selectByUsername(currentUsername);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 2. 加入购物车
     */
    @PostMapping("/add")
    public Result addToCart(@RequestParam("productId") Long productId, HttpServletRequest request) {
        String currentUsername = AuthContext.currentUsername(request);
        Long currentUserId = AuthContext.currentUserId(request);
        if (currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        try {
            Product product = productMapper.selectById(productId);
            if (product == null) {
                return Result.error("商品不存在");
            }
            if (currentUserId != null && product.getSellerId() != null
                    && product.getSellerId().longValue() == currentUserId.longValue()) {
                return Result.error("不能把自己的商品加入购物车");
            }
            Cart existCart = cartMapper.findExistItem(currentUsername, productId);
            if (existCart != null) {
                cartMapper.updateQuantity(existCart.getId(), 1);
            } else {
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
     * 3. 移除购物车商品 (👉 终极修复版：精准区分商品ID和流水号ID)
     */
    @RequestMapping(value = "/remove", method = {RequestMethod.GET, RequestMethod.POST})
    public Result removeCartItem(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "productId", required = false) Long productId,
            HttpServletRequest request) {

        String currentUsername = AuthContext.currentUsername(request);
        if (currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }

        int rows = 0;

        // 👉 逻辑分流：如果前端传了 productId，就按【用户名 + 商品ID】去删！
        if (productId != null) {
            System.out.println("执行删除：用户=" + currentUsername + " 移除商品ID=" + productId);
            rows = cartMapper.deleteByUsernameAndProductId(currentUsername, productId);
        }
        // 否则如果传了流水号 id，就按【主键 ID】去删
        else if (id != null) {
            System.out.println("执行删除：移除购物车流水主键=" + id);
            rows = cartMapper.deleteById(id);
        }
        else {
            return Result.error("删除失败：未接收到有效的商品 ID");
        }

        if (rows > 0) {
            return Result.success("移除成功");
        } else {
            return Result.error("移除失败：购物车中找不到该商品");
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
     */
    @PostMapping("/clear")
    public Result clearCart(HttpServletRequest request) {
        String currentUsername = AuthContext.currentUsername(request);
        if (currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        cartMapper.deleteByUsername(currentUsername);
        return Result.success("购物车已清空");
    }
}
