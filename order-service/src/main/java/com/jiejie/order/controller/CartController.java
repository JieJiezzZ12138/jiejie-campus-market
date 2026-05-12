package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.order.dto.ProductSummary;
import com.jiejie.order.entity.Cart;
import com.jiejie.order.feign.ProductFeign;
import com.jiejie.order.mapper.CartMapper;
import com.jiejie.order.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductFeign productFeign;

    /**
     * 1. 查询购物车列表
     */
    @GetMapping("/list")
    public Result getCartList(HttpServletRequest request) {
        Long currentUserId = AuthContext.currentUserId(request);
        String currentUsername = AuthContext.currentUsername(request);
        if (currentUserId == null || currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        try {
            List<Cart> list = cartMapper.selectByUserId(currentUserId);
            hydrateCartProducts(list);
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
        return addToCart(productId, null, request);
    }

    @PostMapping("/add-with-spec")
    public Result addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(value = "selectedSpec", required = false) String selectedSpec,
                            HttpServletRequest request) {
        String currentUsername = AuthContext.currentUsername(request);
        Long currentUserId = AuthContext.currentUserId(request);
        if (currentUserId == null || currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        try {
            ProductSummary product = productFeign.getSummary(productId);
            if (product == null) {
                return Result.error("商品不存在");
            }
            if (currentUserId != null && product.getSellerId() != null
                    && product.getSellerId().longValue() == currentUserId.longValue()) {
                return Result.error("不能把自己的商品加入购物车");
            }
            Cart existCart = cartMapper.findExistItem(currentUserId, productId);
            if (existCart != null) {
                cartMapper.updateQuantity(existCart.getId(), 1);
                if (selectedSpec != null) {
                    cartMapper.setSpec(currentUserId, currentUsername, productId, selectedSpec.trim());
                }
            } else {
                Cart cart = new Cart();
                cart.setUserId(currentUserId);
                cart.setUsername(currentUsername);
                cart.setProductId(productId);
                cart.setQuantity(1);
                cart.setSelectedSpec(selectedSpec != null ? selectedSpec.trim() : null);
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
        Long currentUserId = AuthContext.currentUserId(request);
        if (currentUserId == null || currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }

        int rows = 0;

        // 👉 逻辑分流：如果前端传了 productId，就按【用户名 + 商品ID】去删！
        if (productId != null) {
            System.out.println("执行删除：用户=" + currentUsername + " 移除商品ID=" + productId);
            rows = cartMapper.deleteByUserIdAndProductId(currentUserId, productId);
        }
        // 否则如果传了流水号 id，就按【主键 ID】去删
        else if (id != null) {
            System.out.println("执行删除：移除购物车流水主键=" + id);
            rows = cartMapper.deleteByIdAndUserId(id, currentUserId);
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
     * 4. 清空购物车
     */
    @PostMapping("/clear")
    public Result clearCart(HttpServletRequest request) {
        Long currentUserId = AuthContext.currentUserId(request);
        String currentUsername = AuthContext.currentUsername(request);
        if (currentUserId == null || currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        cartMapper.deleteByUserId(currentUserId);
        return Result.success("购物车已清空");
    }

    @PostMapping("/quantity")
    public Result updateQuantity(@RequestParam("productId") Long productId,
                                 @RequestParam("quantity") Integer quantity,
                                 HttpServletRequest request) {
        Long currentUserId = AuthContext.currentUserId(request);
        String currentUsername = AuthContext.currentUsername(request);
        if (currentUserId == null || currentUsername == null || currentUsername.isEmpty()) {
            return Result.error("未获取到登录状态");
        }
        if (quantity == null || quantity < 1 || quantity > 99) {
            return Result.error("商品数量必须在 1~99");
        }
        int n = cartMapper.setQuantity(currentUserId, currentUsername, productId, quantity);
        if (n <= 0) {
            return Result.error("购物车中不存在该商品");
        }
        return Result.success("数量已更新");
    }

    private void hydrateCartProducts(List<Cart> carts) {
        if (carts == null || carts.isEmpty()) {
            return;
        }
        Map<Long, ProductSummary> productMap = new LinkedHashMap<>();
        for (Cart cart : carts) {
            Long productId = cart.getProductId();
            if (productId == null || productMap.containsKey(productId)) {
                continue;
            }
            ProductSummary summary = productFeign.getSummary(productId);
            if (summary != null) {
                productMap.put(productId, summary);
            }
        }
        for (Cart cart : carts) {
            ProductSummary summary = productMap.get(cart.getProductId());
            if (summary == null) {
                continue;
            }
            cart.setProductName(summary.getName());
            cart.setPrice(summary.getPrice());
            cart.setImage(summary.getImage());
            cart.setImageUrl(summary.getImageUrl());
        }
    }
}
