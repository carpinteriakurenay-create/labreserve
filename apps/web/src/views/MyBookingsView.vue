<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { BookingStatus, BOOKING_STATUS_LABELS } from "@labreserve/shared";
import type { Review } from "@labreserve/shared";
import * as bookingsApi from "@/api/bookings";
import { getReviewByBooking } from "@/api/reviews";
import ReviewForm from "@/components/ReviewForm.vue";
import type { BookingPage } from "@/api/bookings";

const loading = ref(false);
const page = ref<BookingPage | null>(null);

const filters = reactive({
  status: "",
  pageNum: 1,
  pageSize: 10,
});

const statusOptions = [
  { label: "全部", value: "" },
  { label: "待审核", value: BookingStatus.PENDING },
  { label: "已通过", value: BookingStatus.APPROVED },
  { label: "已拒绝", value: BookingStatus.REJECTED },
  { label: "已取消", value: BookingStatus.CANCELLED },
  { label: "已完成", value: BookingStatus.COMPLETED },
];

const statusType: Record<string, "warning" | "success" | "danger" | "info" | ""> = {
  [BookingStatus.PENDING]: "warning",
  [BookingStatus.APPROVED]: "success",
  [BookingStatus.REJECTED]: "danger",
  [BookingStatus.CANCELLED]: "info",
  [BookingStatus.COMPLETED]: "",
};

const cancellableStatuses = [BookingStatus.PENDING, BookingStatus.APPROVED];

// Review state
const reviewDialogVisible = ref(false);
const selectedBookingId = ref(0);
const reviewedBookings = ref<Record<string, Review>>({});

