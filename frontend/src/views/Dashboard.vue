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
            <el-menu-item index="sellerOrders" class="menu-with-badge">
              <el-icon><Tickets /></el-icon>
              <span class="menu-label">商家订单处理</span>
              <el-badge v-if="sellerOrderUnread > 0" :value="sellerOrderUnread" :max="99" class="side-inline-badge" />
            </el-menu-item>
            <el-menu-item index="sellerMessages" class="menu-with-badge">
              <el-icon><Bell /></el-icon>
              <span class="menu-label">商家私信处理</span>
              <el-badge v-if="sellerMsgUnread > 0" :value="sellerMsgUnread" :max="99" class="side-inline-badge" />
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
            <el-menu-item index="reviews">
              <el-icon><Tickets /></el-icon>
              <span>商品评价管理</span>
            </el-menu-item>
            <el-menu-item index="categories">
              <el-icon><Goods /></el-icon>
              <span>商品分类管理</span>
            </el-menu-item>
            <el-menu-item index="account">
              <el-icon><User /></el-icon>
              <span>后台个人中心</span>
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
              <div style="display:flex;gap:10px;margin-bottom:12px;flex-wrap:wrap;">
                <el-input v-model="productKeyword" clearable placeholder="按商品名/描述/商家筛选" style="width:280px;" />
                <el-button @click="fetchAdminProducts">搜索</el-button>
                <el-button type="primary" @click="openProductForm()">新增商品</el-button>
                <el-button type="success" plain @click="exportProducts">导出商品CSV</el-button>
                <el-upload
                  :show-file-list="false"
                  accept=".csv"
                  :auto-upload="false"
                  :on-change="importProductsByCsv"
                >
                  <el-button type="warning" plain>批量导入CSV</el-button>
                </el-upload>
              </div>

              <el-table :data="pagedAdminProducts" stripe style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="ID" width="80" align="center" />
                <el-table-column label="主图" width="100" align="center">
                  <template #default="{ row }">
                    <el-image :src="getImageUrl(pickFirstImage(row.image, row.imageUrl))" style="width: 50px; height: 50px; border-radius: 4px;" fit="cover">
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
                <el-table-column label="商家" width="120" align="center">
                  <template #default="{ row }"><span>{{ row.sellerName || row.sellerId }}</span></template>
                </el-table-column>
                <el-table-column label="操作" min-width="200" align="center">
                  <template #default="{ row }">
                    <el-button type="primary" size="small" plain @click="openProductForm(row)">编辑</el-button>
                    <el-button type="danger" size="small" plain @click="deleteProduct(row)">删除</el-button>
                    <el-button v-if="getStatusText(row) === '售卖中'" type="danger" size="small" @click="handleStatusChange(row, 2, 0)">违规下架</el-button>
                    <el-button v-if="getStatusText(row) === '违规下架'" type="success" size="small" @click="handleStatusChange(row, 1, 1)">恢复上架</el-button>
                    <el-button disabled size="small" v-if="getStatusText(row) === '已售罄'">商品已售出</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div style="display:flex;justify-content:flex-end;margin-top:10px;">
                <el-pagination background layout="total, prev, pager, next" :total="filteredAdminProducts.length" v-model:current-page="prodPageNo" :page-size="10" />
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
              <div style="display:flex;gap:10px;margin-bottom:12px;flex-wrap:wrap;">
                <el-input v-model="userKeyword" clearable placeholder="按账号/昵称/手机号搜索" style="width:280px;" />
              </div>

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
                      {{ row.role === 'SUPER_ADMIN' ? '超级管理员' : (row.role === 'ADMIN' ? '系统管理员' : '普通用户') }}
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
                    <el-button type="primary" size="small" plain @click="openUserDetail(row)">详情</el-button>
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
                <el-pagination background layout="total, prev, pager, next" :total="filteredAdminUsers.length" v-model:current-page="userPageNo" :page-size="10" />
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
              <div style="display:grid;grid-template-columns:repeat(2,minmax(140px,1fr));gap:10px;margin-bottom:12px;">
                <el-card shadow="never"><div>今日订单</div><div style="font-size:22px;font-weight:700;">{{ stats.todayOrders }}</div></el-card>
                <el-card shadow="never"><div>今日销售额</div><div style="font-size:22px;font-weight:700;">￥{{ stats.todayAmount }}</div></el-card>
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
                <el-card shadow="never">
                  <div style="margin-bottom:8px;font-weight:600;">热销商品 TOP5</div>
                  <div v-for="x in hotProductsTop5" :key="x.productId" style="display:flex;align-items:center;gap:8px;margin:6px 0;">
                    <div style="width:180px;color:#606266;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ x.productName || `商品#${x.productId}` }}</div>
                    <el-progress :percentage="x.percent" :stroke-width="14" status="warning" />
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
                <el-table-column label="商家" min-width="160">
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

          <div v-if="activeMenu === 'sellerOrders'">
            <OrderList :admin-embedded="true" />
          </div>

          <div v-if="activeMenu === 'sellerMessages'">
            <MessageInbox :admin-embedded="true" />
          </div>

          <div v-if="activeMenu === 'banners'">
            <el-card class="box-card">
              <template #header><div class="card-header"><span>🖼 轮播管理</span><el-button v-if="isSuperAdmin" type="primary" @click="openBannerForm()">新增</el-button></div></template>
              <div style="display:flex;gap:10px;margin-bottom:12px;">
                <el-input v-model="bannerKeyword" clearable placeholder="按标题/副标题搜索轮播" style="width:280px;" />
              </div>
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
                <el-pagination background layout="total, prev, pager, next" :total="filteredBannerList.length" v-model:current-page="bannerPageNo" :page-size="10" />
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
              <el-divider content-position="left">用户意见反馈</el-divider>
              <el-table :data="userFeedbackList" stripe>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="userId" label="用户ID" width="100" />
                <el-table-column prop="content" label="反馈内容" />
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    {{ row.status === 0 ? '待处理' : (row.status === 1 ? '已处理' : '驳回') }}
                  </template>
                </el-table-column>
                <el-table-column prop="replyContent" label="回复" />
                <el-table-column label="操作" width="220">
                  <template #default="{ row }">
                    <el-button size="small" type="success" @click="handleUserFeedback(row, 1)">标记已处理</el-button>
                    <el-button size="small" type="danger" @click="handleUserFeedback(row, 2)">驳回</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

          <div v-if="activeMenu === 'reviews'">
            <el-card class="box-card">
              <template #header><div class="card-header"><span>⭐ 商品评价管理</span><el-button type="success" :icon="Refresh" circle @click="fetchAdminReviews" /></div></template>
              <el-table :data="adminReviewList" stripe v-loading="reviewLoading">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="productName" label="商品" min-width="180" />
                <el-table-column label="用户" width="140">
                  <template #default="{row}">{{ row.nickname || ('用户#'+row.userId) }}</template>
                </el-table-column>
                <el-table-column prop="rating" label="评分" width="90" />
                <el-table-column prop="content" label="内容" show-overflow-tooltip />
                <el-table-column label="时间" width="180">
                  <template #default="{row}">{{ row.createTime ? new Date(row.createTime).toLocaleString() : '-' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="{row}">
                    <el-button size="small" type="danger" @click="deleteAdminReview(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

          <div v-if="activeMenu === 'categories'">
            <el-card class="box-card">
              <template #header><div class="card-header"><span>🗂 商品分类管理</span><el-button type="primary" @click="openCategoryForm()">新增分类</el-button></div></template>
              <el-table :data="categoryList" stripe>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="icon" label="图标" width="80" />
                <el-table-column prop="name" label="名称" />
                <el-table-column prop="sortNo" label="排序" width="90" />
                <el-table-column label="状态" width="90"><template #default="{row}">{{ Number(row.status)===1?'上线':'下线' }}</template></el-table-column>
                <el-table-column label="操作" width="180">
                  <template #default="{row}">
                    <el-button size="small" @click="openCategoryForm(row)">编辑</el-button>
                    <el-button size="small" type="danger" @click="deleteCategory(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

          <div v-if="activeMenu === 'account'">
            <el-card class="box-card">
              <template #header><div class="card-header"><span>🙋 后台个人中心</span></div></template>
              <el-form :model="accountForm" label-width="100px" style="max-width:560px;">
                <el-form-item label="登录账号"><el-input v-model="accountForm.username" disabled /></el-form-item>
                <el-form-item label="昵称"><el-input v-model="accountForm.nickname" /></el-form-item>
                <el-form-item label="手机号"><el-input v-model="accountForm.phone" /></el-form-item>
                <el-form-item label="收货地址"><el-input v-model="accountForm.campusAddress" /></el-form-item>
                <el-form-item><el-button type="primary" @click="saveAccountProfile">保存资料</el-button></el-form-item>
              </el-form>
              <el-divider />
              <el-form :model="pwdForm" label-width="100px" style="max-width:560px;">
                <el-form-item label="旧密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
                <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
                <el-form-item><el-button type="warning" @click="changeMyPassword">修改密码</el-button></el-form-item>
              </el-form>
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
      <p class="notif-hint">仅展示用户提交的「纠纷/求助」反馈。点击一条可查看订单与双方消息记录。</p>
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
            <el-descriptions-item label="商家 ID">{{ fbData.order.sellerId }}</el-descriptions-item>
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

          <h4 class="fb-section-title">买卖双方消息（仅此处对管理员可见）</h4>
          <div v-if="(fbData.messages || []).length" class="admin-chat">
            <div
              v-for="m in fbData.messages"
              :key="m.id"
              class="admin-chat-row"
              :class="isBuyerMessage(m) ? 'from-buyer' : 'from-seller'"
            >
              <div class="admin-chat-bubble">
                <span class="admin-chat-role">{{ isBuyerMessage(m) ? '买家' : '商家' }}</span>
                <p>{{ m.content }}</p>
                <span class="admin-chat-time">{{ m.createTime ? new Date(m.createTime).toLocaleString() : '' }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无消息记录" />

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
  <el-dialog v-model="userDetailVisible" title="用户详情" width="520px">
    <el-descriptions :column="1" border size="small" v-if="currentUserDetail">
      <el-descriptions-item label="用户ID">{{ currentUserDetail.id }}</el-descriptions-item>
      <el-descriptions-item label="账号">{{ currentUserDetail.username || '-' }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ currentUserDetail.nickname || '-' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ currentUserDetail.phone || '-' }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ currentUserDetail.role || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ Number(currentUserDetail.status) === 1 ? '正常' : '已封禁' }}</el-descriptions-item>
      <el-descriptions-item label="收货地址">{{ currentUserDetail.campusAddress || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
  <el-dialog v-model="productFormVisible" title="商品编辑" width="620px">
    <el-form :model="productForm" label-width="96px">
      <el-form-item label="商品名称"><el-input v-model="productForm.name" /></el-form-item>
      <el-form-item label="商品分类">
        <el-select v-model="productForm.category" style="width:100%;">
          <el-option label="数码电子" value="数码电子" />
          <el-option label="书籍资料" value="书籍资料" />
          <el-option label="衣物鞋帽" value="衣物鞋帽" />
          <el-option label="生活用品" value="生活用品" />
          <el-option label="其他商品" value="其他商品" />
        </el-select>
      </el-form-item>
      <el-form-item label="价格"><el-input-number v-model="productForm.price" :min="0.01" :precision="2" /></el-form-item>
      <el-form-item label="原价"><el-input-number v-model="productForm.originalPrice" :min="0" :precision="2" /></el-form-item>
      <el-form-item label="库存"><el-input-number v-model="productForm.stock" :min="0" /></el-form-item>
      <el-form-item label="商品图片">
        <el-upload
          :action="uploadActionUrl"
          :headers="uploadHeaders"
          list-type="picture-card"
          :file-list="productImageFileList"
          :limit="9"
          :on-success="handleProductImageUploadSuccess"
          :on-remove="handleProductImageRemove"
          :on-preview="handleProductImagePreview"
          :on-exceed="() => ElMessage.warning('最多上传 9 张图片')"
          accept="image/jpeg,image/jpg,image/png,image/webp,image/gif"
          name="file"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div v-if="productImageFileList.length > 1" class="drag-tip">拖动下方缩略图可调整顺序（第一张为主图）</div>
        <div v-if="productImageFileList.length > 1" class="drag-row">
          <div
            v-for="(f, idx) in productImageFileList"
            :key="f.uid || f.rawUrl || f.url || idx"
            class="drag-item"
            draggable="true"
            @dragstart="onImageDragStart(idx)"
            @dragover.prevent
            @drop="onImageDrop(idx)"
          >
            <img :src="getImageUrl(f.rawUrl || f.response?.data || f.url)" />
            <span class="drag-index">{{ idx + 1 }}</span>
          </div>
        </div>
        <div style="font-size:12px;color:#909399;">支持本地上传，最多 9 张；第一张将作为商品主图。</div>
      </el-form-item>
      <el-form-item label="商品描述"><el-input v-model="productForm.description" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="上架状态"><el-switch v-model="productForm.publishStatus" :active-value="1" :inactive-value="0" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="productFormVisible=false">取消</el-button>
      <el-button type="primary" @click="saveProduct">保存</el-button>
    </template>
  </el-dialog>
  <el-dialog v-model="categoryFormVisible" title="分类编辑" width="520px">
    <el-form :model="categoryForm" label-width="90px">
      <el-form-item label="分类名称"><el-input v-model="categoryForm.name" /></el-form-item>
      <el-form-item label="图标"><el-input v-model="categoryForm.icon" placeholder="如：💻" /></el-form-item>
      <el-form-item label="排序"><el-input-number v-model="categoryForm.sortNo" :min="1" :max="9999" /></el-form-item>
      <el-form-item label="状态"><el-switch v-model="categoryForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="categoryFormVisible=false">取消</el-button>
      <el-button type="primary" @click="saveCategory">保存</el-button>
    </template>
  </el-dialog>
  <el-dialog v-model="productImagePreviewVisible" title="图片预览" width="640px">
    <img v-if="productImagePreviewUrl" :src="productImagePreviewUrl" style="width:100%;max-height:70vh;object-fit:contain;" />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataBoard, Refresh, Picture, Bell, User, Goods, Tickets, Plus } from '@element-plus/icons-vue'
import request from '../utils/request'
import { resolveImageUrl, uploadActionUrl } from '../utils/env'
import OrderList from './OrderList.vue'
import MessageInbox from './MessageInbox.vue'

const router = useRouter()
const route = useRoute()
const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))
const isSuperAdmin = userInfo.value?.role === 'SUPER_ADMIN'

