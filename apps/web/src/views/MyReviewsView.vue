<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getReviews, deleteReview, type ReviewQueryParams } from "@/api/reviews";
import type { Review } from "@labreserve/shared";

const loading = ref(false);
const records = ref<Review[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);

const filters = reactive<ReviewQueryParams>({
  pageNum: 1,
  pageSize: 20,
});

async function fetchReviews() {
  loading.value = true;
  try {
    filters.pageNum = pageNum.value;
    filters.pageSize = pageSize.value;
    const page = await getReviews(filters);
    records.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function handlePageChange(p: number) {
  pageNum.value = p;
  fetchReviews();
}

function handleSizeChange(s: number) {
  pageSize.value = s;
  pageNum.value = 1;
  fetchReviews();
}

async function handleDelete(review: Review) {
  try {
    await ElMessageBox.confirm("确定要删除这条评价吗？", "确认删除", {
      type: "warning",
    });
    await deleteReview(Number(review.id));
    ElMessage.success("删除成功");
    fetchReviews();
  } catch {
    // user cancelled
  }
}

function ratingText(rating: number): string {
  const texts = ["", "非常差", "较差", "一般", "较好", "非常好"];
  return texts[rating] || "";
}

onMounted(() => {
  fetchReviews();
});
</script>

<template>
  <div class="my-reviews">
    <h1 class="page-title">我的评价</h1>

    <el-table v-loading="loading" :data="records" stripe style="width: 100%">
      <el-table-column prop="labName" label="实验室" min-width="150" />
      <el-table-column label="评分" width="200">
        <template #default="{ row }">
          <div class="rating-cell">
            <el-rate
              :model-value="row.rating"
              disabled
              show-score
              :texts="['非常差', '较差', '一般', '较好', '非常好']"
              text-color="#ff9900"
            />
            <span class="rating-label">{{ ratingText(row.rating) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="comment" label="评价内容" min-width="250" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="评价时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString("zh-CN") }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" link @click="handleDelete(row)"> 删除 </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.my-reviews {
  padding: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.rating-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-label {
  font-size: 12px;
  color: #999;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
