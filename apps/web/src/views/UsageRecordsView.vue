<script setup lang="ts">
import { ref, reactive, onMounted, watch } from "vue";
import { ElMessage } from "element-plus";
import {
  getUsageRecords,
  exportUsageRecordsCsv,
  type UsageRecordQueryParams,
} from "@/api/usageRecords";
import type { UsageRecord } from "@labreserve/shared";

const loading = ref(false);
const exporting = ref(false);
const records = ref<UsageRecord[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);
const dateRange = ref<[Date, Date] | null>(null);

const filters = reactive<UsageRecordQueryParams>({
  pageNum: 1,
  pageSize: 20,
});

watch(dateRange, (val) => {
  if (val) {
    filters.dateFrom = formatDate(val[0]);
    filters.dateTo = formatDate(val[1]);
  } else {
    filters.dateFrom = undefined;
    filters.dateTo = undefined;
  }
});

function formatDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

async function fetchRecords() {
  loading.value = true;
  try {
    filters.pageNum = pageNum.value;
    filters.pageSize = pageSize.value;
    const page = await getUsageRecords(filters);
    records.value = page.records;
    total.value = page.total;
  } catch {
    ElMessage.error("加载使用记录失败");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchRecords();
}

function handleReset() {
  filters.labId = undefined;
  filters.userId = undefined;
  filters.dateFrom = undefined;
  filters.dateTo = undefined;
  dateRange.value = null;
  pageNum.value = 1;
  fetchRecords();
}

function handlePageChange(p: number) {
  pageNum.value = p;
  fetchRecords();
}

function handleSizeChange(s: number) {
  pageSize.value = s;
  pageNum.value = 1;
  fetchRecords();
}

async function handleExport() {
  exporting.value = true;
  try {
    const blob = await exportUsageRecordsCsv({
      labId: filters.labId,
      userId: filters.userId,
      dateFrom: filters.dateFrom,
      dateTo: filters.dateTo,
    });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `usage-records-${Date.now()}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    ElMessage.success("导出成功");
  } catch {
    ElMessage.error("导出失败");
  } finally {
    exporting.value = false;
  }
}

onMounted(() => {
  fetchRecords();
});
</script>

<template>
  <div class="usage-records">
    <h1 class="page-title">使用记录</h1>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="实验室">
          <el-input v-model="filters.labId" placeholder="实验室ID" clearable />
        </el-form-item>
        <el-form-item label="用户">
          <el-input v-model="filters.userId" placeholder="用户ID" clearable />
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" :loading="exporting" @click="handleExport">
            导出 CSV
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table v-loading="loading" :data="records" stripe style="width: 100%; margin-top: 16px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="labName" label="实验室" min-width="150" />
      <el-table-column prop="userRealName" label="用户" width="120" />
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column prop="startTime" label="开始时间" width="100" />
      <el-table-column prop="endTime" label="结束时间" width="100" />
      <el-table-column prop="purpose" label="用途" min-width="200" show-overflow-tooltip />
      <el-table-column prop="personCount" label="人数" width="80" />
      <el-table-column prop="completedAt" label="完成时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.completedAt).toLocaleString("zh-CN") }}
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
.usage-records {
  padding: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.filter-card {
  margin-bottom: 0;
}

.filter-form {
  margin-bottom: 0;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
