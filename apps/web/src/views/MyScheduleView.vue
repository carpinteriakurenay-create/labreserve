<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useAuthStore } from "@/stores/auth";
import type { Course } from "@labreserve/shared";
import * as coursesApi from "@/api/courses";

const authStore = useAuthStore();

const userRole = computed(() => authStore.userRole);

const semester = ref("");
const className = ref("");

const loading = ref(false);
const courses = ref<Course[]>([]);

const HOURS = Array.from({ length: 29 }, (_, i) => {
  const h = Math.floor(i / 2) + 8;
  const m = i % 2 === 0 ? "00" : "30";
  return `${String(h).padStart(2, "0")}:${m}`;
});

const COLORS = [
  "#409EFF",
  "#67C23A",
  "#E6A23C",
  "#F56C6C",
  "#909399",
  "#00D4AA",
  "#8B5CF6",
  "#EC4899",
];

const GRID_START_HOUR = 8;
const GRID_END_HOUR = 22;
const ROW_HEIGHT = 32; // px per 30-min slot

const gridRows = computed(() => (GRID_END_HOUR - GRID_START_HOUR) * 2);

function courseStyle(course: Course) {
  const [sh, sm] = course.startTime.split(":").map(Number);
  const [eh, em] = course.endTime.split(":").map(Number);
  const startMinutes = ((sh ?? 8) - GRID_START_HOUR) * 60 + (sm ?? 0);
  const endMinutes = ((eh ?? 10) - GRID_START_HOUR) * 60 + (em ?? 0);
  const top = (startMinutes / 30) * ROW_HEIGHT;
  const height = ((endMinutes - startMinutes) / 30) * ROW_HEIGHT;
  return {
    top: `${top}px`,
    height: `${Math.max(height, ROW_HEIGHT)}px`,
  };
}

function courseColor(name: string) {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return COLORS[Math.abs(hash) % COLORS.length];
}

function formatCourseTime(course: Course) {
  return `${course.startTime}-${course.endTime}`;
}

async function fetchMyCourses() {
  if (userRole.value === "STUDENT" && !className.value.trim()) {
    courses.value = [];
    return;
  }

  loading.value = true;
  try {
    const params: { className?: string; pageNum: number; pageSize: number } = {
      pageNum: 1,
      pageSize: 200,
    };
    if (semester.value) {
      (params as { semester?: string }).semester = semester.value;
    }
    if (userRole.value === "STUDENT") {
      params.className = className.value.trim();
    }
    const result = await coursesApi.getMyCourses(params);
    courses.value = result.records;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  fetchMyCourses();
}

function handleReset() {
  semester.value = "";
  className.value = "";
  fetchMyCourses();
}

const DAYS = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"];

function coursesByDay(day: number) {
  return courses.value.filter((c) => c.dayOfWeek === day);
}

watch(
  userRole,
  () => {
    fetchMyCourses();
  },
  { immediate: true },
);
</script>

<template>
  <div class="my-schedule">
    <h1>我的课表</h1>

    <div class="toolbar">
      <div class="filters">
        <el-input
          v-model="semester"
          placeholder="学期（如 2025-2026-1）"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <template v-if="userRole === 'STUDENT'">
          <el-input
            v-model="className"
            placeholder="请输入班级（如 计科2101）"
            clearable
            style="width: 200px; margin-left: 12px"
            @keyup.enter="handleSearch"
          />
        </template>
        <el-button style="margin-left: 12px" type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div v-loading="loading" class="schedule-container">
      <el-empty v-if="!loading && courses.length === 0" description="暂无课程" />

      <div v-else class="calendar-grid">
        <!-- day headers -->
        <div class="grid-header"></div>
        <div v-for="day in DAYS" :key="day" class="grid-header day-header">
          {{ day }}
        </div>

        <!-- time labels + course slots -->
        <template v-for="(time, timeIdx) in HOURS" :key="time">
          <div v-if="time.endsWith(':00')" class="time-label" :style="{ gridRow: timeIdx + 2 }">
            {{ time }}
          </div>
        </template>

        <!-- day columns with course cards -->
        <div
          v-for="dayIdx in 7"
          :key="dayIdx"
          class="day-column"
          :style="{ gridColumn: dayIdx + 1, gridRow: '2 / ' + (gridRows + 2) }"
        >
          <div
            v-for="course in coursesByDay(dayIdx)"
            :key="course.id"
            class="course-card"
            :style="{
              ...courseStyle(course),
              backgroundColor: courseColor(course.name),
            }"
          >
            <span class="course-name">{{ course.name }}</span>
            <span class="course-info">{{ course.className }}</span>
            <span class="course-info">{{ course.teacherName }}</span>
            <span class="course-time">{{ formatCourseTime(course) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.my-schedule {
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

.schedule-container {
  min-height: 400px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: 60px repeat(7, 1fr);
  grid-template-rows: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: auto;
  position: relative;
}

.grid-header {
  background: #f5f7fa;
  padding: 10px 4px;
  text-align: center;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 2;
}

.day-header {
  border-right: 1px solid #e4e7ed;
}

.day-header:last-child {
  border-right: none;
}

.time-label {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 8px;
  font-size: 12px;
  color: #909399;
  border-right: 1px solid #e4e7ed;
  background: #fafafa;
  grid-row: span 2;
}

.day-column {
  position: relative;
  border-right: 1px solid #ebeef5;
  background: #fff;
}

.day-column:last-child {
  border-right: none;
}

.course-card {
  position: absolute;
  left: 2px;
  right: 2px;
  border-radius: 4px;
  padding: 4px 6px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 1px;
  cursor: default;
  z-index: 1;
}

.course-name {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  line-height: 1.3;
}

.course-info {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.3;
}

.course-time {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.3;
}
</style>
