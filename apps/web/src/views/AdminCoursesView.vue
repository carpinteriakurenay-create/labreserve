<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
  isValidSemester,
  type Course,
  type CourseCreateRequest,
  type CourseUpdateRequest,
} from "@labreserve/shared";
import * as coursesApi from "@/api/courses";
import * as labsApi from "@/api/labs";
import * as usersApi from "@/api/users";
import type { Lab, User } from "@labreserve/shared";

const loading = ref(false);
const courses = ref<Course[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);

const filterLabId = ref<number | undefined>(undefined);
const filterName = ref("");
const filterTeacherName = ref("");

const labs = ref<Lab[]>([]);
const teachers = ref<User[]>([]);

const DAY_LABELS = ["", "周一", "周二", "周三", "周四", "周五", "周六", "周日"];

// ── dialog ──

const dialogVisible = ref(false);
const dialogTitle = ref("新增课程");
const isEdit = ref(false);
const editId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  name: "",
  labId: undefined as number | undefined,
  teacherId: undefined as number | undefined,
  semester: "",
  dayOfWeek: undefined as number | undefined,
  startTime: "",
  endTime: "",
  startDate: "",
  endDate: "",
  className: "",
});

const rules: FormRules = {
  name: [{ required: true, message: "请输入课程名称", trigger: "blur" }],
  labId: [{ required: true, message: "请选择实验室", trigger: "change" }],
  teacherId: [{ required: true, message: "请选择任课教师", trigger: "change" }],
  semester: [
    { required: true, message: "请输入学期", trigger: "blur" },
    {
      validator: (_rule, value, callback) => {
        if (value && !isValidSemester(String(value))) {
          callback(new Error("学期格式无效，应为 YYYY-YYYY-[12]"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
  dayOfWeek: [{ required: true, message: "请选择星期", trigger: "change" }],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  endTime: [{ required: true, message: "请选择结束时间", trigger: "change" }],
  startDate: [{ required: true, message: "请选择开始日期", trigger: "change" }],
  endDate: [{ required: true, message: "请选择结束日期", trigger: "change" }],
  className: [{ required: true, message: "请输入班级", trigger: "blur" }],
};

// ── fetch ──

async function fetchCourses() {
  loading.value = true;
  try {
    const params: coursesApi.CourseQueryParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (filterLabId.value) params.labId = filterLabId.value;
    if (filterName.value) params.className = undefined; // className is the closest match
    const result = await coursesApi.getCourses(params);
    // client-side filter by name and teacher name (API doesn't support these directly)
    let records = result.records;
    if (filterName.value) {
      const q = filterName.value.toLowerCase();
      records = records.filter((c) => c.name.toLowerCase().includes(q));
    }
    if (filterTeacherName.value) {
      const q = filterTeacherName.value.toLowerCase();
      records = records.filter((c) => (c.teacherName ?? "").toLowerCase().includes(q));
    }
    courses.value = records;
    total.value = records.length;
  } finally {
    loading.value = false;
  }
}

async function fetchLabs() {
  try {
    const result = await labsApi.getLabs({ pageSize: 1000 });
    labs.value = result.records;
  } catch {
    // ignore
  }
}

async function fetchTeachers() {
  try {
    const result = await usersApi.getUsers({ role: "TEACHER", pageSize: 1000 });
    teachers.value = result.records;
  } catch {
    // ignore
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchCourses();
}

function handleReset() {
  filterLabId.value = undefined;
  filterName.value = "";
  filterTeacherName.value = "";
  pageNum.value = 1;
  fetchCourses();
}

function handlePageChange(p: number) {
  pageNum.value = p;
  fetchCourses();
}

function handleSizeChange(s: number) {
  pageSize.value = s;
  pageNum.value = 1;
  fetchCourses();
}

// ── create / edit ──

function openCreate() {
  isEdit.value = false;
  editId.value = null;
  dialogTitle.value = "新增课程";
  form.name = "";
  form.labId = undefined;
  form.teacherId = undefined;
  form.semester = "";
  form.dayOfWeek = undefined;
  form.startTime = "";
  form.endTime = "";
  form.startDate = "";
  form.endDate = "";
  form.className = "";
  dialogVisible.value = true;
}

function openEdit(course: Course) {
  isEdit.value = true;
  editId.value = Number(course.id);
  dialogTitle.value = "编辑课程";
  form.name = course.name;
  form.labId = Number(course.labId);
  form.teacherId = Number(course.teacherId);
  form.semester = course.semester;
  form.dayOfWeek = course.dayOfWeek;
  form.startTime = course.startTime;
  form.endTime = course.endTime;
  form.startDate = course.startDate;
  form.endDate = course.endDate;
  form.className = course.className;
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  if (form.startTime >= form.endTime) {
    ElMessage.warning("开始时间必须早于结束时间");
    return;
  }
  if (form.startDate > form.endDate) {
    ElMessage.warning("开始日期不能晚于结束日期");
    return;
  }

  submitting.value = true;
  try {
    if (isEdit.value && editId.value) {
      const data: CourseUpdateRequest = {
        name: form.name,
        labId: String(form.labId ?? ""),
        teacherId: String(form.teacherId ?? ""),
        dayOfWeek: form.dayOfWeek,
        startTime: form.startTime,
        endTime: form.endTime,
        startDate: form.startDate,
        endDate: form.endDate,
        className: form.className,
      };
      await coursesApi.updateCourse(editId.value, data);
      ElMessage.success("更新成功");
    } else {
      const data: CourseCreateRequest = {
        name: form.name,
        labId: String(form.labId ?? ""),
        teacherId: String(form.teacherId ?? ""),
        semester: form.semester,
        dayOfWeek: form.dayOfWeek!,
        startTime: form.startTime,
        endTime: form.endTime,
        startDate: form.startDate,
        endDate: form.endDate,
        className: form.className,
      };
      await coursesApi.createCourse(data);
      ElMessage.success("创建成功");
    }
    dialogVisible.value = false;
    fetchCourses();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(course: Course) {
  try {
    await ElMessageBox.confirm(`确定要删除课程「${course.name}」吗？`, "删除确认", {
      confirmButtonText: "确定删除",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  await coursesApi.deleteCourse(Number(course.id));
  ElMessage.success("删除成功");
  fetchCourses();
}

function formatSchedule(row: Course) {
  const day = DAY_LABELS[row.dayOfWeek] ?? `周${row.dayOfWeek}`;
  return `${day} ${row.startTime}-${row.endTime}`;
}

onMounted(() => {
  fetchLabs();
  fetchTeachers();
  fetchCourses();
});
</script>

<template>
  <div class="admin-courses">
    <h1>课程管理</h1>

    <div class="toolbar">
      <div class="filters">
        <el-select
          v-model="filterLabId"
          placeholder="实验室分室"
          clearable
          style="width: 180px"
          @change="handleSearch"
        >
          <el-option v-for="lab in labs" :key="lab.id" :label="lab.name" :value="Number(lab.id)" />
        </el-select>
        <el-input
          v-model="filterName"
          placeholder="课程名称"
          clearable
          style="width: 180px; margin-left: 12px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-input
          v-model="filterTeacherName"
          placeholder="任课教师"
          clearable
          style="width: 140px; margin-left: 12px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button style="margin-left: 12px" type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-button type="primary" @click="openCreate">新增课程</el-button>
    </div>

    <el-table :data="courses" v-loading="loading" stripe>
      <el-table-column prop="name" label="课程名称" min-width="150" />
      <el-table-column prop="labName" label="实验室分室" min-width="160">
        <template #default="{ row }">{{ row.labName || "-" }}</template>
      </el-table-column>
      <el-table-column prop="teacherName" label="任课教师" width="100">
        <template #default="{ row }">{{ row.teacherName || "-" }}</template>
      </el-table-column>
      <el-table-column prop="semester" label="学期" width="130" />
      <el-table-column label="上课时间" width="200">
        <template #default="{ row }">{{ formatSchedule(row) }}</template>
      </el-table-column>
      <el-table-column prop="className" label="班级" min-width="120">
        <template #default="{ row }">{{ row.className || "-" }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="form.name" placeholder="如：数字逻辑实验" />
        </el-form-item>
        <el-form-item label="所属实验室" prop="labId">
          <el-select v-model="form.labId" style="width: 100%" placeholder="请选择实验室">
            <el-option
              v-for="lab in labs"
              :key="lab.id"
              :label="lab.name"
              :value="Number(lab.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任课教师" prop="teacherId">
          <el-select v-model="form.teacherId" style="width: 100%" placeholder="请选择教师">
            <el-option
              v-for="t in teachers"
              :key="t.id"
              :label="t.realName"
              :value="Number(t.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学期" prop="semester">
          <el-input v-model="form.semester" placeholder="2025-2026-1" />
        </el-form-item>
        <el-form-item label="星期" prop="dayOfWeek">
          <el-select v-model="form.dayOfWeek" style="width: 100%" placeholder="请选择星期">
            <el-option
              v-for="(label, idx) in DAY_LABELS"
              v-show="idx > 0"
              :key="idx"
              :label="label"
              :value="idx"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上课时间" prop="startTime">
          <div class="time-row">
            <el-time-select
              v-model="form.startTime"
              :max-time="form.endTime || '22:00'"
              placeholder="开始"
              start="08:00"
              step="00:30"
              end="22:00"
              style="width: 140px"
            />
            <span class="time-sep">—</span>
            <el-time-select
              v-model="form.endTime"
              :min-time="form.startTime || '08:00'"
              placeholder="结束"
              start="08:00"
              step="00:30"
              end="22:00"
              style="width: 140px"
            />
          </div>
        </el-form-item>
        <el-form-item label="起止日期" prop="startDate">
          <div class="time-row">
            <el-date-picker
              v-model="form.startDate"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              style="width: 180px"
            />
            <span class="time-sep">—</span>
            <el-date-picker
              v-model="form.endDate"
              type="date"
              placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 180px"
            />
          </div>
        </el-form-item>
        <el-form-item label="班级" prop="className">
          <el-input v-model="form.className" placeholder="如：计科2101" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? "保存中..." : "保存" }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-courses {
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

.time-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.time-sep {
  color: #909399;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
