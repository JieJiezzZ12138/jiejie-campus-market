<template>
  <div class="admin-layout">
    <el-container style="height: 100vh;">
      
      <el-header class="admin-header">
        <div class="logo">
          <el-icon size="24" color="#fff"><DataBoard /></el-icon>
          <span>杰物 Jemall 管理后台</span>
        </div>
        
        <div class="header-right">
          <el-badge :value="unreadCount" :hidden="unreadCount <= 0" class="msg-badge" :max="99">
            <el-icon size="22" class="header-icon" @click="openNotifDrawer"><Bell /></el-icon>
          </el-badge>
          <el-dropdown trigger="click">
            <div class="user-profile">
              <el-avatar :size="32" :src="userInfo.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
              <span class="nickname">{{ userInfo.nickname || '超级管理员' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/')">返回杰物商城</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container>
        <el-aside width="220px" class="admin-sidebar">
          <el-menu
            :default-active="activeMenu"
            class="sidebar-menu"
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409EFF"
            @select="handleMenuSelect"
          >
            <el-menu-item index="products">
              <el-icon><Goods /></el-icon>
              <span>全站商品管控</span>
            </el-menu-item>
            <el-menu-item index="users">
              <el-icon><User /></el-icon>
              <span>平台账号管理</span>
            </el-menu-item>
            <el-menu-item index="orders">
              <el-icon><Tickets /></el-icon>
              <span>订单管理</span>
            </el-menu-item>
            <el-menu-item index="banners">
              <el-icon><Picture /></el-icon>
              <span>轮播管理</span>
            </el-menu-item>
            <el-menu-item index="notices">
              <el-icon><Bell /></el-icon>
              <span>公告管理</span>
            </el-menu-item>
            <el-menu-item index="feedback">
              <el-icon><Bell /></el-icon>
              <span>{{ isSuperAdmin ? '管理员反馈处理' : '向超管反馈' }}</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="admin-main">
          
          <div v-if="activeMenu === 'products'">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>📦 平台商品总览</span>
                  <el-button type="success" :icon="Refresh" circle @click="fetchAdminProducts" />
                </div>
              </template>

              <el-table :data="pagedAdminProducts" stripe style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="ID" width="80" align="center" />
                <el-table-column label="主图" width="100" align="center">
                  <template #default="{ row }">
                    <el-image :src="getImageUrl(row.image || row.imageUrl)" style="width: 50px; height: 50px; border-radius: 4px;" fit="cover">
                      <template #error><el-icon><Picture /></el-icon></template>
                    </el-image>
                  </template>
                </el-table-column>
                <el-table-column prop="name" label="商品名称" width="180" show-overflow-tooltip />
                <el-table-column label="价格" width="100">
                  <template #default="{ row }"><span style="color: #f56c6c; font-weight: bold;">¥{{ row.price }}</span></template>
                </el-table-column>
                <el-table-column prop="stock" label="剩余库存" width="100" align="center" />
                <el-table-column label="综合状态" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getStatusTheme(row)" effect="dark">{{ getStatusText(row) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="发布人" width="120" align="center">
                  <template #default="{ row }"><span>{{ row.sellerName || row.sellerId }}</span></template>
                </el-table-column>
                <el-table-column label="操作" min-width="200" align="center">
                  <template #default="{ row }">
                    <el-button v-if="getStatusText(row) === '售卖中'" type="danger" size="small" @click="handleStatusChange(row, 2, 0)">违规下架</el-button>
                    <el-button v-if="getStatusText(row) === '违规下架'" type="success" size="small" @click="handleStatusChange(row, 1, 1)">恢复上架</el-button>
                    <el-button disabled size="small" v-if="getStatusText(row) === '已售罄'">商品已售出</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:10px;">
                <el-pagination background layout="total, prev, pager, next" :total="adminProducts.length" v-model:current-page="prodPageNo" :page-size="10" />
              </div>
            </el-card>
          </div>

          <div v-if="activeMenu === 'users'">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>👥 平台用户管理</span>
                  <el-button type="success" :icon="Refresh" circle @click="fetchAdminUsers" />
                </div>
              </template>

              <el-table :data="pagedAdminUsers" stripe style="width: 100%" v-loading="userLoading">
                <el-table-column prop="id" label="用户ID" width="100" align="center" />
                
                <el-table-column label="头像" width="100" align="center">
                  <template #default="{ row }">
                    <el-avatar :size="40" :src="row.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                  </template>
                </el-table-column>

                <el-table-column prop="nickname" label="昵称" width="150" />
                <el-table-column prop="username" label="登录账号" width="150" />
                <el-table-column prop="phone" label="手机号" width="140" />
                
                <el-table-column label="身份角色" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.role === 'SUPER_ADMIN' ? 'warning' : (row.role === 'ADMIN' ? 'danger' : 'primary')">
                      {{ row.role === 'SUPER_ADMIN' ? '超级管理员' : (row.role === 'ADMIN' ? '系统管理员' : '学生卖家') }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="账号状态" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark">
                      {{ row.status === 1 ? '正常' : '已封禁' }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="操作" min-width="250" align="center">
                  <template #default="{ row }">
                    <el-button 
                      v-if="row.status === 1" 
                      type="danger" size="small" plain 
                      @click="handleUserStatus(row, 0)">
                      封禁账号
                    </el-button>
                    <el-button 
                      v-else 
                      type="success" size="small" plain 
                      @click="handleUserStatus(row, 1)">
                      解封账号
                    </el-button>

                    <el-button 
                      v-if="isSuperAdmin && row.role !== 'ADMIN'" 
                      type="warning" size="small" 
                      @click="handleUserRole(row, 'ADMIN')">
                      设为管理员
                    </el-button>
                    <el-button
                      v-if="isSuperAdmin && row.role !== 'SUPER_ADMIN'"
                      type="danger"
                      size="small"
                      @click="handleUserRole(row, 'SUPER_ADMIN')">
                      设为超级管理员
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:10px;">
                <el-pagination background layout="total, prev, pager, next" :total="adminUsers.length" v-model:current-page="userPageNo" :page-size="10" />
              </div>
            </el-card>
          </div>

          <div v-if="activeMenu === 'orders'">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>🧾 平台订单管理</span>
                  <el-button type="success" :icon="Refresh" circle @click="fetchAdminOrders" />
                </div>
              </template>

              <div style="display:flex; gap:10px; margin-bottom: 12px; flex-wrap: wrap;">
                <el-input v-model="orderQuery.orderNo" placeholder="按订单号搜索" clearable style="width: 220px;" />
                <el-input v-model="orderQuery.userKeyword" placeholder="按用户ID/账号/昵称搜索" clearable style="width: 240px;" />
                <el-select v-model="orderQuery.status" placeholder="全部状态" clearable style="width: 180px;">
                  <el-option label="待支付" :value="0" />
                  <el-option label="待发货" :value="1" />
                  <el-option label="已发货" :value="2" />
                  <el-option label="已完成" :value="3" />
                </el-select>
                <el-button type="primary" @click="fetchAdminOrders">查询</el-button>
                <el-button type="warning" plain @click="exportOrders">导出 CSV(Excel可打开)</el-button>
              </div>

              <div style="display:grid;grid-template-columns:repeat(4,minmax(140px,1fr));gap:10px;margin-bottom:12px;">
                <el-card shadow="never"><div>总订单数</div><div style="font-size:22px;font-weight:700;">{{ stats.totalOrders }}</div></el-card>
                <el-card shadow="never"><div>总销售额</div><div style="font-size:22px;font-weight:700;">￥{{ stats.totalAmount }}</div></el-card>
                <el-card shadow="never"><div>待处理订单</div><div style="font-size:22px;font-weight:700;">{{ stats.pendingOrders }}</div></el-card>
                <el-card shadow="never"><div>平台用户数</div><div style="font-size:22px;font-weight:700;">{{ stats.totalUsers }}</div></el-card>
              </div>
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:12px;">
                <el-card shadow="never">
                  <div style="margin-bottom:8px;font-weight:600;">订单状态分布</div>
                  <div v-for="x in orderStatusSeries" :key="x.label" style="display:flex;align-items:center;gap:8px;margin:6px 0;">
                    <div style="width:88px;color:#606266;">{{ x.label }}</div>
                    <el-progress :percentage="x.percent" :stroke-width="14" />
                    <div style="width:24px;text-align:right;">{{ x.count }}</div>
                  </div>
                </el-card>
                <el-card shadow="never">
                  <div style="margin-bottom:8px;font-weight:600;">近7天订单趋势</div>
                  <div v-for="x in orderTrend7d" :key="x.day" style="display:flex;align-items:center;gap:8px;margin:6px 0;">
                    <div style="width:88px;color:#606266;">{{ x.day }}</div>
                    <el-progress :percentage="x.percent" :stroke-width="14" status="success" />
                    <div style="width:24px;text-align:right;">{{ x.count }}</div>
                  </div>
                </el-card>
              </div>

              <el-table :data="pagedAdminOrders" stripe style="width: 100%" v-loading="orderLoading">
                <el-table-column prop="id" label="ID" width="80" align="center" />
                <el-table-column prop="orderNo" label="订单号" min-width="180" />
                <el-table-column prop="productName" label="商品" min-width="160" />
                <el-table-column label="买家" min-width="160">
                  <template #default="{ row }">{{ row.buyerNickname || row.buyerUsername || row.buyerId }}</template>
                </el-table-column>
                <el-table-column label="卖家" min-width="160">
                  <template #default="{ row }">{{ row.sellerNickname || row.sellerUsername || row.sellerId }}</template>
                </el-table-column>
                <el-table-column label="金额" width="110">
                  <template #default="{ row }">￥{{ row.totalAmount }}</template>
                </el-table-column>
                <el-table-column label="状态" width="160">
                  <template #default="{ row }">
                    <el-select :model-value="row.orderStatus" size="small" style="width: 130px;" @change="(v) => handleAdminOrderStatus(row, v)">
                      <el-option label="待支付" :value="0" />
                      <el-option label="待发货" :value="1" />
                      <el-option label="已发货" :value="2" />
                      <el-option label="已完成" :value="3" />
                      <el-option label="已取消" :value="4" />
                      <el-option label="已退款" :value="5" />
                      <el-option label="退款待处理" :value="6" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="退款处理" width="180" align="center">
                  <template #default="{ row }">
                    <el-space v-if="Number(row.orderStatus) === 6">
                      <el-button size="small" type="success" @click="approveRefund(row)">同意</el-button>
                      <el-button size="small" type="danger" @click="rejectRefund(row)">拒绝</el-button>
                    </el-space>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="下单时间" min-width="180">
                  <template #default="{ row }">{{ row.createTime ? new Date(row.createTime).toLocaleString() : '-' }}</template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:10px;">
                <el-pagination background layout="total, prev, pager, next" :total="adminOrders.length" v-model:current-page="orderPageNo" :page-size="10" />
              </div>
            </el-card>
          </div>

          <div v-if="activeMenu === 'banners'">
            <el-card class="box-card">
              <template #header><div class="card-header"><span>🖼 轮播管理</span><el-button v-if="isSuperAdmin" type="primary" @click="openBannerForm()">新增</el-button></div></template>
              <el-table :data="pagedBannerList" stripe v-loading="bannerLoading">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="title" label="标题" />
                <el-table-column prop="subtitle" label="副标题" />
                <el-table-column prop="sortNo" label="排序" width="90" />
                <el-table-column label="状态" width="90"><template #default="{row}">{{ Number(row.status)===1?'上线':'下线' }}</template></el-table-column>
                <el-table-column label="操作" width="180">
                  <template #default="{row}">
                    <el-button size="small" :disabled="!isSuperAdmin" @click="openBannerForm(row)">编辑</el-button>
                    <el-button size="small" type="danger" :disabled="!isSuperAdmin" @click="deleteBanner(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:10px;">
                <el-pagination background layout="total, prev, pager, next" :total="bannerList.length" v-model:current-page="bannerPageNo" :page-size="10" />
              </div>
            </el-card>
          </div>

          <div v-if="activeMenu === 'notices'">
            <el-card class="box-card">
              <template #header><div class="card-header"><span>📢 公告管理</span><el-button v-if="isSuperAdmin" type="primary" @click="openNoticeForm()">新增</el-button></div></template>
              <el-table :data="pagedNoticeListAdmin" stripe v-loading="noticeLoadingAdmin">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column prop="title" label="标题" />
                <el-table-column prop="content" label="内容" show-overflow-tooltip />
                <el-table-column label="状态" width="90"><template #default="{row}">{{ Number(row.status)===1?'上线':'下线' }}</template></el-table-column>
                <el-table-column label="操作" width="180">
                  <template #default="{row}">
                    <el-button size="small" :disabled="!isSuperAdmin" @click="openNoticeForm(row)">编辑</el-button>
                    <el-button size="small" type="danger" :disabled="!isSuperAdmin" @click="deleteNotice(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:10px;">
                <el-pagination background layout="total, prev, pager, next" :total="noticeListAdmin.length" v-model:current-page="noticePageNo" :page-size="10" />
              </div>
            </el-card>
          </div>

          <div v-if="activeMenu === 'feedback'">
            <el-card class="box-card">
              <template #header>
                <div class="card-header"><span>📮 管理员反馈</span></div>
              </template>
              <div v-if="!isSuperAdmin" style="display:flex;gap:8px;margin-bottom:10px;">
                <el-input v-model="adminFeedbackText" placeholder="向超级管理员反馈问题" />
                <el-button type="primary" @click="submitAdminFeedback">提交</el-button>
              </div>
              <el-table :data="adminFeedbackList" stripe>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column v-if="isSuperAdmin" prop="reporterId" label="反馈管理员ID" width="120" />
                <el-table-column prop="content" label="内容" />
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    {{ row.status === 0 ? '待处理' : (row.status === 1 ? '已处理' : '驳回') }}
                  </template>
                </el-table-column>
                <el-table-column v-if="isSuperAdmin" label="操作" width="180">
                  <template #default="{ row }">
                    <el-button size="small" type="success" :disabled="row.status === 1" @click="updateFeedbackStatus(row, 1)">标记已处理</el-button>
                    <el-button size="small" type="danger" :disabled="row.status === 2" @click="updateFeedbackStatus(row, 2)">驳回</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

        </el-main>
      </el-container>
    </el-container>

    <el-drawer v-model="notifDrawerVisible" title="交易反馈通知" size="420px" destroy-on-close>
      <div class="notif-toolbar">
        <el-button type="primary" size="small" plain :disabled="unreadCount === 0" @click="handleMarkAllRead">
          全部标记已读
        </el-button>
        <el-button size="small" :icon="Refresh" @click="loadNotifications">刷新</el-button>
      </div>
      <p class="notif-hint">仅展示用户提交的「纠纷/求助」反馈。点击一条可查看订单与双方私信记录。</p>
      <el-empty v-if="!notifLoading && notifications.length === 0" description="暂无反馈" />
      <div v-loading="notifLoading" class="notif-list">
        <div
          v-for="n in notifications"
          :key="n.id"
          class="notif-item"
          :class="{ unread: Number(n.isRead) === 0 }"
          @click="handleNotifClick(n)"
        >
          <p class="notif-preview">{{ stripFeedbackPreview(n.preview) }}</p>
          <p class="notif-meta">
            <span v-if="n.orderId">订单 #{{ n.orderId }}</span>
            <span v-else>无订单号</span>
            · {{ formatNotifTime(n.createTime) }}
          </p>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="feedbackDetailVisible" title="订单与聊天记录" size="560px" destroy-on-close>
      <div v-loading="fbLoading" class="fb-detail">
        <template v-if="fbData && fbData.order">
          <el-descriptions title="订单信息" :column="1" border size="small">
            <el-descriptions-item label="订单号">{{ fbData.order.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商品">{{ fbData.order.productName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="金额">￥{{ fbData.order.totalAmount }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ orderStatusLabel(fbData.order.orderStatus) }}</el-descriptions-item>
            <el-descriptions-item label="买家 ID">{{ fbData.order.buyerId }}</el-descriptions-item>
            <el-descriptions-item label="卖家 ID">{{ fbData.order.sellerId }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="fb-section-title">用户提交的反馈</h4>
          <el-timeline v-if="(fbData.feedbacks || []).length">
            <el-timeline-item
              v-for="f in fbData.feedbacks"
              :key="f.id"
              :timestamp="f.createTime ? new Date(f.createTime).toLocaleString() : ''"
              placement="top"
            >
              <p class="fb-meta">用户 ID {{ f.userId }}</p>
              <p class="fb-content">{{ f.content }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="无反馈记录" />

          <h4 class="fb-section-title">买卖双方私信（仅此处对管理员可见）</h4>
          <div v-if="(fbData.messages || []).length" class="admin-chat">
            <div
              v-for="m in fbData.messages"
              :key="m.id"
              class="admin-chat-row"
              :class="isBuyerMessage(m) ? 'from-buyer' : 'from-seller'"
            >
              <div class="admin-chat-bubble">
                <span class="admin-chat-role">{{ isBuyerMessage(m) ? '买家' : '卖家' }}</span>
                <p>{{ m.content }}</p>
                <span class="admin-chat-time">{{ m.createTime ? new Date(m.createTime).toLocaleString() : '' }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无私信记录" />

          <h4 class="fb-section-title">管理员三方协调聊天室</h4>
          <div style="display:flex;gap:8px;margin-bottom:8px;">
            <el-input v-model="disputeReplyText" placeholder="输入协调意见并发送给买卖双方" />
            <el-button type="primary" @click="sendDisputeReply">发送</el-button>
          </div>
          <div v-if="disputeMessages.length" class="admin-chat">
            <div v-for="m in disputeMessages" :key="`d-${m.id}`" class="admin-chat-row">
              <div class="admin-chat-bubble">
                <span class="admin-chat-role">{{ m.senderRole }}#{{ m.senderId }}</span>
                <p>{{ m.content }}</p>
                <span class="admin-chat-time">{{ m.createTime ? new Date(m.createTime).toLocaleString() : '' }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无三方协调消息" />
        </template>
      </div>
    </el-drawer>
  </div>
  <el-dialog v-model="bannerFormVisible" title="轮播编辑" width="520px">
    <el-form :model="bannerForm" label-width="90px">
      <el-form-item label="标题"><el-input v-model="bannerForm.title" /></el-form-item>
      <el-form-item label="副标题"><el-input v-model="bannerForm.subtitle" /></el-form-item>
      <el-form-item label="背景色">
        <div style="display:flex;align-items:center;gap:10px;width:100%;">
          <el-color-picker v-model="bannerForm.bgColor" show-alpha />
          <el-input v-model="bannerForm.bgColor" placeholder="#a0cfff 或 rgba(...)" />
        </div>
      </el-form-item>
      <el-form-item label="排序"><el-input-number v-model="bannerForm.sortNo" :min="1" :max="9999" /></el-form-item>
      <el-form-item label="状态"><el-switch v-model="bannerForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="bannerFormVisible=false">取消</el-button><el-button type="primary" @click="saveBanner">保存</el-button></template>
  </el-dialog>

  <el-dialog v-model="noticeFormVisible" title="公告编辑" width="560px">
    <el-form :model="noticeForm" label-width="90px">
      <el-form-item label="标题"><el-input v-model="noticeForm.title" /></el-form-item>
      <el-form-item label="内容"><el-input v-model="noticeForm.content" type="textarea" :rows="4" /></el-form-item>
      <el-form-item label="状态"><el-switch v-model="noticeForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="noticeFormVisible=false">取消</el-button><el-button type="primary" @click="saveNotice">保存</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataBoard, Refresh, Picture, Bell, User, Goods, Tickets } from '@element-plus/icons-vue'
import request from '../utils/request'
import { resolveImageUrl } from '../utils/env'

const router = useRouter()
const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))
const isSuperAdmin = userInfo.value?.role === 'SUPER_ADMIN'

const notifDrawerVisible = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const notifLoading = ref(false)
let notifPollTimer = null

const formatNotifTime = (t) => (t ? new Date(t).toLocaleString() : '-')

const stripFeedbackPreview = (p) => {
  if (!p) return ''
  return String(p).replace(/^\[交易反馈\]\s*/, '')
}

const feedbackDetailVisible = ref(false)
const fbLoading = ref(false)
const fbData = ref(null)
const disputeMessages = ref<any[]>([])
const disputeReplyText = ref('')

const orderStatusLabel = (s) => {
  if (s === 0) return '待支付'
  if (s === 1) return '已支付 / 待发货'
  if (s === 2) return '已发货'
  return String(s ?? '-')
}

const isBuyerMessage = (m) => Number(m.senderId) === Number(fbData.value?.order?.buyerId)

const fetchUnreadCount = async () => {
  try {
    const n = await request.get('/user/admin/notifications/unread-count')
    unreadCount.value = typeof n === 'number' ? n : 0
  } catch (e) {
    console.error(e)
  }
}

const loadNotifications = async () => {
  notifLoading.value = true
  try {
    const list = await request.get('/user/admin/notifications', { params: { limit: 80 } })
    notifications.value = list || []
  } catch (e) {
    console.error(e)
  } finally {
    notifLoading.value = false
  }
}

const openNotifDrawer = async () => {
  notifDrawerVisible.value = true
  await loadNotifications()
  await fetchUnreadCount()
}

const handleNotifClick = async (n) => {
  if (Number(n.isRead) === 0) {
    try {
      await request.post(`/user/admin/notifications/read?id=${n.id}`)
      n.isRead = 1
      await fetchUnreadCount()
    } catch (e) {
      console.error(e)
    }
  }
  if (!n.orderId) {
    ElMessage.warning('该通知未关联订单')
    return
  }
  feedbackDetailVisible.value = true
  fbLoading.value = true
  fbData.value = null
  try {
    fbData.value = await request.get('/order/admin/feedback-order-view', { params: { orderId: n.orderId } })
    disputeMessages.value = await request.get('/order/admin/dispute/messages', { params: { orderId: n.orderId } }) || []
  } catch (e) {
    console.error(e)
    feedbackDetailVisible.value = false
  } finally {
    fbLoading.value = false
  }
}

const sendDisputeReply = async () => {
  const text = disputeReplyText.value.trim()
  if (!text || !fbData.value?.order?.id) return
  try {
    await request.post('/order/admin/dispute/send', { orderId: fbData.value.order.id, content: text })
    disputeReplyText.value = ''
    disputeMessages.value = await request.get('/order/admin/dispute/messages', { params: { orderId: fbData.value.order.id } }) || []
  } catch (e) {
    console.error(e)
  }
}

const handleMarkAllRead = async () => {
  try {
    await request.post('/user/admin/notifications/read-all')
    notifications.value.forEach((x) => { x.isRead = 1 })
    await fetchUnreadCount()
  } catch (e) {
    console.error(e)
  }
}

const activeMenu = ref('products')
const handleMenuSelect = (index) => {
  activeMenu.value = index
  if (index === 'products') fetchAdminProducts()
  if (index === 'users') fetchAdminUsers()
  if (index === 'orders') fetchAdminOrders()
  if (index === 'banners') fetchBanners()
  if (index === 'notices') fetchNotices()
  if (index === 'feedback') loadAdminFeedback()
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出管理员后台吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(() => {
    localStorage.clear() 
    router.push('/login')
    ElMessage.success('已退出登录')
  }).catch(() => {})
}

// --- 商品管理逻辑 ---
const adminProducts = ref([])
const loading = ref(false)
const prodPageNo = ref(1)
const pagedAdminProducts = computed(() => adminProducts.value.slice((prodPageNo.value - 1) * 10, prodPageNo.value * 10))

const getImageUrl = (url) => {
  return resolveImageUrl(url)
}

const fetchAdminProducts = async () => {
  loading.value = true
  try {
    const res = await request.get('/product/admin/all')
    adminProducts.value = res || []
  } catch (error) {
    console.error("获取商品失败:", error)
  } finally {
    loading.value = false
  }
}

const getStatusText = (row) => {
  if (row.stock <= 0 || row.publishStatus === 2) return '已售罄'
  if (row.auditStatus === 0) return '待审核'
  if (row.auditStatus === 2 || row.publishStatus === 0) return '违规下架'
  return '售卖中'
}
const getStatusTheme = (row) => {
  if (row.stock <= 0 || row.publishStatus === 2) return 'info'
  if (row.auditStatus === 0) return 'warning'
  if (row.auditStatus === 2 || row.publishStatus === 0) return 'danger'
  return 'success'
}

const handleStatusChange = async (row, auditTarget, publishTarget) => {
  try {
    await request.post(`/product/admin/audit?id=${row.id}&status=${auditTarget}`)
    await request.post(`/product/admin/status?id=${row.id}&status=${publishTarget}`)
    ElMessage.success('商品状态更新成功')
    fetchAdminProducts() 
  } catch (error) {
    console.error("更新失败", error)
  }
}

// --- 👉 新增：账号管理逻辑 ---
const adminUsers = ref([])
const userLoading = ref(false)
const userPageNo = ref(1)
const pagedAdminUsers = computed(() => adminUsers.value.slice((userPageNo.value - 1) * 10, userPageNo.value * 10))

// 获取全量用户
const fetchAdminUsers = async () => {
  userLoading.value = true
  try {
    // ⚠️ 注意：这里默认你后端有个 /user/admin/list 的接口，如果没有会报错
    const res = await request.get('/user/admin/list')
    adminUsers.value = res || []
  } catch (error) {
    console.error("获取用户数据失败:", error)
    // 兜底测试数据（防止你后端接口没写好时页面报错空白）
    if (adminUsers.value.length === 0) {
      ElMessage.warning('未能连接到用户接口，显示模拟数据')
      adminUsers.value = [
        { id: 1, username: 'admin', nickname: '系统管理员', role: 'ADMIN', status: 1 },
        { id: 2, username: 'student1', nickname: '张同学', role: 'USER', status: 1 },
        { id: 3, username: 'badguy', nickname: '违规小号', role: 'USER', status: 0 }
      ]
    }
  } finally {
    userLoading.value = false
  }
}

// 封号 / 解封
const handleUserStatus = async (row, targetStatus) => {
  try {
    // ⚠️ 注意：需要后端配合提供此接口
    await request.post(`/user/admin/status?id=${row.id}&status=${targetStatus}`)
    ElMessage.success(targetStatus === 1 ? '账号已解封' : '账号已封禁')
    fetchAdminUsers()
  } catch (error) {
    console.error("操作失败", error)
    row.status = targetStatus // 模拟前端变化
  }
}

// 提升管理员
const handleUserRole = async (row, targetRole) => {
  try {
    // ⚠️ 注意：需要后端配合提供此接口
    await request.post(`/user/admin/role?id=${row.id}&role=${targetRole}`)
    ElMessage.success('权限设置成功')
    fetchAdminUsers()
  } catch (error) {
    console.error("操作失败", error)
    row.role = targetRole // 模拟前端变化
  }
}

const adminOrders = ref([])
const orderLoading = ref(false)
const orderPageNo = ref(1)
const pagedAdminOrders = computed(() => adminOrders.value.slice((orderPageNo.value - 1) * 10, orderPageNo.value * 10))
const orderQuery = ref({
  orderNo: '',
  userKeyword: '',
  status: null
})
const stats = ref({ totalOrders: 0, totalAmount: 0, pendingOrders: 0, totalUsers: 0 })
const orderStatusSeries = computed(() => {
  const list = adminOrders.value || []
  const map = [
    { label: '待支付', key: 0 },
    { label: '待发货', key: 1 },
    { label: '已发货', key: 2 },
    { label: '已完成', key: 3 },
    { label: '已取消', key: 4 },
    { label: '退款', key: 5 }
  ]
  const total = Math.max(list.length, 1)
  return map.map((x) => {
    const c = list.filter((o) => Number(o.orderStatus) === x.key).length
    return { label: x.label, count: c, percent: Math.round((c / total) * 100) }
  })
})
const orderTrend7d = computed(() => {
  const list = adminOrders.value || []
  const now = new Date()
  const days = Array.from({ length: 7 }).map((_, i) => {
    const d = new Date(now)
    d.setDate(now.getDate() - (6 - i))
    const key = `${d.getMonth() + 1}/${d.getDate()}`
    const c = list.filter((o) => {
      if (!o.createTime) return false
      const t = new Date(o.createTime)
      return t.getFullYear() === d.getFullYear() && t.getMonth() === d.getMonth() && t.getDate() === d.getDate()
    }).length
    return { day: key, count: c }
  })
  const max = Math.max(...days.map((x) => x.count), 1)
  return days.map((x) => ({ ...x, percent: Math.round((x.count / max) * 100) }))
})

const fetchAdminOrders = async () => {
  orderLoading.value = true
  try {
    const params: any = {}
    if (orderQuery.value.orderNo?.trim()) params.orderNo = orderQuery.value.orderNo.trim()
    if (orderQuery.value.userKeyword?.trim()) params.userKeyword = orderQuery.value.userKeyword.trim()
    if (orderQuery.value.status !== null && orderQuery.value.status !== undefined) params.status = orderQuery.value.status
    const res = await request.get('/order/admin/list', { params })
    adminOrders.value = res || []
    const list = adminOrders.value || []
    stats.value.totalOrders = list.length
    stats.value.totalAmount = list.reduce((s, x) => s + Number(x.totalAmount || 0), 0).toFixed(2)
    stats.value.pendingOrders = list.filter((x) => Number(x.orderStatus) === 0 || Number(x.orderStatus) === 1).length
    stats.value.totalUsers = (adminUsers.value || []).length
  } catch (e) {
    console.error('获取订单失败', e)
  } finally {
    orderLoading.value = false
  }
}

const handleAdminOrderStatus = async (row, nextStatus) => {
  if (nextStatus === row.orderStatus) return
  try {
    await request.post(`/order/admin/status?orderId=${row.id}&status=${nextStatus}`)
    row.orderStatus = nextStatus
    ElMessage.success('订单状态已更新')
  } catch (e) {
    console.error(e)
    fetchAdminOrders()
  }
}

const approveRefund = async (row) => {
  try {
    await request.post(`/order/refund/approve?orderId=${row.id}`)
    ElMessage.success('已同意退款')
    fetchAdminOrders()
  } catch (e) {
    console.error(e)
  }
}

const rejectRefund = async (row) => {
  try {
    await request.post(`/order/refund/reject?orderId=${row.id}&rollbackStatus=1`)
    ElMessage.success('已拒绝退款')
    fetchAdminOrders()
  } catch (e) {
    console.error(e)
  }
}

const exportOrders = async () => {
  try {
    const base = (import.meta as any).env?.VITE_API_BASE || '/api'
    const token = localStorage.getItem('token') || ''
    const resp = await fetch(`${base}/order/admin/export`, {
      headers: { Authorization: token ? `Bearer ${token}` : '' }
    })
    const text = await resp.text()
    const blob = new Blob(["\uFEFF" + text], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `jemall-orders-${Date.now()}.csv`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error(e)
  }
}

const bannerList = ref([])
const bannerLoading = ref(false)
const bannerPageNo = ref(1)
const pagedBannerList = computed(() => bannerList.value.slice((bannerPageNo.value - 1) * 10, bannerPageNo.value * 10))
const bannerFormVisible = ref(false)
const bannerForm = ref<any>({ id: null, title: '', subtitle: '', bgColor: '#a0cfff', sortNo: 100, status: 1 })

const fetchBanners = async () => {
  bannerLoading.value = true
  try { bannerList.value = await request.get('/user/admin/banner/list') || [] } catch (e) { console.error(e) } finally { bannerLoading.value = false }
}
const openBannerForm = (row?: any) => {
  bannerForm.value = row ? { ...row } : { id: null, title: '', subtitle: '', bgColor: '#a0cfff', sortNo: 100, status: 1 }
  bannerFormVisible.value = true
}
const saveBanner = async () => {
  try {
    await request.post('/user/admin/banner/save', bannerForm.value)
    bannerFormVisible.value = false
    fetchBanners()
  } catch (e) {
    console.error(e)
  }
}
const deleteBanner = async (row) => {
  try {
    await request.post(`/user/admin/banner/delete?id=${row.id}`)
    fetchBanners()
  } catch (e) {
    console.error(e)
  }
}

const noticeListAdmin = ref([])
const noticeLoadingAdmin = ref(false)
const noticePageNo = ref(1)
const pagedNoticeListAdmin = computed(() => noticeListAdmin.value.slice((noticePageNo.value - 1) * 10, noticePageNo.value * 10))
const noticeFormVisible = ref(false)
const noticeForm = ref<any>({ id: null, title: '', content: '', status: 1 })
const adminFeedbackText = ref('')
const adminFeedbackList = ref<any[]>([])

const fetchNotices = async () => {
  noticeLoadingAdmin.value = true
  try { noticeListAdmin.value = await request.get('/user/admin/notice/list') || [] } catch (e) { console.error(e) } finally { noticeLoadingAdmin.value = false }
}
const openNoticeForm = (row?: any) => {
  noticeForm.value = row ? { ...row } : { id: null, title: '', content: '', status: 1 }
  noticeFormVisible.value = true
}
const saveNotice = async () => {
  try {
    await request.post('/user/admin/notice/save', noticeForm.value)
    noticeFormVisible.value = false
    fetchNotices()
  } catch (e) {
    console.error(e)
  }
}
const deleteNotice = async (row) => {
  try {
    await request.post(`/user/admin/notice/delete?id=${row.id}`)
    fetchNotices()
  } catch (e) {
    console.error(e)
  }
}

const loadAdminFeedback = async () => {
  try {
    if (isSuperAdmin) {
      adminFeedbackList.value = await request.get('/user/super-admin/feedback/list') || []
      return
    }
    adminFeedbackList.value = await request.get('/user/admin/feedback/mine') || []
  } catch (e) {
    console.error(e)
  }
}

const submitAdminFeedback = async () => {
  const t = adminFeedbackText.value.trim()
  if (!t) return
  try {
    await request.post('/user/admin/feedback/submit', { content: t })
    adminFeedbackText.value = ''
    loadAdminFeedback()
  } catch (e) {
    console.error(e)
  }
}

const updateFeedbackStatus = async (row, status) => {
  if (!isSuperAdmin) return
  try {
    await request.post(`/user/super-admin/feedback/status?id=${row.id}&status=${status}`)
    await loadAdminFeedback()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  fetchAdminProducts()
  fetchUnreadCount()
  notifPollTimer = setInterval(fetchUnreadCount, 8000)
})

onBeforeUnmount(() => {
  if (notifPollTimer) clearInterval(notifPollTimer)
})
</script>

<style scoped>
.admin-layout { height: 100vh; overflow: hidden; background-color: #f0f2f5; }

/* 顶栏样式 */
.admin-header { background-color: #2b3643; color: white; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); z-index: 10; }
.logo { display: flex; align-items: center; gap: 10px; font-size: 18px; font-weight: bold; letter-spacing: 1px; }
.header-right { display: flex; align-items: center; gap: 25px; }
.header-icon { color: #fff; cursor: pointer; transition: color 0.3s; }
.header-icon:hover { color: #409EFF; }
.msg-badge :deep(.el-badge__content) { top: 5px; right: 5px; }

/* 用户信息下拉菜单 */
.user-profile { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #fff; }
.user-profile:hover { opacity: 0.8; }
.nickname { font-size: 14px; }

/* 侧边栏样式 */
.admin-sidebar { background-color: #304156; transition: width 0.3s; }
.sidebar-menu { border-right: none; height: calc(100vh - 60px); }

/* 主内容区 */
.admin-main { padding: 20px; height: calc(100vh - 60px); overflow-y: auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-size: 16px; font-weight: bold; color: #333; }

.notif-toolbar { display: flex; gap: 10px; margin-bottom: 14px; }
.notif-list { display: flex; flex-direction: column; gap: 10px; min-height: 120px; }
.notif-item {
  padding: 12px 14px;
  border-radius: 8px;
  background: #f5f7fa;
  cursor: pointer;
  border: 1px solid transparent;
  transition: border-color 0.2s, background 0.2s;
}
.notif-item.unread {
  background: #ecf5ff;
  border-color: #b3d8ff;
}
.notif-item:hover { border-color: #dcdfe6; }
.notif-preview { margin: 0 0 8px; font-size: 14px; color: #303133; line-height: 1.5; word-break: break-word; }
.notif-hint { font-size: 12px; color: #909399; margin: 0 0 12px; line-height: 1.5; }
.fb-detail { min-height: 120px; }
.fb-section-title { margin: 20px 0 12px; font-size: 15px; color: #303133; }
.fb-meta { margin: 0 0 4px; font-size: 12px; color: #909399; }
.fb-content { margin: 0; white-space: pre-wrap; word-break: break-word; }
.admin-chat {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 8px 0;
  max-height: 360px;
  overflow-y: auto;
}
.admin-chat-row {
  display: flex;
  max-width: 92%;
}
.admin-chat-row.from-buyer {
  align-self: flex-start;
  justify-content: flex-start;
}
.admin-chat-row.from-seller {
  align-self: flex-end;
  justify-content: flex-end;
}
.admin-chat-bubble {
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.5;
}
.from-buyer .admin-chat-bubble {
  background: #ecf5ff;
  border: 1px solid #d9ecff;
}
.from-seller .admin-chat-bubble {
  background: #fdf6ec;
  border: 1px solid #faecd8;
}
.admin-chat-role { font-weight: 600; color: #606266; margin-right: 6px; }
.admin-chat-time { display: block; font-size: 11px; color: #c0c4cc; margin-top: 6px; }
.notif-meta { margin: 0; font-size: 12px; color: #909399; }
</style>
