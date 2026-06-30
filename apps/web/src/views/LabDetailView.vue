<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeft } from "@element-plus/icons-vue";
import { LAB_STATUS_LABELS, LabStatus } from "@labreserve/shared";
import type { Lab, LabHours } from "@labreserve/shared";
import * as labsApi from "@/api/labs";

const route = useRoute();
const router = useRouter();

const DAY_OF_WEEK_LABELS: Record<number, string> = {
  1: "周一",
  2: "周二",
  3: "周三",
  4: "周四",
  5: "周五",
  6: "周六",
  7: "周日",
};

const WEEKDAY_ORDER = [1, 2, 3, 4, 5, 6, 7];

const loading = ref(true);
const lab = ref<Lab | null>(null);
const hours = ref<LabHours[]>([]);

function getHoursForDay(dayOfWeek: number): LabHours[] {
  return hours.value
    .filter((h) => h.dayOfWeek === dayOfWeek)
    .sort((a, b) => a.openTime.localeCompare(b.openTime));
}

function statusTagType(status: LabStatus): "success" | "warning" | "danger" {
  if (status === LabStatus.AVAILABLE) return "success";
  if (status === LabStatus.MAINTENANCE) return "warning";
  return "danger";
}

function goBack() {
  router.push("/labs");
}

onMounted(async () => {
  try {
    const id = Number(route.params.id);
    const [labData, hoursData] = await Promise.all([
      labsApi.getLabById(id),
      labsApi.getLabHours(id),
    ]);
    lab.value = labData;
    hours.value = hoursData;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="lab-detail" v-loading="loading">
    <div class="back-row">
      <el-button text :icon="ArrowLeft" @click="goBack">返回列表</el-button>
    </div>

    <template v-if="lab">
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <h1>{{ lab.name }}</h1>
            <el-tag :type="statusTagType(lab.status)">
              {{ LAB_STATUS_LABELS[lab.status] ?? lab.status }}
            </el-tag>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="位置">{{ lab.location || "-" }}</el-descriptions-item>
          <el-descriptions-item label="容量">{{ lab.capacity }} 人</el-descriptions-item>
          <el-descriptions-item label="设备数量">{{ lab.equipmentNum }} 台</el-descriptions-item>
          <el-descriptions-item label="管理员">{{ lab.managerName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{
            lab.description || "暂无描述"
          }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="lab.imageUrl" class="lab-image">
          <el-image :src="lab.imageUrl" fit="cover" style="max-width: 400px; border-radius: 4px" />
        </div>

        <div class="booking-action">
          <el-button type="primary" @click="router.push(`/bookings/create?labId=${lab.id}`)">
            预约此实验室
          </el-button>
        </div>
      </el-card>

      <el-card class="hours-card">
        <template #header>
          <span class="section-title">开放时间</span>
        </template>
        <template v-if="hours.length > 0">
          <div class="hours-list">
            <div v-for="day in WEEKDAY_ORDER" :key="day" class="hours-row">
              <span class="day-label">{{ DAY_OF_WEEK_LABELS[day] }}</span>
              <span v-if="getHoursForDay(day).length > 0" class="time-slots">
                <template v-for="(h, i) in getHoursForDay(day)" :key="h.id ?? i">
                  <span v-if="i > 0">, </span>
                  {{ h.openTime }} - {{ h.closeTime }}
                </template>
              </span>
              <span v-else class="no-hours">-</span>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无开放时间" :image-size="80" />
      </el-card>

      <el-card class="equipment-card">
        <template #header>
          <span class="section-title">设备清单</span>
        </template>
        <div class="placeholder-text">
          本实验室共有 {{ lab.equipmentNum }} 台设备。设备管理功能即将上线。
        </div>
      </el-card>
    </template>

    <el-result
      v-else-if="!loading"
      icon="warning"
      title="实验室不存在"
      sub-title="请检查链接是否正确"
    >
      <template #extra>
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.lab-detail {
  padding: 24px;
  max-width: 960px;
}

.back-row {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-header h1 {
  margin: 0;
  font-size: 22px;
}

.info-card,
.hours-card,
.equipment-card {
  margin-bottom: 20px;
}

.section-title {
  font-weight: 600;
  font-size: 16px;
}

.lab-image {
  margin-top: 16px;
}

.booking-action {
  margin-top: 16px;
  text-align: right;
}

.hours-list {
  padding: 4px 0;
}

.hours-row {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
}

.hours-row:last-child {
  border-bottom: none;
}

.day-label {
  width: 80px;
  font-weight: 600;
  color: #303133;
}

.time-slots {
  color: #606266;
}

.no-hours {
  color: #c0c4cc;
}

.placeholder-text {
  color: #909399;
  font-size: 14px;
}
</style>
