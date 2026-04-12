package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.order.entity.Cart;
import com.jiejie.order.entity.AdminNotification;
import com.jiejie.order.entity.ChatThread;
import com.jiejie.order.entity.Order;
import com.jiejie.order.entity.OrderFeedback;
import com.jiejie.order.entity.PrivateMessage;
import com.jiejie.order.entity.User;
import com.jiejie.order.mapper.AdminNotificationMapper;
import com.jiejie.order.mapper.CartMapper;
import com.jiejie.order.mapper.ChatThreadMapper;
import com.jiejie.order.mapper.OrderFeedbackMapper;
import com.jiejie.order.mapper.OrderMapper;
import com.jiejie.order.mapper.OrderNoticeMapper;
import com.jiejie.order.mapper.PrivateMessageMapper;
import com.jiejie.order.mapper.UserMapper;
import com.jiejie.product.mapper.ProductMapper;
import jakarta.servlet.http.HttpServletRequest; // 👈 必须引入请求对象，用来拿 Token 里的身份
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderFeedbackMapper orderFeedbackMapper;

    @Autowired
    private AdminNotificationMapper adminNotificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ChatThreadMapper chatThreadMapper;

    @Autowired
    private PrivateMessageMapper privateMessageMapper;

    @Autowired
    private OrderNoticeMapper orderNoticeMapper;

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

            // 订单通知：买家待付款、卖家新订单
            Long sellerId = null;
            try {
                if (cartItem.getProductId() != null) {
                    com.jiejie.product.entity.Product p = productMapper.selectById(cartItem.getProductId());
                    sellerId = p != null ? p.getSellerId() : null;
                }
            } catch (Exception ignored) {
                sellerId = null;
            }
            createNotice(order.getId(), currentUserId, "BUYER", "PAY_PENDING");
            if (sellerId != null) {
                createNotice(order.getId(), sellerId, "SELLER", "NEW_ORDER");
            }
        }

        // 结算完成后，清空该用户的购物车
        cartMapper.deleteByUsername(currentUsername);

        return Result.success("下单成功，请在订单中心完成支付！");
    }

    /**
     * 订单详情（买卖双方可见，用于私信页标题等）
     */
    @GetMapping("/detail")
    public Result orderDetail(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("未获取到登录状态");
        }
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) {
            return Result.error("订单不存在");
        }
        if (!currentUserId.equals(o.getBuyerId())
                && (o.getSellerId() == null || !currentUserId.equals(o.getSellerId()))) {
            return Result.error("无权查看该订单");
        }
        return Result.success(o);
    }

    /**
     * 2. 查询我的订单 (安全升级版)
     */
    @GetMapping("/list")
    public Result getOrderList(HttpServletRequest request,
                               @RequestParam(value = "scope", defaultValue = "buyer") String scope) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("未获取到登录状态");
        }

        System.out.println("查询订单列表，安全用户ID: " + currentUserId + ", scope=" + scope);

        List<Order> orders;
        if ("seller".equalsIgnoreCase(scope)) {
            orders = orderMapper.findBySellerId(currentUserId);
            // 卖家查看订单时，已收货通知可视为已读（信息性）
            orderNoticeMapper.markReadByTypes(currentUserId, "SELLER", Arrays.asList("RECEIVED"));
        } else {
            orders = orderMapper.findByUserId(currentUserId);
        }

        return Result.success(orders);
    }

    /**
     * 3. 模拟支付：仅买家本人、待支付订单可支付
     */
    @PostMapping("/pay")
    public Result payOrder(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("请先登录后再支付");
        }

        Order owned = orderMapper.findByIdAndUser(orderId, currentUserId);
        if (owned == null) {
            return Result.error("订单不存在或不属于当前用户");
        }
        if (owned.getOrderStatus() != null && owned.getOrderStatus() != 0) {
            return Result.error("订单已支付或状态异常");
        }
        int n = orderMapper.markPaid(orderId, currentUserId);
        if (n == 0) {
            return Result.error("支付失败，请刷新后重试");
        }
        // 清除买家待付款通知
        orderNoticeMapper.markReadByOrder(currentUserId, orderId);
        // 通知卖家：买家已付款
        Order paid = orderMapper.findByIdWithProduct(orderId);
        if (paid != null && paid.getSellerId() != null) {
            // 清理卖家该订单旧通知（如 NEW_ORDER）
            orderNoticeMapper.markReadByOrder(paid.getSellerId(), orderId);
            createNotice(orderId, paid.getSellerId(), "SELLER", "PAID");
        }
        return Result.success("支付成功！卖家将尽快为您发货。");
    }

    /**
     * 4. 卖家发货：已支付(待发货) → 已发货
     */
    @PostMapping("/ship")
    public Result shipOrder(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("请先登录");
        }
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) {
            return Result.error("订单不存在");
        }
        if (o.getSellerId() == null || !currentUserId.equals(o.getSellerId())) {
            return Result.error("只有商品卖家可以发货");
        }
        if (o.getOrderStatus() == null || o.getOrderStatus() != 1) {
            return Result.error("当前订单状态不可发货（需买家已支付）");
        }
        int n = orderMapper.markShipped(orderId);
        if (n == 0) {
            return Result.error("发货失败，请重试");
        }
        // 卖家已完成发货，清理该订单下卖家通知
        orderNoticeMapper.markReadByOrder(currentUserId, orderId);
        // 通知买家：已发货
        if (o.getBuyerId() != null) {
            createNotice(orderId, o.getBuyerId(), "BUYER", "SHIPPED");
        }
        return Result.success("已标记发货");
    }

    /**
     * 5. 买家确认收货：已发货 → 已收货
     */
    @PostMapping("/receive")
    public Result receiveOrder(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("请先登录");
        }
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) {
            return Result.error("订单不存在");
        }
        if (o.getBuyerId() == null || !currentUserId.equals(o.getBuyerId())) {
            return Result.error("只有买家可以确认收货");
        }
        if (o.getOrderStatus() == null || o.getOrderStatus() != 2) {
            return Result.error("当前订单状态不可确认收货（需卖家已发货）");
        }
        int n = orderMapper.markReceived(orderId, currentUserId);
        if (n == 0) {
            return Result.error("确认收货失败，请重试");
        }
        // 买家已确认收货，清理该订单下买家通知
        orderNoticeMapper.markReadByOrder(currentUserId, orderId);
        // 通知卖家：买家已确认收货
        if (o.getSellerId() != null) {
            createNotice(orderId, o.getSellerId(), "SELLER", "RECEIVED");
        }
        return Result.success("已确认收货，订单已完成");
    }

    /**
     * 订单红点：未读通知统计
     */
    @GetMapping("/notice/unread-count")
    public Result orderNoticeUnread(HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("未获取到登录状态");
        }
        int buyer = orderNoticeMapper.countUnreadByScope(currentUserId, "BUYER");
        int seller = orderNoticeMapper.countUnreadByScope(currentUserId, "SELLER");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("buyer", buyer);
        map.put("seller", seller);
        map.put("total", buyer + seller);
        return Result.success(map);
    }

    @GetMapping("/notice/list")
    public Result orderNoticeList(HttpServletRequest request,
                                  @RequestParam("scope") String scope,
                                  @RequestParam(value = "limit", defaultValue = "50") int limit) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("未获取到登录状态");
        }
        String normalized = normalizeScope(scope);
        if (normalized == null) {
            return Result.error("无效的 scope");
        }
        int cap = Math.min(Math.max(limit, 1), 200);
        return Result.success(orderNoticeMapper.listNotice(currentUserId, normalized, cap));
    }

    @PostMapping("/notice/read")
    public Result orderNoticeRead(HttpServletRequest request, @RequestParam("id") Long id) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("未获取到登录状态");
        }
        int n = orderNoticeMapper.markReadById(id, currentUserId);
        if (n == 0) {
            return Result.error("未找到通知或已读");
        }
        return Result.success("已标记已读");
    }

    @PostMapping("/notice/read-all")
    public Result orderNoticeReadAll(HttpServletRequest request, @RequestParam("scope") String scope) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("未获取到登录状态");
        }
        String normalized = normalizeScope(scope);
        if (normalized == null) {
            return Result.error("无效的 scope");
        }
        orderNoticeMapper.markReadAll(currentUserId, normalized);
        return Result.success("已全部标记已读");
    }

    private String normalizeScope(String scope) {
        if (scope == null) {
            return null;
        }
        String s = scope.trim().toLowerCase();
        if ("buyer".equals(s)) {
            return "BUYER";
        }
        if ("seller".equals(s)) {
            return "SELLER";
        }
        return null;
    }

    private void createNotice(Long orderId, Long userId, String scope, String type) {
        if (orderId == null || userId == null) {
            return;
        }
        com.jiejie.order.entity.OrderNotice n = new com.jiejie.order.entity.OrderNotice();
        n.setOrderId(orderId);
        n.setUserId(userId);
        n.setScope(scope);
        n.setNoticeType(type);
        n.setIsRead(0);
        orderNoticeMapper.insert(n);
    }

    /**
     * 买卖双方就订单问题提交反馈，通知管理员介入
     */
    @PostMapping("/feedback")
    public Result submitFeedback(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error("请先登录");
        }
        Object oid = body.get("orderId");
        String content = body.get("content") != null ? String.valueOf(body.get("content")).trim() : "";
        if (oid == null) {
            return Result.error("缺少 orderId");
        }
        if (!StringUtils.hasText(content)) {
            return Result.error("请填写反馈内容");
        }
        if (content.length() > 2000) {
            return Result.error("内容过长");
        }
        Long orderId = Long.valueOf(oid.toString());
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) {
            return Result.error("订单不存在");
        }
        if (!currentUserId.equals(o.getBuyerId()) && !currentUserId.equals(o.getSellerId())) {
            return Result.error("仅买卖双方可提交交易反馈");
        }
        orderFeedbackMapper.insert(orderId, currentUserId, content);

        AdminNotification n = new AdminNotification();
        n.setOrderId(orderId);
        n.setSenderId(currentUserId);
        String head = o.getOrderNo() != null ? o.getOrderNo() : ("#" + orderId);
        String tail = content.length() > 120 ? content.substring(0, 120) + "…" : content;
        n.setPreview("[交易反馈] 订单 " + head + "：" + tail);
        adminNotificationMapper.insert(n);

        return Result.success("已提交管理员，请耐心等待处理");
    }

    /**
     * 管理员：仅当订单存在用户「交易反馈」时，可查看该订单详情及买卖双方完整私信（不在其它入口暴露）
     */
    @GetMapping("/admin/feedback-order-view")
    public Result adminFeedbackOrderView(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long uid = (Long) request.getAttribute("currentUserId");
        if (uid == null) {
            return Result.error("未登录");
        }
        User admin = userMapper.findById(uid);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return Result.error("需要管理员权限");
        }
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) {
            return Result.error("订单不存在");
        }
        List<OrderFeedback> feedbacks = orderFeedbackMapper.listByOrderId(orderId);
        if (feedbacks == null || feedbacks.isEmpty()) {
            return Result.error("该订单暂无用户反馈记录，无权查看私信");
        }
        ChatThread thread = chatThreadMapper.findByProductAndCustomer(o.getProductId(), o.getBuyerId());
        List<PrivateMessage> messages;
        if (thread != null) {
            messages = privateMessageMapper.listForThread(thread.getId(), orderId);
        } else {
            messages = privateMessageMapper.listByOrderId(orderId);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order", o);
        data.put("feedbacks", feedbacks);
        data.put("messages", messages != null ? messages : List.of());
        return Result.success(data);
    }
}
