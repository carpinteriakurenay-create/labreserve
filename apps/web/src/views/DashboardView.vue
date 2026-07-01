<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useAuthStore } from "@/stores/auth";
import {
  getDashboardKpi,
  getLabUsage,
  getEquipmentUsage,
  getStudentRanking,
} from "@/api/dashboard";
import type {
  DashboardKpi,
  LabUsageStat,
  EquipmentUsageStat,
  StudentRanking,
} from "@labreserve/shared";
import { ElMessage } from "element-plus";
import LabUsageChart from "@/components/LabUsageChart.vue";
import EquipmentStatusChart from "@/components/EquipmentStatusChart.vue";

const authStore = useAuthStore();
const loading = ref(false);
const showStudentRanking = computed(() => authStore.isTeacher || authStore.isAdmin);

const kpi = ref<DashboardKpi>({
  todayBookings: 0,
  todayBorrows: 0,
  labUsageRate: 0,
  pendingApprovals: 0,
});

const labUsage = ref<LabUsageStat[]>([]);
const equipmentUsage = ref<EquipmentUsageStat[]>([]);
const studentRanking = ref<StudentRanking[]>([]);

const dateRange = ref<[Date, Date] | null>(null);

const dateFrom = computed(() => {
  if (!dateRange.value) return undefined;
  return formatDateStr(dateRange.value[0]);
});

const dateTo = computed(() => {
  if (!dateRange.value) return undefined;
  return formatDateStr(dateRange.value[1]);
});

function formatDateStr(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

const kpiCards = computed(() => [
  { label: "今日预约", value: kpi.value.todayBookings, color: "#409EFF", unit: "次" },
  { label: "今日借用", value: kpi.value.todayBorrows, color: "#67C23A", unit: "次" },
  {
    label: "实验室使用率",
    value: (kpi.value.labUsageRate * 100).toFixed(1),
    color: "#E6A23C",
    unit: "%",
  },
  { label: "待审批", value: kpi.value.pendingApprovals, color: "#F56C6C", unit: "项" },
]);

async function fetchAll() {
  loading.value = true;
  try {
    const [kpiData, labData, equipData, rankingData] = await Promise.all([
      getDashboardKpi(),
      getLabUsage({ dateFrom: dateFrom.value, dateTo: dateTo.value }),
      getEquipmentUsage({ dateFrom: dateFrom.value, dateTo: dateTo.value }),
      showStudentRanking.value
        ? getStudentRanking({ dateFrom: dateFrom.value, dateTo: dateTo.value, limit: 10 })
        : Promise.resolve([]),
    ]);
    kpi.value = kpiData;
    labUsage.value = labData;
    equipmentUsage.value = equipData;
    studentRanking.value = rankingData;
  } catch {
    ElMessage.error("加载仪表盘数据失败");
  } finally {
    loading.value = false;
  }
}

function handleDateChange() {
  fetchAll();
}

onMounted(() => {
  fetchAll();
});
</script>

<template>
  <div class="dashboard">
    <h1 class="page-title">数据仪表盘</h1>

    <div class="filter-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        @change="handleDateChange"
      />
    </div>

    <div v-loading="loading">
      <div class="kpi-row">
        <el-card v-for="card in kpiCards" :key="card.label" class="kpi-card" shadow="hover">
          <div class="kpi-label">{{ card.label }}</div>
          <div class="kpi-value" :style="{ color: card.color }">
            {{ card.value }}<span class="kpi-unit"> {{ card.unit }}</span>
          </div>
        </el-card>
      </div>

      <el-card class="chart-card" shadow="hover">
        <template #header>
          <span class="section-title">实验室使用统计</span>
        </template>
        <LabUsageChart v-if="labUsage.length > 0" :data="labUsage" />
        <el-empty v-else description="暂无数据" />
      </el-card>

      <div class="chart-row">
        <el-card class="chart-card chart-half" shadow="hover">
          <template #header>
            <span class="section-title">设备状态分布</span>
          </template>
          <EquipmentStatusChart />
        </el-card>

        <el-card class="chart-card chart-half" shadow="hover">
          <template #header>
            <span class="section-title">设备借用统计</span>
          </template>
          <el-table :data="equipmentUsage" stripe>
            <el-table-column prop="equipmentName" label="设备" min-width="120" />
            <el-table-column prop="borrowCount" label="借用次数" width="100" sortable />
            <el-table-column label="平均借用天数" width="130" sortable>
              <template #default="{ row }">
                {{ row.avgBorrowDays.toFixed(1) }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="equipmentUsage.length === 0" description="暂无数据" />
        </el-card>
      </div>

      <el-card v-if="showStudentRanking" class="chart-card" shadow="hover">
        <template #header>
          <span class="section-title">学生使用排行</span>
        </template>
        <el-table :data="studentRanking" stripe>
          <el-table-column label="排名" width="80">
            <template #default="{ $index }">
              {{ $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="userRealName" label="学生姓名" min-width="150" />
          <el-table-column prop="bookingCount" label="预约次数" width="120" sortable />
          <el-table-column label="总时长（小时）" width="150" sortable>
            <template #default="{ row }">
              {{ row.totalHours.toFixed(1) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="studentRanking.length === 0" description="暂无数据" />
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.filter-bar {
  margin-bottom: 20px;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.kpi-card {
  text-align: center;
}

.kpi-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 32px;
  font-weight: 700;
}

.kpi-unit {
  font-size: 14px;
  font-weight: 400;
  color: #909399;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-half {
  margin-bottom: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-row {
    grid-template-columns: 1fr;
  }
}
</style>
