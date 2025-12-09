<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <header class="p-3 mb-3 border-bottom bg-white shadow-sm">
    <div class="container">
      <div class="d-flex flex-wrap align-items-center justify-content-center justify-content-lg-start">
        <a href="/" class="d-flex align-items-center mb-2 mb-lg-0 text-dark text-decoration-none me-lg-auto">
          <span class="fs-4 fw-bold text-primary">ShopAPKD (Vue)</span>
        </a>

        <ul class="nav col-12 col-lg-auto me-lg-auto mb-2 justify-content-center mb-md-0">
          <li><router-link to="/" class="nav-link px-2 link-secondary">Trang chủ</router-link></li>
          <li><a href="#" class="nav-link px-2 link-dark">Sản phẩm</a></li>
          <li><a href="#" class="nav-link px-2 link-dark">Khuyến mãi</a></li>
        </ul>

        <div class="text-end">
          <button type="button" class="btn btn-outline-primary me-2">Đăng nhập</button>
          <button type="button" class="btn btn-primary">Đăng ký</button>
        </div>
      </div>
    </div>
  </header>

  <RouterView />
</template>