async function fetchBookings() {
  loading.value = true;
  try {
    page.value = await bookingsApi.getMyBookings({
      status: filters.status || undefined,
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
    });
    await fetchReviewsForCompleted();
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
}

async function fetchReviewsForCompleted() {
  if (!page.value) return;
  const completedBookings = page.value.records.filter((b) => b.status === BookingStatus.COMPLETED);
  if (completedBookings.length === 0) return;

  // Parallel fetch instead of sequential N+1
  const results = await Promise.allSettled(
    completedBookings.map((b) => getReviewByBooking(Number(b.id))),
  );
  results.forEach((result, i) => {
    if (result.status === "fulfilled" && result.value) {
      reviewedBookings.value[completedBookings[i]!.id] = result.value;
    }
  });
}

function handleSearch() {
  filters.pageNum = 1;
  fetchBookings();
}

function handleReset() {
  filters.status = "";
  filters.pageNum = 1;
  fetchBookings();
}

async function handleCancel(booking: { id: string; labName?: string }) {
  try {
    await ElMessageBox.confirm(`确定要取消「${booking.labName ?? "-"}」的预约吗？`, "取消预约", {
      confirmButtonText: "确定取消",
      cancelButtonText: "返回",
      type: "warning",
    });
  } catch {
    return;
  }
  try {
    await bookingsApi.cancelBooking(Number(booking.id));
    ElMessage.success("预约已取消");
    fetchBookings();
  } catch {
    // error handled by interceptor
  }
}

function openReviewDialog(bookingId: string) {
  selectedBookingId.value = Number(bookingId);
  reviewDialogVisible.value = true;
}

function handleReviewSubmitted() {
  reviewDialogVisible.value = false;
  fetchBookings();
}

function handlePageChange(pageNum: number) {
  filters.pageNum = pageNum;
  fetchBookings();
}

function formatDate(dateStr: string) {
  if (!dateStr) return "-";
  return dateStr;
}

onMounted(() => {
  fetchBookings();
});
</script>

<template>
  <div class="my-bookings">
    <h1>我的预约</h1>

    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading" class="booking-list">
      <template v-if="page && page.records.length > 0">
        <el-card
          v-for="booking in page.records"
          :key="booking.id"
          class="booking-card"
          shadow="hover"
        >
          <div class="booking-header">
            <span class="lab-name">{{ booking.labName ?? `实验室 #${booking.labId}` }}</span>
            <el-tag
              :type="statusType[booking.status] || ''"
              :effect="booking.status === BookingStatus.APPROVED ? 'dark' : 'plain'"
            >
              {{ BOOKING_STATUS_LABELS[booking.status as BookingStatus] ?? booking.status }}
            </el-tag>
          </div>
          <div class="booking-body">
            <div class="info-row">
              <span class="label">日期</span>
              <span>{{ formatDate(booking.date) }}</span>
            </div>
            <div class="info-row">
              <span class="label">时间段</span>
              <span>{{ booking.startTime }} — {{ booking.endTime }}</span>
            </div>
            <div class="info-row">
              <span class="label">用途</span>
              <span>{{ booking.purpose || "-" }}</span>
            </div>
            <div class="info-row">
              <span class="label">人数</span>
              <span>{{ booking.personCount }} 人</span>
            </div>
            <div v-if="booking.rejectReason" class="info-row">
              <span class="label">驳回原因</span>
              <span class="reject-reason">{{ booking.rejectReason }}</span>
            </div>
            <div class="info-row">
              <span class="label">提交时间</span>
              <span>{{ booking.createdAt }}</span>
            </div>
          </div>

          <!-- Review section for completed bookings -->
          <div v-if="booking.status === BookingStatus.COMPLETED" class="review-info">
            <template v-if="reviewedBookings[booking.id]">
              <div class="reviewed-badge">
                <el-rate
                  :model-value="reviewedBookings[booking.id]!.rating"
                  disabled
                  size="small"
                />
                <span v-if="reviewedBookings[booking.id]!.comment" class="reviewed-comment">
                  {{ reviewedBookings[booking.id]!.comment }}
                </span>
              </div>
            </template>
          </div>

          <div class="booking-footer">
            <el-button
              v-if="cancellableStatuses.includes(booking.status as BookingStatus)"
              type="danger"
              plain
              size="small"
              @click="handleCancel(booking)"
            >
              取消预约
            </el-button>
            <el-button
              v-if="booking.status === BookingStatus.COMPLETED && !reviewedBookings[booking.id]"
              type="primary"
              size="small"
              @click="openReviewDialog(booking.id)"
            >
              评价
            </el-button>
          </div>
        </el-card>
      </template>
      <el-empty v-else-if="!loading" description="暂无预约记录" />
    </div>

    <div v-if="page && page.total > 0" class="pagination-wrapper">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="page.total"
        :page-size="filters.pageSize"
        v-model:current-page="filters.pageNum"
        @current-change="handlePageChange"
      />
    </div>

    <!-- Review Dialog -->
    <el-dialog v-model="reviewDialogVisible" title="评价预约" width="480px" destroy-on-close>
      <ReviewForm
        v-if="reviewDialogVisible"
        :booking-id="selectedBookingId"
        @submitted="handleReviewSubmitted"
        @cancel="reviewDialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<style scoped>
.my-bookings {
  padding: 24px;
  max-width: 800px;
}

h1 {
  margin: 0 0 20px;
  font-size: 24px;
}

.filter-card {
  margin-bottom: 16px;
}

.booking-list {
  min-height: 200px;
}

.booking-card {
  margin-bottom: 12px;
}

.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.lab-name {
  font-size: 16px;
  font-weight: 600;
}

.booking-body {
  display: flex;
  flex-wrap: wrap;
  gap: 0 32px;
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  gap: 8px;
  line-height: 1.8;
  min-width: 200px;
}

.info-row .label {
  color: #909399;
  flex-shrink: 0;
}

.reject-reason {
  color: #f56c6c;
}

.review-info {
  padding: 8px 0;
  border-top: 1px solid #ebeef5;
}

.reviewed-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reviewed-comment {
  color: #606266;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.booking-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
