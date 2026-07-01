<script setup lang="ts">
import { ref, onMounted } from "vue";
import { BORROW_STATUS_LABELS } from "@labreserve/shared";
import type { Borrow } from "@labreserve/shared";
import * as borrowsApi from "@/api/borrows";

const loading = ref(false);
const borrows = ref<Borrow[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);

const filterStatus = ref("");

function handleSearch() {
  pageNum.value = 1;
  fetchMyBorrows();
}

function handleReset() {
  filterStatus.value = "";
  pageNum.value = 1;
  fetchMyBorrows();
}

async function fetchMyBorrows() {
  loading.value = true;
  try {
    const params: borrowsApi.BorrowQueryParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (filterStatus.value) params.status = filterStatus.value;
    const result = await borrowsApi.getMyBorrows(params);
    borrows.value = result.records;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function handlePageChange(p: number) {
  pageNum.value = p;
  fetchMyBorrows();
}

function handleSizeChange(s: number) {
  pageSize.value = s;
  pageNum.value = 1;
  fetchMyBorrows();
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
  fetchMyBorrows();
});
</script>

<template>
  <div class="my-borrows">
    <h1>我的借用</h1>

    <div class="toolbar">
      <div class="filters">
        <el-select
          v-model="filterStatus"
          placeholder="状态筛选"
          clearable
          style="width: 140px"
          @change="handleSearch"
        >
          <el-option
            v-for="(label, key) in BORROW_STATUS_LABELS"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-button style="margin-left: 12px" type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <el-table :data="borrows" v-loading="loading" stripe>
      <el-table-column prop="equipmentName" label="设备名称" min-width="150" />
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
      <el-table-column prop="rejectReason" label="拒绝原因" min-width="150">
        <template #default="{ row }">{{ row.rejectReason || "-" }}</template>
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
  </div>
</template>

<style scoped>
.my-borrows {
  padding: 24px;
}

h1 {
  margin: 0 0 20px;
  font-size: 24px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
