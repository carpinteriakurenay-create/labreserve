<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { BORROW_STATUS_LABELS } from "@labreserve/shared";
import type { Borrow } from "@labreserve/shared";
import * as borrowsApi from "@/api/borrows";

const loading = ref(false);
const borrows = ref<Borrow[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);

const activeTab = ref("PENDING");
const tabs = [
  { label: "待审批", value: "PENDING" },
  { label: "借用中", value: "BORROWING" },
  { label: "全部", value: "" },
];

const rejectDialogVisible = ref(false);
const rejectBorrowId = ref<number | null>(null);
const rejectReason = ref("");

function handleTabChange() {
  pageNum.value = 1;
  fetchBorrows();
}

async function fetchBorrows() {
  loading.value = true;
  try {
    const params: borrowsApi.BorrowQueryParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (activeTab.value) params.status = activeTab.value;
    const result = await borrowsApi.getBorrows(params);
    borrows.value = result.records;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function handlePageChange(p: number) {
  pageNum.value = p;
  fetchBorrows();
}

function handleSizeChange(s: number) {
  pageSize.value = s;
  pageNum.value = 1;
  fetchBorrows();
}

async function handleApprove(borrow: Borrow) {
  try {
    await ElMessageBox.confirm(`确认通过「${borrow.equipmentName}」的借用申请？`, "审批通过确认", {
      confirmButtonText: "确认通过",
      cancelButtonText: "取消",
      type: "success",
    });
  } catch {
    return;
  }
  await borrowsApi.approveBorrow(Number(borrow.id), { approved: true });
  ElMessage.success("已通过借用申请");
  fetchBorrows();
}

function openReject(borrow: Borrow) {
  rejectBorrowId.value = Number(borrow.id);
  rejectReason.value = "";
  rejectDialogVisible.value = true;
}

async function handleReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning("请填写拒绝原因");
    return;
  }
  if (rejectBorrowId.value === null) return;

  await borrowsApi.approveBorrow(rejectBorrowId.value, {
    approved: false,
    rejectReason: rejectReason.value,
  });
  ElMessage.success("已拒绝借用申请");
  rejectDialogVisible.value = false;
  fetchBorrows();
}

async function handleReturn(borrow: Borrow) {
  try {
    await ElMessageBox.confirm(
      `确认「${borrow.equipmentName}」已归还？设备状态将恢复为可借用。`,
      "归还确认",
      { confirmButtonText: "确认归还", cancelButtonText: "取消", type: "warning" },
    );
  } catch {
    return;
  }
  await borrowsApi.returnBorrow(Number(borrow.id));
  ElMessage.success("已确认归还");
  fetchBorrows();
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    PENDING: "info",
    APPROVED: "success",
    REJECTED: "danger",
    BORROWING: "warning",
    RETURNED: "",
  };
  return map[status] ?? "info";
}

onMounted(() => {
  fetchBorrows();
});
</script>

<template>
  <div class="admin-borrows">
    <h1>借用管理</h1>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <el-table :data="borrows" v-loading="loading" stripe>
      <el-table-column prop="equipmentName" label="设备" min-width="140" />
      <el-table-column prop="userName" label="申请人" width="100" />
      <el-table-column prop="borrowDate" label="借用日期" width="120" />
      <el-table-column prop="expectedReturn" label="预计归还" width="120" />
      <el-table-column prop="actualReturn" label="实际归还" width="120">
        <template #default="{ row }">{{ row.actualReturn || "-" }}</template>
      </el-table-column>
      <el-table-column prop="purpose" label="用途" min-width="150">
        <template #default="{ row }">{{ row.purpose || "-" }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{
              BORROW_STATUS_LABELS[row.status as keyof typeof BORROW_STATUS_LABELS] ?? row.status
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
            <el-button size="small" type="danger" @click="openReject(row)">拒绝</el-button>
          </template>
          <template v-else-if="row.status === 'BORROWING'">
            <el-button size="small" type="primary" @click="handleReturn(row)">确认归还</el-button>
          </template>
          <span v-else style="color: #909399">-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
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

    <el-dialog v-model="rejectDialogVisible" title="拒绝借用申请" width="450px">
      <el-form label-width="80px">
        <el-form-item label="拒绝原因" required>
          <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-borrows {
  padding: 24px;
}

h1 {
  margin: 0 0 8px;
  font-size: 24px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
