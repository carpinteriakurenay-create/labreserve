<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { Location } from "@element-plus/icons-vue";
import { LAB_STATUS_LABELS, LabStatus } from "@labreserve/shared";
import type { Lab } from "@labreserve/shared";
import * as labsApi from "@/api/labs";

const router = useRouter();

const loading = ref(false);
const labs = ref<Lab[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(12);

const filterName = ref("");
const filterStatus = ref<LabStatus | "">("");

async function fetchLabs() {
  loading.value = true;
  try {
    const params: labsApi.LabQueryParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (filterName.value) params.name = filterName.value;
    if (filterStatus.value) params.status = filterStatus.value;
    const result = await labsApi.getLabs(params);
    labs.value = result.records;
    total.value = result.total;
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchLabs();
}

function handleReset() {
  filterName.value = "";
  filterStatus.value = "";
  handleSearch();
}

function handlePageChange(p: number) {
  pageNum.value = p;
  fetchLabs();
}

function handleSizeChange(s: number) {
  pageSize.value = s;
  pageNum.value = 1;
  fetchLabs();
}

function goToDetail(id: string | number) {
  router.push(`/labs/${id}`);
}

function statusTagType(status: LabStatus): "success" | "warning" | "danger" {
  if (status === LabStatus.AVAILABLE) return "success";
  if (status === LabStatus.MAINTENANCE) return "warning";
  return "danger";
}

onMounted(() => {
  fetchLabs();
});
</script>

<template>
  <div class="labs-page">
    <h1>实验室列表</h1>

    <div class="toolbar">
      <div class="filters">
        <el-input
          v-model="filterName"
          placeholder="实验室名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="filterStatus"
          placeholder="状态筛选"
          clearable
          style="width: 140px; margin-left: 12px"
          @change="handleSearch"
        >
          <el-option label="全部" value="" />
          <el-option
            v-for="(label, key) in LAB_STATUS_LABELS"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-button type="primary" style="margin-left: 12px" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div v-loading="loading" class="card-grid">
      <el-empty v-if="!loading && labs.length === 0" description="暂无实验室数据" />
      <el-card v-for="lab in labs" :key="lab.id" class="lab-card" shadow="hover">
        <template #header>
          <span class="lab-name" @click="goToDetail(lab.id)">{{ lab.name }}</span>
        </template>
        <div class="lab-info">
          <div class="info-row">
            <el-icon><Location /></el-icon>
            <span>{{ lab.location || "-" }}</span>
          </div>
          <div class="info-row">
            <span>容量：{{ lab.capacity }} 人</span>
          </div>
          <div class="info-row">
            <span>设备：{{ lab.equipmentNum }} 台</span>
          </div>
          <div class="info-row">
            <el-tag :type="statusTagType(lab.status)" size="small">
              {{ LAB_STATUS_LABELS[lab.status] ?? lab.status }}
            </el-tag>
          </div>
          <div class="info-row" v-if="lab.managerName">
            <span>管理员：{{ lab.managerName }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.labs-page {
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
  margin-bottom: 20px;
}

.filters {
  display: flex;
  align-items: center;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 200px;
}

@media (max-width: 1400px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 800px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}

.lab-card {
  cursor: default;
}

.lab-name {
  color: var(--el-color-primary);
  cursor: pointer;
  font-weight: 600;
  font-size: 16px;
}

.lab-name:hover {
  text-decoration: underline;
}

.lab-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
