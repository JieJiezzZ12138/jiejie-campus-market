package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.order.entity.ChatInboxItem;
import com.jiejie.order.entity.ChatThread;
import com.jiejie.order.entity.Order;
import com.jiejie.order.entity.PrivateMessage;
import com.jiejie.order.entity.User;
import com.jiejie.order.mapper.ChatThreadMapper;
import com.jiejie.order.mapper.ChatThreadReadMapper;
import com.jiejie.order.mapper.OrderMapper;
import com.jiejie.order.mapper.PrivateMessageMapper;
import com.jiejie.order.mapper.UserMapper;
import com.jiejie.order.security.AuthContext;
import com.jiejie.product.entity.Product;
import com.jiejie.product.mapper.ProductMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/chat")
public class ChatController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ChatThreadMapper chatThreadMapper;

    @Autowired
    private PrivateMessageMapper privateMessageMapper;

    @Autowired
    private ChatThreadReadMapper chatThreadReadMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 与登录用户相关的会话列表（作为买家/咨询者或卖家）
     */
    @GetMapping("/inbox")
    public Result inbox(HttpServletRequest request) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) {
            return Result.error("未登录");
        }
        List<ChatInboxItem> rows = chatThreadMapper.listInboxForUser(uid);
        return Result.success(rows);
    }

    /**
     * 进入私聊：三选一 — threadId（收件箱）、productId（商品页）、orderId（订单页）
     */
    @GetMapping("/context")
    public Result context(HttpServletRequest request,
                          @RequestParam(value = "threadId", required = false) Long threadIdParam,
                          @RequestParam(value = "productId", required = false) Long productId,
                          @RequestParam(value = "orderId", required = false) Long orderId) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) {
            return Result.error("未登录");
        }
        int n = (threadIdParam != null ? 1 : 0) + (productId != null ? 1 : 0) + (orderId != null ? 1 : 0);
        if (n != 1) {
            return Result.error("请仅指定 threadId、productId 或 orderId 之一");
        }
        if (threadIdParam != null) {
            return buildThreadContext(uid, threadIdParam);
        }
        if (productId != null) {
            return buildProductContext(uid, productId);
        }
        return buildOrderContext(uid, orderId);
    }

    private Result buildThreadContext(Long uid, Long threadIdParam) {
        ChatThread t = chatThreadMapper.findById(threadIdParam);
        if (t == null) {
            return Result.error("会话不存在");
        }
        if (!sameUser(uid, t.getSellerId()) && !sameUser(uid, t.getCustomerId())) {
            return Result.error("无权查看该会话");
        }
        Product p = productMapper.selectById(t.getProductId());
        Long peerId = sameUser(uid, t.getSellerId()) ? t.getCustomerId() : t.getSellerId();
        User peer = peerId != null ? userMapper.findById(peerId) : null;
        String peerNick = peer != null && StringUtils.hasText(peer.getNickname()) ? peer.getNickname() : "用户";

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("threadId", t.getId());
        map.put("productId", t.getProductId());
        map.put("productName", p != null ? p.getName() : null);
        map.put("sellerId", t.getSellerId());
        map.put("sellerName", p != null ? p.getSellerName() : null);
        map.put("peerNickname", peerNick);

        Order latest = orderMapper.findLatestByProductAndBuyer(t.getProductId(), t.getCustomerId());
        if (latest != null) {
            map.put("orderId", latest.getId());
            map.put("orderNo", latest.getOrderNo());
        } else {
            map.put("orderId", null);
            map.put("orderNo", null);
        }
        map.put("chatTitle", "与 " + peerNick + " · " + (p != null && p.getName() != null ? p.getName() : "商品"));
        return Result.success(map);
    }

    private Result buildProductContext(Long uid, Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) {
            return Result.error("商品不存在");
        }
        if (p.getSellerId() == null) {
            return Result.error("商品缺少卖家信息");
        }
        if (sameUser(uid, p.getSellerId())) {
            return Result.error("这是您自己发布的商品，无需联系卖家");
        }
        ChatThread thread = ensureThread(productId, p.getSellerId(), uid);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("threadId", thread.getId());
        map.put("productId", p.getId());
        map.put("productName", p.getName());
        map.put("sellerId", p.getSellerId());
        map.put("sellerName", p.getSellerName());
        map.put("orderId", null);
        map.put("orderNo", null);
        map.put("peerNickname", p.getSellerName());
        map.put("chatTitle", "咨询：" + (p.getName() != null ? p.getName() : "商品"));
        return Result.success(map);
    }

    private Result buildOrderContext(Long uid, Long orderId) {
        Order o = orderMapper.findByIdWithProduct(orderId);
        if (o == null) {
            return Result.error("订单不存在");
        }
        if (o.getSellerId() == null) {
            return Result.error("订单缺少卖家信息");
        }
        if (!sameUser(uid, o.getBuyerId()) && !sameUser(uid, o.getSellerId())) {
            return Result.error("无权查看该会话");
        }
        ChatThread thread = ensureThread(o.getProductId(), o.getSellerId(), o.getBuyerId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("threadId", thread.getId());
        map.put("productId", o.getProductId());
        map.put("productName", o.getProductName());
        map.put("sellerId", o.getSellerId());
        map.put("orderNo", o.getOrderNo());
        map.put("orderId", o.getId());
        User buyerU = o.getBuyerId() != null ? userMapper.findById(o.getBuyerId()) : null;
        User sellerU = o.getSellerId() != null ? userMapper.findById(o.getSellerId()) : null;
        map.put("peerNickname", sameUser(uid, o.getSellerId())
                ? (buyerU != null && StringUtils.hasText(buyerU.getNickname()) ? buyerU.getNickname() : "买家")
                : (sellerU != null && StringUtils.hasText(sellerU.getNickname()) ? sellerU.getNickname() : "卖家"));
        map.put("chatTitle", "订单沟通：" + (o.getProductName() != null ? o.getProductName() : "商品"));
        Product p = productMapper.selectById(o.getProductId());
        map.put("sellerName", p != null ? p.getSellerName() : null);
        return Result.success(map);
    }

    private ChatThread ensureThread(Long productId, Long sellerId, Long customerId) {
        ChatThread existing = chatThreadMapper.findByProductAndCustomer(productId, customerId);
        if (existing != null) {
            return existing;
        }
        ChatThread row = new ChatThread();
        row.setProductId(productId);
        row.setSellerId(sellerId);
        row.setCustomerId(customerId);
        chatThreadMapper.insert(row);
        if (row.getId() != null) {
            return row;
        }
        return chatThreadMapper.findByProductAndCustomer(productId, customerId);
    }

    @GetMapping("/messages")
    public Result listMessages(HttpServletRequest request,
                               @RequestParam("threadId") Long threadId,
                               @RequestParam(value = "orderId", required = false) Long orderId) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) {
            return Result.error("未登录");
        }
        ChatThread thread = chatThreadMapper.findById(threadId);
        if (thread == null) {
            return Result.error("会话不存在");
        }
        if (!sameUser(uid, thread.getSellerId()) && !sameUser(uid, thread.getCustomerId())) {
            return Result.error("无权查看该会话");
        }
        if (orderId != null) {
            Order o = orderMapper.findByIdWithProduct(orderId);
            if (o == null || (!sameUser(uid, o.getBuyerId()) && !sameUser(uid, o.getSellerId()))) {
                return Result.error("无权查看该订单消息");
            }
            if (!sameUser(o.getProductId(), thread.getProductId()) || !sameUser(o.getBuyerId(), thread.getCustomerId())) {
                return Result.error("订单与该会话不匹配");
            }
        }
        List<PrivateMessage> list = privateMessageMapper.listForThread(threadId, orderId);
        // 进入会话即视为已读（刷新/轮询也会更新）
        chatThreadReadMapper.upsert(threadId, uid, new java.util.Date());
        return Result.success(list);
    }

    @GetMapping("/unread-count")
    public Result unreadCount(HttpServletRequest request) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) {
            return Result.error("未登录");
        }
        int total = chatThreadReadMapper.countUnread(uid);
        return Result.success(total);
    }

    @PostMapping("/send")
    public Result send(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) {
            return Result.error("未登录");
        }
        String content = body.get("content") != null ? String.valueOf(body.get("content")).trim() : "";
        if (!StringUtils.hasText(content)) {
            return Result.error("消息不能为空");
        }
        if (content.length() > 2000) {
            return Result.error("消息过长");
        }

        Long threadId = toLong(body.get("threadId"));
        Long productIdParam = toLong(body.get("productId"));
        Long orderIdParam = toLong(body.get("orderId"));

        Long resolvedThreadId;
        Long productIdForMsg;
        Long orderIdForMsg = orderIdParam;

        if (threadId != null) {
            ChatThread th = chatThreadMapper.findById(threadId);
            if (th == null) {
                return Result.error("会话不存在");
            }
            if (!sameUser(uid, th.getSellerId()) && !sameUser(uid, th.getCustomerId())) {
                return Result.error("无权发送私信");
            }
            resolvedThreadId = threadId;
            productIdForMsg = th.getProductId();
            if (orderIdForMsg != null) {
                Order chk = orderMapper.findByIdWithProduct(orderIdForMsg);
                if (chk == null || (!sameUser(uid, chk.getBuyerId()) && !sameUser(uid, chk.getSellerId()))) {
                    return Result.error("无权关联该订单");
                }
                if (!sameUser(chk.getProductId(), th.getProductId()) || !sameUser(chk.getBuyerId(), th.getCustomerId())) {
                    return Result.error("订单与会话不匹配");
                }
            }
        } else if (productIdParam != null) {
            Product p = productMapper.selectById(productIdParam);
            if (p == null) {
                return Result.error("商品不存在");
            }
            if (p.getSellerId() == null) {
                return Result.error("商品缺少卖家信息");
            }
            if (sameUser(uid, p.getSellerId())) {
                return Result.error("不能给自己发私信");
            }
            ChatThread th = ensureThread(productIdParam, p.getSellerId(), uid);
            resolvedThreadId = th.getId();
            productIdForMsg = productIdParam;
            orderIdForMsg = null;
        } else if (orderIdParam != null) {
            Order o = orderMapper.findByIdWithProduct(orderIdParam);
            if (o == null) {
                return Result.error("订单不存在");
            }
            if (!sameUser(uid, o.getBuyerId()) && !sameUser(uid, o.getSellerId())) {
                return Result.error("无权发送私信");
            }
            ChatThread th = ensureThread(o.getProductId(), o.getSellerId(), o.getBuyerId());
            resolvedThreadId = th.getId();
            productIdForMsg = o.getProductId();
        } else {
            return Result.error("请提供 threadId、productId 或 orderId");
        }

        ChatThread th = chatThreadMapper.findById(resolvedThreadId);
        if (th == null) {
            return Result.error("会话不存在");
        }
        if (th.getSellerId() == null || th.getCustomerId() == null) {
            return Result.error("会话数据异常");
        }
        long receiverId = sameUser(uid, th.getCustomerId()) ? longId(th.getSellerId()) : longId(th.getCustomerId());

        PrivateMessage msg = new PrivateMessage();
        msg.setOrderId(orderIdForMsg);
        msg.setThreadId(resolvedThreadId);
        msg.setProductId(productIdForMsg);
        msg.setSenderId(uid);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        privateMessageMapper.insert(msg);

        return Result.success("发送成功");
    }

    private static long longId(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    /**
     * JWT / MyBatis 可能混用 Integer、Long，避免 equals 误判导致收不到消息
     */
    private static boolean sameUser(Object a, Object b) {
        if (a == null || b == null) {
            return false;
        }
        return ((Number) a).longValue() == ((Number) b).longValue();
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        return Long.valueOf(v.toString());
    }
}
