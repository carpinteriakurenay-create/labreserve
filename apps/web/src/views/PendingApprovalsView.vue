<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { BookingStatus, BOOKING_STATUS_LABELS, type ApprovalRequest } from "@labreserve/shared";
import * as bookingsApi from "@/api/bookings";
import type { BookingPage } from "@/api/bookings";

const loading = ref(false);
const rejectDialogVisible = ref(false);
const rejectForm = reactive({ id: 0, reason: "" });
const rejecting = ref(false);

const pendingPage = ref<BookingPage | null>(null);
const allPage = ref<BookingPage | null>(null);

const pendingFilters = reactive({ pageNum: 1, pageSize: 10 });
const allFilters = reactive({ pageNum: 1, pageSize: 10 });

const activeTab = ref("pending");

const statusType: Record<string, "warning" | "success" | "danger" | "info" | ""> = {
  [BookingStatus.PENDING]: "warning",
  [BookingStatus.APPROVED]: "success",
  [BookingStatus.REJECTED]: "danger",
  [BookingStatus.CANCELLED]: "info",
  [BookingStatus.COMPLETED]: "",
};

async function fetchPending() {
  loading.value = true;
  try {
    pendingPage.value = await bookingsApi.getPendingApprovals({
      pageNum: pendingFilters.pageNum,
      pageSize: pendingFilters.pageSize,
    });
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
}

async function fetchAll() {
  loading.value = true;
  try {
    allPage.value = await bookingsApi.getBookings({
      pageNum: allFilters.pageNum,
      pageSize: allFilters.pageSize,
    });
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
}

function fetchCurrentTab() {
  if (activeTab.value === "pending") {
    fetchPending();
  } else {
    fetchAll();
  }
}

function handleTabChange() {
  fetchCurrentTab();
}

async function handleApprove(booking: { id: string; labName?: string; userRealName?: string }) {
  try {
    await ElMessageBox.confirm(
      `确定要通过「${booking.userRealName ?? "-"}」在「${booking.labName ?? "-"}」的预约吗？`,
      "审批通过",
      { confirmButtonText: "确定通过", cancelButtonText: "取消", type: "success" },
    );
  } catch {
    return;
  }
  const data: ApprovalRequest = { approved: true };
  try {
    await bookingsApi.approveBooking(Number(booking.id), data);
    ElMessage.success("已通过");
    fetchPending();
  } catch {
    // error handled by interceptor
  }
}

function openRejectDialog(booking: { id: string }) {
  rejectForm.id = Number(booking.id);
  rejectForm.reason = "";
  rejectDialogVisible.value = true;
}

async function handleReject() {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning("请填写驳回原因");
    return;
  }
  rejecting.value = true;
  const data: ApprovalRequest = { approved: false, rejectReason: rejectForm.reason.trim() };
  try {
    await bookingsApi.approveBooking(rejectForm.id, data);
    ElMessage.success("已驳回");
    rejectDialogVisible.value = false;
    fetchPending();
  } catch {
    // error handled by interceptor
  } finally {
    rejecting.value = false;
  }
}

async function handleComplete(booking: { id: string; labName?: string }) {
  try {
    await ElMessageBox.confirm(
      `确定要将「${booking.labName ?? "-"}」的预约标记为"已完成"吗？`,
      "确认完成",
      { confirmButtonText: "确定", cancelButtonText: "取消", type: "info" },
    );
  } catch {
    return;
  }
  try {
    await bookingsApi.completeBooking(Number(booking.id));
    ElMessage.success("已标记为完成");
    fetchCurrentTab();
  } catch {
    // error handled by interceptor
  }
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
    fetchCurrentTab();
  } catch {
    // error handled by interceptor
  }
}

function onPendingPageChange(pageNum: number) {
  pendingFilters.pageNum = pageNum;
  fetchPending();
}

function onAllPageChange(pageNum: number) {
  allFilters.pageNum = pageNum;
  fetchAll();
}

