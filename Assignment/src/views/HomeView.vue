<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const products = ref([])
const loading = ref(true)

// Thêm các biến quản lý phân trang
const currentPage = ref(0) // Trang hiện tại (Spring thường bắt đầu từ 0)
const totalPages = ref(0)  // Tổng số trang

// Hàm format tiền tệ
const formatPrice = (price) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
}

// Hàm gọi API load sản phẩm theo số trang
const loadProducts = async (page) => {
  try {
    loading.value = true;
    
    // SỬA Ở ĐÂY: đổi 'p' thành 'page' cho khớp với Java Controller
    const response = await axios.get('/api/home', {
      params: { page: page } 
    });

    // Cập nhật dữ liệu từ Map trả về của Java
    products.value = response.data.products; 
    totalPages.value = response.data.totalPages; // Lấy tổng số trang từ Java
    currentPage.value = page;

  } catch (error) {
    console.error("Lỗi tải dữ liệu:", error);
  } finally {
    loading.value = false;
  }
}

// Gọi API khi component được load (mặc định trang 0)
onMounted(() => {
  loadProducts(0);
})
</script>

<template>
  <div class="container py-4">
    <div class="p-5 mb-4 bg-light rounded-3 text-center banner-bg text-white">
      <h1 class="display-5 fw-bold">Siêu Sale Tháng 10</h1>
      <p class="fs-4">Giảm giá lên đến 50% cho tất cả sản phẩm</p>
      <button class="btn btn-light btn-lg text-primary fw-bold" type="button">Mua Ngay</button>
    </div>

    <h2 class="mb-4 text-center">🔥 Sản phẩm nổi bật</h2>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <div v-else class="row row-cols-1 row-cols-md-3 row-cols-lg-4 g-4">
      <div class="col" v-for="product in products" :key="product.productId">
        <div class="card h-100 shadow-sm product-card">
          <img :src="product.imageUrl" class="card-img-top product-img" :alt="product.name" 
               @error="$event.target.src='https://via.placeholder.com/300x300?text=No+Image'">
          
          <div class="card-body d-flex flex-column">
            <h5 class="card-title text-truncate">{{ product.name }}</h5>
            
            <div class="mt-auto">
               <div class="d-flex justify-content-between align-items-center mb-3">
                <span class="fs-5 fw-bold text-primary">{{ formatPrice(product.price) }}</span>
                <span class="text-warning small">⭐ {{ product.rating }}</span>
              </div>
              <button class="btn btn-primary w-100">Thêm vào giỏ</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <nav v-if="totalPages > 1" class="mt-4" aria-label="Page navigation">
      <ul class="pagination justify-content-center">
        
        <li class="page-item" :class="{ disabled: currentPage === 0 }">
          <button class="page-link" @click="loadProducts(currentPage - 1)">
            &laquo; Trước
          </button>
        </li>

        <li v-for="page in totalPages" :key="page" 
            class="page-item" 
            :class="{ active: currentPage === (page - 1) }">
          <button class="page-link" @click="loadProducts(page - 1)">
            {{ page }}
          </button>
        </li>

        <li class="page-item" :class="{ disabled: currentPage === totalPages - 1 }">
          <button class="page-link" @click="loadProducts(currentPage + 1)">
            Sau &raquo;
          </button>
        </li>

      </ul>
    </nav>

  </div>
</template>