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

function statusColor(status: string) {
  const map: Record<string, string> = {
    PENDING: "#409EFF",
    APPROVED: "#67C23A",
    REJECTED: "#F56C6C",
    BORROWING: "#67C23A",
    RETURNED: "#909399",
  };
  return map[status] ?? "#909399";
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

    <div v-loading="loading">
      <el-empty v-if="!loading && borrows.length === 0" description="暂无借用记录" />

      <el-timeline v-else>
        <el-timeline-item
          v-for="item in borrows"
          :key="item.id"
          :timestamp="item.borrowDate"
          :color="statusColor(item.status)"
          placement="top"
        >
          <el-card class="borrow-card" shadow="hover">
            <div class="card-header">
              <span class="equipment-name">{{ item.equipmentName }}</span>
              <el-tag :type="statusTagType(item.status)" size="small">
                {{
                  BORROW_STATUS_LABELS[item.status as keyof typeof BORROW_STATUS_LABELS] ??
                  item.status
                }}
              </el-tag>
            </div>
            <div class="card-body">
              <div class="info-row">
                <span class="label">借用日期：</span>
                <span>{{ item.borrowDate }}</span>
              </div>
              <div class="info-row">
                <span class="label">预计归还：</span>
                <span>{{ item.expectedReturn }}</span>
              </div>
              <div v-if="item.actualReturn" class="info-row">
                <span class="label">实际归还：</span>
                <span>{{ item.actualReturn }}</span>
              </div>
              <div class="info-row">
                <span class="label">用途：</span>
                <span>{{ item.purpose || "-" }}</span>
              </div>
              <div v-if="item.rejectReason" class="info-row">
                <span class="label">拒绝原因：</span>
                <span class="reject-reason">{{ item.rejectReason }}</span>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div v-if="total > pageSize" class="pagination">
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
  margin-bottom: 24px;
}

.filters {
  display: flex;
  align-items: center;
}

.borrow-card {
  margin-bottom: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.equipment-name {
  font-size: 16px;
  font-weight: 600;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-row {
  font-size: 14px;
  line-height: 1.6;
}

.info-row .label {
  color: #909399;
}

.reject-reason {
  color: #f56c6c;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
