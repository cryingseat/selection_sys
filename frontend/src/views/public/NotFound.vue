<script setup>
import Icon from '../../components/Icon.vue'
</script>

<template>
  <div style="min-height: 100vh; display: grid; place-items: center; padding: 24px">
    <div class="card card-pad center" style="max-width: 460px; width: 100%">
      <div class="empty-icon" style="width: 64px; height: 64px"><Icon name="search" :size="28" /></div>
      <h1 style="font-size: 44px; color: var(--primary)">404</h1>
      <h3 class="mt-8">页面走丢了</h3>
      <p class="muted small mt-4">你访问的页面不存在，或者已经被移动。</p>
      <div class="button-row center mt-24" style="justify-content: center">
        <router-link to="/" class="btn btn-primary">返回首页</router-link>
        <router-link to="/login" class="btn btn-ghost">去登录</router-link>
      </div>
    </div>
  </div>
</template>
