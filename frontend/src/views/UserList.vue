<template>
  <div class="container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>🚀 用户管理系统 - 实验演示</span>
          <el-button type="primary" size="small" @click="fetchData">刷新数据</el-button>
        </div>
      </template>

      <el-table :data="userList" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="createTime" label="创建时间">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from './utils/request' // 👈 关键！必须引入封装好的 request，而不是原生 axios
import { ElMessage } from 'element-plus'

const userList = ref([])

const fetchData = async () => {
  try {
    const res = await request.get('/user/all')
    
    // 加一个判断：如果 res 里有 data 字段，说明没拆包，我们手动拿出来
    if (res.data && Array.isArray(res.data)) {
      userList.value = res.data // 手动拿里面的数组
      console.log("手动拆包成功:", res.data)
    } else {
      userList.value = res // 如果已经是数组了，直接赋值
      console.log("拦截器似乎已经拆过了:", res)
    }
  } catch (error) {
    console.error("请求失败:", error)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.container {
  padding: 40px;
  background-color: #f0f2f5;
  min-height: 100vh;
}
.box-card {
  max-width: 900px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>