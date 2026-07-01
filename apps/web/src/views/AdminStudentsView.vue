<script setup lang="ts">
import { reactive, ref, onMounted, computed } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import type { Student, StudentCreateRequest, StudentUpdateRequest } from "@labreserve/shared";
import * as studentsApi from "@/api/students";
import * as labsApi from "@/api/labs";
import type { Lab } from "@labreserve/shared";

const loading = ref(false);
const students = ref<Student[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);

const filterLabId = ref<number | undefined>(undefined);
const filterName = ref("");
const labs = ref<Lab[]>([]);
const selectedIds = ref<number[]>([]);

const dialogVisible = ref(false);
const dialogTitle = ref("录入学生");
const isEdit = ref(false);
const editId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  labId: undefined as number | undefined,
  name: "",
  gender: "" as string,
  age: undefined as number | undefined,
  address: "",
});

const rules: FormRules = {
  labId: [{ required: true, message: "请选择实验室分室", trigger: "change" }],
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
};

const genderOptions = [
  { label: "男", value: "MALE" },
  { label: "女", value: "FEMALE" },
];

async function fetchStudents() {
  loading.value = true;
  try {
    const params: studentsApi.StudentQueryParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (filterLabId.value) params.labId = filterLabId.value;
    if (filterName.value) params.name = filterName.value;
    const result = await studentsApi.getStudents(params);
    students.value = result.records;
    total.value = result.total;
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

function handleSearch() {
  pageNum.value = 1;
  fetchStudents();
}

function handleReset() {
  filterLabId.value = undefined;
  filterName.value = "";
  pageNum.value = 1;
  fetchStudents();
}

function showCreateDialog() {
  dialogTitle.value = "录入学生";
  isEdit.value = false;
  editId.value = null;
  form.labId = undefined;
  form.name = "";
  form.gender = "";
  form.age = undefined;
  form.address = "";
  dialogVisible.value = true;
}

function showEditDialog(row: Student) {
  dialogTitle.value = "编辑学生";
  isEdit.value = true;
  editId.value = Number(row.id);
  form.labId = Number(row.labId);
  form.name = row.name;
  form.gender = row.gender || "";
  form.age = row.age ?? undefined;
  form.address = row.address || "";
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    if (isEdit.value && editId.value) {
      const data: StudentUpdateRequest = {};
      if (form.labId) data.labId = String(form.labId);
      if (form.name) data.name = form.name;
      if (form.gender !== undefined) data.gender = form.gender as "MALE" | "FEMALE" | undefined;
      if (form.age !== undefined) data.age = form.age;
      if (form.address !== undefined) data.address = form.address;
      await studentsApi.updateStudent(editId.value, data);
      ElMessage.success("更新成功");
    } else {
      const data: StudentCreateRequest = {
        labId: String(form.labId!),
        name: form.name,
      };
      if (form.gender) data.gender = form.gender as "MALE" | "FEMALE";
      if (form.age !== undefined) data.age = form.age;
      if (form.address) data.address = form.address;
      await studentsApi.createStudent(data);
      ElMessage.success("录入成功");
    }
    dialogVisible.value = false;
    fetchStudents();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm("确定要删除该学生信息吗？", "确认删除", {
      type: "warning",
    });
    await studentsApi.deleteStudent(id);
    ElMessage.success("删除成功");
    fetchStudents();
  } catch {
    // cancelled
  }
}

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning("请先选择学生");
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 条学生信息吗？`,
      "批量删除",
      { type: "warning" },
    );
    for (const id of selectedIds.value) {
      await studentsApi.deleteStudent(id);
    }
    ElMessage.success("批量删除成功");
    selectedIds.value = [];
    fetchStudents();
  } catch {
    // cancelled
  }
}

function handleSelectionChange(selection: Student[]) {
  selectedIds.value = selection.map((s) => Number(s.id));
}

function handlePageChange(page: number) {
  pageNum.value = page;
  fetchStudents();
}

const labNameMap = computed(() => {
  const map: Record<string, string> = {};
  labs.value.forEach((l) => {
    map[String(l.id)] = l.name;
  });
  return map;
});

onMounted(() => {
  fetchLabs();
  fetchStudents();
});
</script>

<template>
  <div class="admin-students">
    <div class="search-bar">
      <el-select v-model="filterLabId" placeholder="实验室分室" clearable style="width: 200px">
        <el-option v-for="lab in labs" :key="lab.id" :label="lab.name" :value="Number(lab.id)" />
      </el-select>
      <el-input
        v-model="filterName"
        placeholder="姓名"
        clearable
        style="width: 200px; margin-left: 12px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" style="margin-left: 12px" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">清空</el-button>
    </div>

    <div class="action-bar">
      <el-button type="primary" @click="showCreateDialog">新增</el-button>
      <el-button type="danger" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="students"
      border
      stripe
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column label="实验室分室" min-width="180">
        <template #default="{ row }">
          {{ labNameMap[String(row.labId)] || "-" }}
        </template>
      </el-table-column>
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column label="性别" width="70">
        <template #default="{ row }">
          {{ row.gender === "MALE" ? "男" : row.gender === "FEMALE" ? "女" : "-" }}
        </template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="70" />
      <el-table-column prop="address" label="地址" min-width="150" />
      <el-table-column prop="creatorName" label="创建人" width="100" />
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(Number(row.id))"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <span>共{{ total }}条</span>
      <el-pagination
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      @close="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="实验室分室" prop="labId">
          <el-select v-model="form.labId" placeholder="请选择实验室" style="width: 100%">
            <el-option
              v-for="lab in labs"
              :key="lab.id"
              :label="lab.name"
              :value="Number(lab.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" placeholder="请选择性别" style="width: 100%">
            <el-option
              v-for="opt in genderOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="form.age" :min="1" :max="120" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-students {
  padding: 0;
}
.search-bar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
.action-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}
.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