const notifDrawerVisible = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const sellerMsgUnread = ref(0)
const sellerOrderUnread = ref(0)
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
    const msg = (e as any)?.message || '商品保存失败，请稍后重试'
    ElMessage.error(msg)
  }
}

const fetchSellerMsgUnread = async () => {
  try {
    const n = await request.get('/order/chat/unread-count')
    sellerMsgUnread.value = Number(n || 0)
  } catch (e) {
    sellerMsgUnread.value = 0
  }
}

const fetchSellerOrderUnread = async () => {
  try {
    const n = await request.get('/order/notice/unread-count')
    sellerOrderUnread.value = Number(n?.seller || 0)
  } catch (e) {
    sellerOrderUnread.value = 0
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
  if (index === 'sellerOrders') {
    window.dispatchEvent(new CustomEvent('admin-open-seller-orders'))
  }
  if (index === 'sellerMessages') {
    window.dispatchEvent(new CustomEvent('admin-open-seller-messages'))
  }
  if (index === 'banners') fetchBanners()
  if (index === 'notices') fetchNotices()
  if (index === 'feedback') loadAdminFeedback()
  if (index === 'reviews') fetchAdminReviews()
  if (index === 'categories') fetchCategories()
  if (index === 'account') loadAccountProfile()
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
const productKeyword = ref('')
const filteredAdminProducts = computed(() => adminProducts.value)
const pagedAdminProducts = computed(() => filteredAdminProducts.value.slice((prodPageNo.value - 1) * 10, prodPageNo.value * 10))
const productFormVisible = ref(false)
const productForm = ref<any>({ id: null, name: '', description: '', category: '其他商品', price: 1, originalPrice: 0, stock: 1, image: '', imageUrl: '', publishStatus: 1 })
const productImageFileList = ref<any[]>([])
const productImagePreviewVisible = ref(false)
const productImagePreviewUrl = ref('')
const draggingImageIndex = ref<number | null>(null)
const localToken = localStorage.getItem('token') || localStorage.getItem('jiejie_assignment_token') || ''
const uploadHeaders = localToken ? { Authorization: `Bearer ${localToken}` } : {}

const getImageUrl = (url) => {
  return resolveImageUrl(url)
}
const pickFirstImage = (image: any, imageUrl: any) => {
  const parse = (v: any): string[] => {
    if (!v) return []
    if (Array.isArray(v)) return v.filter(Boolean)
    const s = String(v)
    if (s.startsWith('[')) {
      try { return (JSON.parse(s) || []).filter(Boolean) } catch { return [] }
    }
    return [s]
  }
  return [...parse(image), ...parse(imageUrl)][0] || ''
}

const fetchAdminProducts = async () => {
  loading.value = true
  try {
    const res = await request.get('/product/admin/all')
    const k = productKeyword.value.trim().toLowerCase()
    const list = res || []
    adminProducts.value = !k ? list : list.filter((x: any) => {
      const t = `${x.name || ''} ${x.description || ''} ${x.sellerName || ''}`.toLowerCase()
      return t.includes(k)
    })
    prodPageNo.value = 1
  } catch (error) {
    console.error("获取商品失败:", error)
  } finally {
    loading.value = false
  }
}
const openProductForm = (row?: any) => {
  const parseImages = (x: any) => {
    if (!x) return []
    if (Array.isArray(x)) return x
    const s = String(x)
    if (s.startsWith('[')) {
      try { return JSON.parse(s) } catch { return [] }
    }
    return [s]
  }
  const imgs = row ? Array.from(new Set([...(parseImages(row.imageUrl)), ...(parseImages(row.image))].filter(Boolean))) : []
  productForm.value = row ? {
    id: row.id,
    name: row.name || '',
    description: row.description || '',
    category: row.category || '其他商品',
    price: Number(row.price || 1),
    originalPrice: Number(row.originalPrice || 0),
    stock: Number(row.stock || 1),
    image: imgs[0] || '',
    imageUrl: imgs.length ? JSON.stringify(imgs) : '',
    publishStatus: Number(row.publishStatus ?? 1),
    auditStatus: Number(row.auditStatus ?? 1),
    sellerId: row.sellerId
  } : { id: null, name: '', description: '', category: '其他商品', price: 1, originalPrice: 0, stock: 1, image: '', imageUrl: '', publishStatus: 1, auditStatus: 1 }
  productImageFileList.value = imgs.map((u: string, i: number) => ({ name: `img-${i + 1}`, url: getImageUrl(u), rawUrl: u }))
  productFormVisible.value = true
}
const saveProduct = async () => {
  try {
    const normalizeServerPath = (v: any) => {
      const s = String(v || '').trim()
      if (!s) return ''
      // 只接受服务端返回的图片路径，防止把 blob/base64 临时地址提交到后端
      if (s.startsWith('/images/')) return s
      return ''
    }
    const imgs = productImageFileList.value
      .map((f: any) => normalizeServerPath(f.rawUrl || f.response?.data || f.url))
      .filter(Boolean)
    const localPending = (productImageFileList.value || []).some((f: any) => {
      const status = String(f?.status || '')
      if (status === 'uploading') return true
      const maybePath = normalizeServerPath(f?.rawUrl || f?.response?.data || f?.url)
      return !maybePath
    })

    if (localPending) {
      return ElMessage.warning('有图片未上传成功，请等待上传完成后再保存')
    }
    const payload: any = {
      ...productForm.value,
      name: String(productForm.value?.name || '').trim(),
      category: String(productForm.value?.category || '').trim(),
      description: String(productForm.value?.description || '').trim(),
      price: Number(productForm.value?.price || 0),
      originalPrice: Number(productForm.value?.originalPrice || 0),
      stock: Number(productForm.value?.stock ?? 1),
      image: imgs[0] || '',
      imageUrl: imgs.length ? JSON.stringify(imgs) : '',
      publishStatus: Number(productForm.value?.publishStatus ?? 1),
      auditStatus: Number(productForm.value?.auditStatus ?? 1)
    }

    if (!payload.name) {
      return ElMessage.warning('请填写商品名称')
    }
    if (!payload.category) {
      return ElMessage.warning('请选择商品分类')
    }
    if (!(payload.price > 0)) {
      return ElMessage.warning('价格必须大于 0')
    }

    await request.post('/product/admin/save', payload)
    productFormVisible.value = false
    await fetchAdminProducts()
    ElMessage.success('商品保存成功')
  } catch (e) {
    console.error(e)
    const msg = (e as any)?.message || '商品保存失败'
    ElMessage.error(msg)
  }
}

const handleProductImageUploadSuccess = (resp: any, file: any, fileList: any[]) => {
  const path = resp?.data || ''
  productImageFileList.value = fileList.map((f: any) => ({ ...f, rawUrl: f.rawUrl || f.response?.data || path || f.url }))
}

const handleProductImageRemove = (file: any, fileList: any[]) => {
  productImageFileList.value = fileList.map((f: any) => ({ ...f, rawUrl: f.rawUrl || f.response?.data || f.url }))
}

const handleProductImagePreview = (file: any) => {
  const src = file?.rawUrl || file?.response?.data || file?.url || ''
  if (!src) return
  productImagePreviewUrl.value = getImageUrl(src)
  productImagePreviewVisible.value = true
}

const onImageDragStart = (idx: number) => {
  draggingImageIndex.value = idx
}

const onImageDrop = (targetIdx: number) => {
  const fromIdx = draggingImageIndex.value
  draggingImageIndex.value = null
  if (fromIdx == null || fromIdx === targetIdx) return
  const list = [...productImageFileList.value]
  const [moved] = list.splice(fromIdx, 1)
  list.splice(targetIdx, 0, moved)
  productImageFileList.value = list
}

const csvEscape = (value: any) => `"${String(value || '').replace(/"/g, '""')}"`

const exportProducts = () => {
  const rows = filteredAdminProducts.value || []
  const header = ['id', 'name', 'category', 'price', 'originalPrice', 'stock', 'description', 'image', 'publishStatus', 'auditStatus']
  const lines = [header.join(',')]
  rows.forEach((x: any) => {
    const row = [
      x.id ?? '',
      csvEscape(x.name),
      csvEscape(x.category),
      x.price ?? '',
      x.originalPrice ?? '',
      x.stock ?? '',
      csvEscape(x.description),
      csvEscape(x.image || x.imageUrl),
      x.publishStatus ?? 1,
      x.auditStatus ?? 1
    ]
    lines.push(row.join(','))
  })
  const blob = new Blob(["\uFEFF" + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `jemall-products-${Date.now()}.csv`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

const importProductsByCsv = async (file: any) => {
  const raw = file?.raw as File
  if (!raw) return
  const text = await raw.text()
  const lines = text.split(/\r?\n/).filter(Boolean)
  if (lines.length <= 1) return ElMessage.warning('CSV 内容为空')
  const parseLine = (line: string) => {
    const out: string[] = []
    let cur = ''
    let q = false
    for (let i = 0; i < line.length; i++) {
      const ch = line[i]
      if (ch === '"') {
        if (q && line[i + 1] === '"') { cur += '"'; i++ } else { q = !q }
      } else if (ch === ',' && !q) {
        out.push(cur); cur = ''
      } else {
        cur += ch
      }
    }
    out.push(cur)
    return out
  }
  const header = parseLine(lines[0]).map((x) => x.trim())
  const idx = (k: string) => header.indexOf(k)
  let ok = 0
  for (let i = 1; i < lines.length; i++) {
    const cols = parseLine(lines[i])
    const body: any = {
      name: (cols[idx('name')] || '').trim(),
      category: (cols[idx('category')] || '其他商品').trim(),
      price: Number(cols[idx('price')] || 0),
      originalPrice: Number(cols[idx('originalPrice')] || 0),
      stock: Number(cols[idx('stock')] || 1),
      description: (cols[idx('description')] || '').trim(),
      image: (cols[idx('image')] || '').trim(),
      publishStatus: Number(cols[idx('publishStatus')] || 1),
      auditStatus: Number(cols[idx('auditStatus')] || 1)
    }
    if (!body.name || body.price <= 0) continue
    try {
      await request.post('/product/admin/save', body)
      ok++
    } catch (e) {
      console.error('导入单行失败', i + 1, e)
    }
  }
  ElMessage.success(`导入完成：成功 ${ok} 条`)
  await fetchAdminProducts()
}
const deleteProduct = async (row) => {
  try {
    await request.post(`/product/admin/delete?id=${row.id}`)
    await fetchAdminProducts()
  } catch (e) {
    console.error(e)
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
const userDetailVisible = ref(false)
const currentUserDetail = ref<any>(null)
const userKeyword = ref('')
const filteredAdminUsers = computed(() => adminUsers.value)
const pagedAdminUsers = computed(() => filteredAdminUsers.value.slice((userPageNo.value - 1) * 10, userPageNo.value * 10))

// 获取全量用户
const fetchAdminUsers = async () => {
  userLoading.value = true
  try {
    const params: any = {}
    if (userKeyword.value.trim()) params.keyword = userKeyword.value.trim()
    const res = await request.get('/user/admin/list', { params })
    adminUsers.value = res || []
    userPageNo.value = 1
  } catch (error) {
    console.error("获取用户数据失败:", error)
    // 兜底测试数据（防止你后端接口没写好时页面报错空白）
    if (adminUsers.value.length === 0) {
      ElMessage.warning('未能连接到用户接口，显示模拟数据')
      adminUsers.value = [
        { id: 1, username: 'admin', nickname: '系统管理员', role: 'ADMIN', status: 1 },
        { id: 2, username: 'student1', nickname: '普通用户A', role: 'USER', status: 1 },
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

const openUserDetail = (row) => {
  currentUserDetail.value = row
  userDetailVisible.value = true
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
const stats = ref({ totalOrders: 0, totalAmount: 0, pendingOrders: 0, totalUsers: 0, todayOrders: 0, todayAmount: 0 })
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
const hotProductsTop5 = computed(() => {
  const map: Record<string, { productId: number, productName: string, count: number }> = {}
  ;(adminOrders.value || []).forEach((o: any) => {
    const pid = Number(o.productId || 0)
    if (!pid) return
    const key = String(pid)
    if (!map[key]) {
      map[key] = { productId: pid, productName: o.productName || '', count: 0 }
    }
    map[key].count += Number(o.buyCount || 0)
  })
  const arr = Object.values(map).sort((a, b) => b.count - a.count).slice(0, 5)
  const max = Math.max(...arr.map((x) => x.count), 1)
  return arr.map((x) => ({ ...x, percent: Math.round((x.count / max) * 100) }))
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
    const now = new Date()
    const todayList = list.filter((x) => {
      if (!x.createTime) return false
      const t = new Date(x.createTime)
      return t.getFullYear() === now.getFullYear() && t.getMonth() === now.getMonth() && t.getDate() === now.getDate()
    })
    stats.value.todayOrders = todayList.length
    stats.value.todayAmount = Number(todayList.reduce((s, x) => s + Number(x.totalAmount || 0), 0).toFixed(2))
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
const bannerKeyword = ref('')
const filteredBannerList = computed(() => {
  const k = bannerKeyword.value.trim().toLowerCase()
  if (!k) return bannerList.value
  return (bannerList.value || []).filter((x: any) => `${x.title || ''} ${x.subtitle || ''}`.toLowerCase().includes(k))
})
const pagedBannerList = computed(() => filteredBannerList.value.slice((bannerPageNo.value - 1) * 10, bannerPageNo.value * 10))
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
const userFeedbackList = ref<any[]>([])

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
    } else {
      adminFeedbackList.value = await request.get('/user/admin/feedback/mine') || []
    }
    userFeedbackList.value = await request.get('/user/admin/feedback/user-list') || []
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

const adminReviewList = ref<any[]>([])
const reviewLoading = ref(false)
const fetchAdminReviews = async () => {
  reviewLoading.value = true
  try {
    adminReviewList.value = await request.get('/product/admin/review/list') || []
  } catch (e) {
    console.error(e)
  } finally {
    reviewLoading.value = false
  }
}
const deleteAdminReview = async (row) => {
  try {
    await request.post(`/product/admin/review/delete?id=${row.id}`)
    await fetchAdminReviews()
  } catch (e) {
    console.error(e)
  }
}

const categoryList = ref<any[]>([])
const categoryFormVisible = ref(false)
const categoryForm = ref<any>({ id: null, name: '', icon: '📦', sortNo: 100, status: 1 })
const fetchCategories = async () => {
  try {
    categoryList.value = await request.get('/product/admin/category/list') || []
  } catch (e) {
    console.error(e)
  }
}
const openCategoryForm = (row?: any) => {
  categoryForm.value = row ? { ...row } : { id: null, name: '', icon: '📦', sortNo: 100, status: 1 }
  categoryFormVisible.value = true
}
const saveCategory = async () => {
  try {
    await request.post('/product/admin/category/save', categoryForm.value)
    categoryFormVisible.value = false
    fetchCategories()
  } catch (e) {
    console.error(e)
  }
}
const deleteCategory = async (row) => {
  try {
    await request.post(`/product/admin/category/delete?id=${row.id}`)
    fetchCategories()
  } catch (e) {
    console.error(e)
  }
}

const accountForm = ref<any>({ username: '', nickname: '', phone: '', campusAddress: '' })
const pwdForm = ref<any>({ oldPassword: '', newPassword: '' })
const loadAccountProfile = async () => {
  try {
    const data = await request.get('/user/profile')
    accountForm.value = {
      username: data.username || '',
      nickname: data.nickname || '',
      phone: data.phone || '',
      campusAddress: data.campusAddress || ''
    }
  } catch (e) {
    console.error(e)
  }
}
const saveAccountProfile = async () => {
  try {
    const updated = await request.put('/user/profile', accountForm.value)
    localStorage.setItem('userInfo', JSON.stringify(updated))
    ElMessage.success('资料已保存')
  } catch (e) {
    console.error(e)
  }
}
const changeMyPassword = async () => {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) return
  try {
    await request.post('/auth/change-password', {
      username: accountForm.value.username,
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    pwdForm.value.oldPassword = ''
    pwdForm.value.newPassword = ''
    ElMessage.success('密码已修改，请重新登录')
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

const handleUserFeedback = async (row, status) => {
  try {
    const replyContent = status === 1 ? '已处理，感谢反馈' : '不符合处理条件'
    await request.post(`/user/admin/feedback/user-status?id=${row.id}&status=${status}&replyContent=${encodeURIComponent(replyContent)}`)
    await loadAdminFeedback()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  const panel = String(route.query?.panel || '')
  if (panel === 'sellerOrders' || panel === 'sellerMessages') {
    activeMenu.value = panel
  }
  fetchAdminProducts()
  fetchAdminUsers()
  fetchUnreadCount()
  fetchSellerMsgUnread()
  fetchSellerOrderUnread()
  notifPollTimer = setInterval(() => {
    fetchUnreadCount()
    fetchSellerMsgUnread()
    fetchSellerOrderUnread()
  }, 8000)
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
.admin-sidebar { overflow: visible; }
.sidebar-menu { overflow: visible; }
.menu-with-badge { overflow: visible; }
.menu-label { margin-right: 8px; }
.side-inline-badge { display: inline-flex; align-items: center; transform: translateY(1px); }
.side-inline-badge :deep(.el-badge__content) { position: static; transform: none; margin-left: 0; }
.drag-tip { margin-top: 8px; font-size: 12px; color: #909399; }
.drag-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.drag-item { width: 64px; height: 64px; border: 1px dashed #dcdfe6; border-radius: 6px; position: relative; overflow: hidden; cursor: move; background: #fff; }
.drag-item img { width: 100%; height: 100%; object-fit: cover; }
.drag-index { position: absolute; left: 4px; top: 4px; background: rgba(0,0,0,.55); color: #fff; border-radius: 10px; font-size: 11px; line-height: 1; padding: 2px 6px; }

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

@media (max-width: 900px) {
  .admin-main { padding: 10px; }
  .card-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-dialog) { width: 94vw !important; }
  :deep(.el-drawer) { width: 94vw !important; }
}
</style>
