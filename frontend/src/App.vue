<template>
  <div id="app">
    <el-container class="app-container">
      <el-header class="app-header">
        <div class="header-content">
          <div class="logo" @click="$router.push('/')">
            <el-icon size="32"><Goods /></el-icon>
            <span>闲置好物置换市集</span>
          </div>
          <nav class="nav-menu">
            <el-menu :default-active="activeMenu" mode="horizontal" @select="handleMenuSelect">
              <el-menu-item index="/">首页</el-menu-item>
              <el-menu-item index="/market">物品市集</el-menu-item>
              <el-menu-item index="/publish">发布闲置</el-menu-item>
              <el-menu-item index="/my">我的库房</el-menu-item>
            </el-menu>
          </nav>
          <div class="user-area">
            <el-button type="primary" @click="showLogin = true" v-if="!userStore.isLoggedIn">
              登录
            </el-button>
            <el-dropdown v-else @command="handleUserCommand">
              <span class="user-name">
                <el-icon><User /></el-icon>
                {{ userStore.userInfo.nickname || '用户' }}
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
      <el-footer class="app-footer">
        <p>闲置好物置换市集 - 让闲置流动起来</p>
      </el-footer>
    </el-container>

    <el-dialog v-model="showLogin" title="用户登录" width="400px">
      <el-form :model="loginForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLogin = false">取消</el-button>
        <el-button type="primary" @click="handleLogin">登录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showLogin = ref(false)
const loginForm = ref({
  username: '',
  password: ''
})

const activeMenu = computed(() => route.path)

const handleMenuSelect = (index) => {
  router.push(index)
}

const handleLogin = () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  userStore.login(loginForm.value)
  showLogin.value = false
  ElMessage.success('登录成功')
}

const handleUserCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (command === 'profile') {
    router.push('/my')
  }
}
</script>

<style lang="scss">
#app {
  min-height: 100vh;
}

.app-container {
  min-height: 100vh;
}

.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0;
  height: 64px;
  line-height: 64px;

  .header-content {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 10px;
    color: white;
    font-size: 20px;
    font-weight: bold;
    cursor: pointer;
  }

  .nav-menu {
    :deep(.el-menu) {
      background: transparent;
      border-bottom: none;

      .el-menu-item {
        color: rgba(255, 255, 255, 0.9);

        &:hover,
        &.is-active {
          color: white;
          background: rgba(255, 255, 255, 0.2);
        }
      }
    }
  }

  .user-area {
    .user-name {
      color: white;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 5px;
    }
  }
}

.app-main {
  background: #f5f7fa;
  padding: 20px;
}

.app-footer {
  text-align: center;
  padding: 20px;
  background: #fff;
  color: #999;
  font-size: 14px;
}
</style>
// test cache
