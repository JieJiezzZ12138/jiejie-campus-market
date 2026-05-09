package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.order.entity.Cart;
import com.jiejie.order.entity.AdminNotification;
import com.jiejie.order.entity.ChatThread;
import com.jiejie.order.entity.Order;
import com.jiejie.order.entity.OrderFeedback;
import com.jiejie.order.entity.PrivateMessage;
import com.jiejie.order.entity.DisputeMessage;
import com.jiejie.order.entity.User;
import com.jiejie.order.mapper.AdminNotificationMapper;
import com.jiejie.order.mapper.CartMapper;
import com.jiejie.order.mapper.ChatThreadMapper;
import com.jiejie.order.mapper.OrderFeedbackMapper;
import com.jiejie.order.mapper.OrderMapper;
import com.jiejie.order.mapper.OrderNoticeMapper;
import com.jiejie.order.mapper.PrivateMessageMapper;
import com.jiejie.order.mapper.UserMapper;
import com.jiejie.order.mapper.CouponMapper;
import com.jiejie.order.mapper.DisputeMessageMapper;
import com.jiejie.order.security.AuthContext;
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
import java.util.stream.Collectors;

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
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private DisputeMessageMapper disputeMessageMapper;

    private boolean isAdminOrSuperAdmin(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return "ADMIN".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole());
    }

    /**
     * 1. 马上结算接口 (彻底告别参数伪造！)
     * 移除了 @RequestParam("username")，直接从 request 中安全获取
     */
    @PostMapping("/checkout")
    @Transactional
    public Result checkout(HttpServletRequest request,
                           @RequestParam("totalAmount") Double totalAmount,
                           @RequestParam(value = "couponId", required = false) Long couponId) {

        // 👉 核心安全升级：从拦截器口袋里拿出绝对真实的身份
        Long currentUserId = AuthContext.currentUserId(request);
        String currentUsername = AuthContext.currentUsername(request);

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

        BigDecimal couponDiscount = BigDecimal.ZERO;
        String couponTitle = null;
        if (couponId != null) {
            com.jiejie.order.entity.Coupon c = couponMapper.findById(couponId);
            if (c == null || c.getStatus() == null || c.getStatus() != 1) {
                return Result.error("优惠券不可用");
            }
            List<com.jiejie.order.entity.Coupon> mine = couponMapper.listMyAvailable(currentUserId);
            boolean has = mine.stream().anyMatch(x -> x.getId().equals(couponId));
            if (!has) {
                return Result.error("您未持有该优惠券");
            }
            BigDecimal threshold = c.getThresholdAmount() == null ? BigDecimal.ZERO : c.getThresholdAmount();
            BigDecimal total = BigDecimal.valueOf(totalAmount);
            if (total.compareTo(threshold) < 0) {
                return Result.error("未达到优惠券门槛");
            }
            couponDiscount = c.getDiscountAmount() == null ? BigDecimal.ZERO : c.getDiscountAmount();
            couponTitle = c.getTitle();
        }

        for (int i = 0; i < cartList.size(); i++) {
            Cart cartItem = cartList.get(i);
            Order order = new Order();
            order.setOrderNo("ORD" + batchNo + System.currentTimeMillis() % 1000);

            // 👉 核心修复：终于可以把写死的 2L 换成真实买家 ID 了！
            order.setBuyerId(currentUserId);

            order.setProductId(cartItem.getProductId());
            order.setBuyCount(cartItem.getQuantity());

            BigDecimal itemTotal = BigDecimal.valueOf(cartItem.getPrice())
                    .multiply(new BigDecimal(cartItem.getQuantity()));
            if (couponDiscount.compareTo(BigDecimal.ZERO) > 0 && i == 0) {
                itemTotal = itemTotal.subtract(couponDiscount);
                if (itemTotal.compareTo(BigDecimal.ZERO) < 0) {
                    itemTotal = BigDecimal.ZERO;
                }
                order.setCouponId(couponId);
                order.setCouponTitle(couponTitle);
                order.setDiscountAmount(couponDiscount);
            }
            order.setTotalAmount(itemTotal);
            order.setSelectedSpec(cartItem.getSelectedSpec());

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
        if (couponId != null) {
            couponMapper.markUsed(currentUserId, couponId);
        }

        return Result.success("下单成功，请在订单中心完成支付！");
    }

    @GetMapping("/coupon/list")
    public Result couponList() {
        return Result.success(couponMapper.listOnline());
    }

    @GetMapping("/coupon/my")
    public Result myCoupons(HttpServletRequest request) {
        Long currentUserId = AuthContext.currentUserId(request);
        if (currentUserId == null) return Result.error("未登录");
        return Result.success(couponMapper.listMyAvailable(currentUserId));
    }

    @PostMapping("/coupon/receive")
    public Result receiveCoupon(HttpServletRequest request, @RequestParam("couponId") Long couponId) {
        Long currentUserId = AuthContext.currentUserId(request);
        if (currentUserId == null) return Result.error("未登录");
        int n = couponMapper.reduceStock(couponId);
        if (n == 0) return Result.error("优惠券已抢完");
        couponMapper.receive(currentUserId, couponId);
        return Result.success("领取成功");
    }

    /**
     * 订单详情（买卖双方可见，用于私信页标题等）
     */
    @GetMapping("/detail")
    public Result orderDetail(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long currentUserId = AuthContext.currentUserId(request);
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
        Long currentUserId = AuthContext.currentUserId(request);
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
        Long currentUserId = AuthContext.currentUserId(request);
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
        Long currentUserId = AuthContext.currentUserId(request);
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
        Long currentUserId = AuthContext.currentUserId(request);
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
        Long currentUserId = AuthContext.currentUserId(request);
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
        Long currentUserId = AuthContext.currentUserId(request);
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
        Long currentUserId = AuthContext.currentUserId(request);
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
        Long currentUserId = AuthContext.currentUserId(request);
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
        Long currentUserId = AuthContext.currentUserId(request);
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
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) {
            return Result.error("未登录");
        }
        User admin = userMapper.findById(uid);
        if (!isAdminOrSuperAdmin(admin)) {
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

    @GetMapping("/admin/list")
    public Result adminOrderList(HttpServletRequest request,
                                 @RequestParam(value = "orderNo", required = false) String orderNo,
                                 @RequestParam(value = "status", required = false) Integer status,
                                 @RequestParam(value = "userKeyword", required = false) String userKeyword) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) {
            return Result.error("未登录");
        }
        User admin = userMapper.findById(uid);
        if (!isAdminOrSuperAdmin(admin)) {
            return Result.error("需要管理员权限");
        }
        return Result.success(orderMapper.adminList(
                orderNo != null ? orderNo.trim() : null,
                status,
                userKeyword != null ? userKeyword.trim() : null
        ));
    }

    @PostMapping("/admin/status")
    public Result adminOrderStatus(HttpServletRequest request,
                                   @RequestParam("orderId") Long orderId,
                                   @RequestParam("status") Integer status) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) {
            return Result.error("未登录");
        }
        User admin = userMapper.findById(uid);
        if (!isAdminOrSuperAdmin(admin)) {
            return Result.error("需要管理员权限");
        }
        if (status == null || status < 0 || status > 5) {
            return Result.error("无效的订单状态");
        }
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) {
            return Result.error("订单不存在");
        }
        if (o.getOrderStatus() != null && o.getOrderStatus().equals(status)) {
            return Result.success("状态未变化");
        }
        int n = orderMapper.adminUpdateStatus(orderId, status);
        if (n == 0) {
            return Result.error("状态更新失败");
        }
        return Result.success("订单状态已更新");
    }

    @PostMapping("/cancel")
    public Result cancelOrder(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long currentUserId = AuthContext.currentUserId(request);
        if (currentUserId == null) return Result.error("请先登录");
        int n = orderMapper.markCanceled(orderId, currentUserId);
        if (n == 0) return Result.error("仅待支付订单可取消");
        return Result.success("订单已取消");
    }

    @PostMapping("/refund")
    public Result refundOrder(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long currentUserId = AuthContext.currentUserId(request);
        if (currentUserId == null) return Result.error("请先登录");
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null || o.getBuyerId() == null || !currentUserId.equals(o.getBuyerId())) {
            return Result.error("仅买家可申请退款");
        }
        int n = orderMapper.markRefundPending(orderId, currentUserId);
        if (n == 0) return Result.error("仅已支付或已发货订单可申请退款");
        if (o.getSellerId() != null) {
            createNotice(orderId, o.getSellerId(), "SELLER", "REFUND_PENDING");
        }
        AdminNotification an = new AdminNotification();
        an.setOrderId(orderId);
        an.setSenderId(currentUserId);
        an.setPreview("[退款申请] 订单 " + (o.getOrderNo() != null ? o.getOrderNo() : ("#" + orderId)) + "：买家申请退款");
        adminNotificationMapper.insert(an);
        return Result.success("退款申请已提交，等待卖家处理或管理员裁决");
    }

    @GetMapping("/logistics")
    public Result logistics(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long currentUserId = AuthContext.currentUserId(request);
        if (currentUserId == null) return Result.error("请先登录");
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) return Result.error("订单不存在");
        if (!currentUserId.equals(o.getBuyerId()) && !currentUserId.equals(o.getSellerId())) {
            return Result.error("无权查看该订单");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("company", "Jemall Express");
        data.put("trackingNo", "JM" + (o.getOrderNo() != null ? o.getOrderNo().replace("ORD", "") : orderId));
        List<String> traces = switch (o.getOrderStatus() == null ? -1 : o.getOrderStatus()) {
            case 0 -> List.of("订单已创建，待付款");
            case 1 -> List.of("买家已付款，等待商家发货");
            case 2 -> List.of("商家已发货", "运输途中", "派送中");
            case 3 -> List.of("商家已发货", "运输途中", "已签收");
            case 4 -> List.of("订单已取消");
            case 5 -> List.of("退款处理中", "退款已完成");
            default -> List.of("状态未知");
        };
        data.put("traces", traces.stream().map(x -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", x);
            m.put("time", System.currentTimeMillis());
            return m;
        }).collect(Collectors.toList()));
        return Result.success(data);
    }

    @GetMapping(value = "/admin/export", produces = "text/csv; charset=UTF-8")
    public String adminExport(HttpServletRequest request) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) return "未登录";
        User admin = userMapper.findById(uid);
        if (!isAdminOrSuperAdmin(admin)) return "无权限";
        List<Order> list = orderMapper.adminList(null, null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("orderId,orderNo,productName,buyer,seller,totalAmount,orderStatus,createTime\n");
        for (Order o : list) {
            sb.append(o.getId()).append(",")
                    .append(nullSafe(o.getOrderNo())).append(",")
                    .append(csv(o.getProductName())).append(",")
                    .append(csv(o.getBuyerNickname())).append(",")
                    .append(csv(o.getSellerNickname())).append(",")
                    .append(o.getTotalAmount()).append(",")
                    .append(o.getOrderStatus()).append(",")
                    .append(o.getCreateTime()).append("\n");
        }
        return sb.toString();
    }

    @PostMapping("/refund/approve")
    public Result approveRefund(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) return Result.error("未登录");
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) return Result.error("订单不存在");
        User u = userMapper.findById(uid);
        boolean sellerApprove = o.getSellerId() != null && o.getSellerId().equals(uid);
        boolean adminApprove = u != null && ("ADMIN".equals(u.getRole()) || "SUPER_ADMIN".equals(u.getRole()));
        if (!sellerApprove && !adminApprove) return Result.error("仅卖家或管理员可同意退款");
        int n = orderMapper.approveRefund(orderId);
        if (n == 0) return Result.error("当前订单不是待退款状态");
        if (o.getBuyerId() != null) createNotice(orderId, o.getBuyerId(), "BUYER", "REFUND_APPROVED");
        if (o.getSellerId() != null) orderNoticeMapper.markReadByOrder(o.getSellerId(), orderId);
        return Result.success("已同意退款");
    }

    @PostMapping("/refund/reject")
    public Result rejectRefund(HttpServletRequest request,
                               @RequestParam("orderId") Long orderId,
                               @RequestParam(value = "rollbackStatus", defaultValue = "1") Integer rollbackStatus) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) return Result.error("未登录");
        if (rollbackStatus == null || (rollbackStatus != 1 && rollbackStatus != 2)) {
            return Result.error("回滚状态仅允许 1(待发货) 或 2(已发货)");
        }
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) return Result.error("订单不存在");
        User u = userMapper.findById(uid);
        boolean sellerReject = o.getSellerId() != null && o.getSellerId().equals(uid);
        boolean adminReject = u != null && ("ADMIN".equals(u.getRole()) || "SUPER_ADMIN".equals(u.getRole()));
        if (!sellerReject && !adminReject) return Result.error("仅卖家或管理员可拒绝退款");
        int n = orderMapper.rejectRefund(orderId, rollbackStatus);
        if (n == 0) return Result.error("当前订单不是待退款状态");
        if (o.getBuyerId() != null) createNotice(orderId, o.getBuyerId(), "BUYER", "REFUND_REJECTED");
        if (o.getSellerId() != null) orderNoticeMapper.markReadByOrder(o.getSellerId(), orderId);
        return Result.success("已拒绝退款");
    }

    private String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    @GetMapping("/admin/dispute/messages")
    public Result adminDisputeMessages(HttpServletRequest request, @RequestParam("orderId") Long orderId) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) return Result.error("未登录");
        User u = userMapper.findById(uid);
        if (u == null || (!"ADMIN".equals(u.getRole()) && !"SUPER_ADMIN".equals(u.getRole()))) {
            return Result.error("需要管理员权限");
        }
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) return Result.error("订单不存在");
        return Result.success(disputeMessageMapper.listByOrderId(orderId));
    }

    @PostMapping("/admin/dispute/send")
    public Result adminDisputeSend(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) return Result.error("未登录");
        User u = userMapper.findById(uid);
        if (u == null || (!"ADMIN".equals(u.getRole()) && !"SUPER_ADMIN".equals(u.getRole()))) {
            return Result.error("需要管理员权限");
        }
        Object oid = body.get("orderId");
        String content = body.get("content") != null ? String.valueOf(body.get("content")).trim() : "";
        if (oid == null) return Result.error("缺少 orderId");
        if (!StringUtils.hasText(content)) return Result.error("消息不能为空");
        Long orderId = Long.valueOf(oid.toString());
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) return Result.error("订单不存在");
        DisputeMessage m = new DisputeMessage();
        m.setOrderId(orderId);
        m.setSenderId(uid);
        m.setSenderRole("ADMIN");
        m.setContent(content);
        disputeMessageMapper.insert(m);
        return Result.success("发送成功");
    }
}
