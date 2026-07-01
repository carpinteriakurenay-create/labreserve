<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { createReview } from "@/api/reviews";

const props = defineProps<{
  bookingId: number;
}>();

const emit = defineEmits<{
  submitted: [];
  cancel: [];
}>();

const submitting = ref(false);
const rating = ref(0);
const comment = ref("");

async function handleSubmit() {
  if (rating.value === 0) {
    ElMessage.warning("请选择评分");
    return;
  }
  submitting.value = true;
  try {
    await createReview({
      bookingId: String(props.bookingId),
      rating: rating.value,
      comment: comment.value || undefined,
    });
    ElMessage.success("评价提交成功");
    emit("submitted");
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="review-form">
    <div class="form-item">
      <label class="form-label">评分</label>
      <el-rate v-model="rating" :max="5" show-score />
    </div>
    <div class="form-item">
      <label class="form-label">评价内容</label>
      <el-input
        v-model="comment"
        type="textarea"
        :rows="3"
        maxlength="1000"
        show-word-limit
        placeholder="分享一下你的使用体验（选填）"
      />
    </div>
    <div class="form-actions">
      <el-button @click="emit('cancel')">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit"> 提交评价 </el-button>
    </div>
  </div>
</template>

<style scoped>
.review-form {
  padding: 8px 0;
}

.form-item {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #303133;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