function formatDate(dateStr: string) {
  if (!dateStr) return "-";
  return dateStr;
}

onMounted(() => {
  fetchPending();
});
</script>

<template>
  <div class="approvals">
    <h1>预约审批</h1>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- Tab 1: 待审批 -->
      <el-tab-pane label="待审批" name="pending">
        <div v-loading="loading">
          <el-table
            v-if="pendingPage && pendingPage.records.length > 0"
            :data="pendingPage.records"
            border
            stripe
          >
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="labName" label="实验室" min-width="160" />
            <el-table-column prop="userRealName" label="申请人" width="100" />
            <el-table-column label="日期" width="120">
              <template #default="{ row }">{{ formatDate(row.date) }}</template>
            </el-table-column>
            <el-table-column label="时间段" width="150">
              <template #default="{ row }"> {{ row.startTime }} — {{ row.endTime }} </template>
            </el-table-column>
            <el-table-column prop="purpose" label="用途" min-width="180" />
            <el-table-column prop="personCount" label="人数" width="70" />
            <el-table-column label="提交时间" width="170">
              <template #default="{ row }">{{ row.createdAt }}</template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="handleApprove(row)">
                  通过
                </el-button>
                <el-button
                  v-if="row.status === BookingStatus.PENDING"
                  type="danger"
                  size="small"
                  @click="openRejectDialog(row)"
                >
                  驳回
                </el-button>
                <el-button
                  v-if="row.status === BookingStatus.APPROVED"
                  type="primary"
                  size="small"
                  plain
                  @click="handleComplete(row)"
                >
                  确认完成
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else-if="!loading" description="暂无待审批的预约" />
        </div>

        <div v-if="pendingPage && pendingPage.total > 0" class="pagination-wrapper">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="pendingPage.total"
            :page-size="pendingFilters.pageSize"
            v-model:current-page="pendingFilters.pageNum"
            @current-change="onPendingPageChange"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 2: 全部预约 -->
      <el-tab-pane label="全部预约" name="all">
        <div v-loading="loading">
          <el-table
            v-if="allPage && allPage.records.length > 0"
            :data="allPage.records"
            border
            stripe
          >
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="labName" label="实验室" min-width="160" />
            <el-table-column prop="userRealName" label="申请人" width="100" />
            <el-table-column label="日期" width="120">
              <template #default="{ row }">{{ formatDate(row.date) }}</template>
            </el-table-column>
            <el-table-column label="时间段" width="150">
              <template #default="{ row }"> {{ row.startTime }} — {{ row.endTime }} </template>
            </el-table-column>
            <el-table-column prop="purpose" label="用途" min-width="160" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag
                  :type="statusType[row.status] || ''"
                  :effect="row.status === BookingStatus.APPROVED ? 'dark' : 'plain'"
                  size="small"
                >
                  {{ BOOKING_STATUS_LABELS[row.status as BookingStatus] ?? row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === BookingStatus.APPROVED"
                  type="primary"
                  size="small"
                  plain
                  @click="handleComplete(row)"
                >
                  确认完成
                </el-button>
                <el-button
                  v-if="
                    row.status === BookingStatus.PENDING || row.status === BookingStatus.APPROVED
                  "
                  type="danger"
                  size="small"
                  plain
                  @click="handleCancel(row)"
                >
                  取消
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else-if="!loading" description="暂无预约记录" />
        </div>

        <div v-if="allPage && allPage.total > 0" class="pagination-wrapper">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="allPage.total"
            :page-size="allFilters.pageSize"
            v-model:current-page="allFilters.pageNum"
            @current-change="onAllPageChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 驳回原因弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回预约" width="420px">
      <el-form @submit.prevent="handleReject">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请填写驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="handleReject"> 确认驳回 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.approvals {
  padding: 24px;
}

h1 {
  margin: 0 0 20px;
  font-size: 24px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
