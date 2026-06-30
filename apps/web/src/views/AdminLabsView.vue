<script setup lang="ts">
import { reactive, ref, onMounted, computed } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { LAB_STATUS_LABELS } from "@labreserve/shared";
import type { Lab, LabCreateRequest, LabUpdateRequest, LabHours } from "@labreserve/shared";
import * as labsApi from "@/api/labs";

const DAY_OF_WEEK_LABELS: Record<number, string> = {
  1: "周一",
  2: "周二",
  3: "周三",
  4: "周四",
  5: "周五",
  6: "周六",
  7: "周日",
};

// ---- Tab 1: CRUD ----
const loading = ref(false);
const labs = ref<Lab[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);

const filterName = ref("");
const filterStatus = ref("");

const dialogVisible = ref(false);
const dialogTitle = ref("创建实验室");
const isEdit = ref(false);
const editLabId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  name: "",
  location: "",
  capacity: 1,
  description: "",
  imageUrl: "",
  managerId: "",
});

const createRules: FormRules = {
  name: [
    { required: true, message: "请输入实验室名称", trigger: "blur" },
    { min: 1, max: 100, message: "名称长度为1-100个字符", trigger: "blur" },
  ],
  capacity: [
    { required: true, message: "请输入容量", trigger: "blur" },
    { type: "number", min: 1, message: "容量至少为1", trigger: "blur" },
  ],
};

const editRules: FormRules = {
  name: [{ min: 1, max: 100, message: "名称长度为1-100个字符", trigger: "blur" }],
  capacity: [{ type: "number", min: 1, message: "容量至少为1", trigger: "blur" }],
};

const rules = computed(() => (isEdit.value ? editRules : createRules));

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

function openCreate() {
  isEdit.value = false;
  editLabId.value = null;
  dialogTitle.value = "创建实验室";
  form.name = "";
  form.location = "";
  form.capacity = 1;
  form.description = "";
  form.imageUrl = "";
  form.managerId = "";
  dialogVisible.value = true;
}

function openEdit(lab: Lab) {
  isEdit.value = true;
  editLabId.value = Number(lab.id);
  dialogTitle.value = "编辑实验室";
  form.name = lab.name;
  form.location = lab.location || "";
  form.capacity = lab.capacity;
  form.description = lab.description || "";
  form.imageUrl = lab.imageUrl || "";
  form.managerId = lab.managerId || "";
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    if (isEdit.value && editLabId.value) {
      const data: LabUpdateRequest = {
        name: form.name,
        location: form.location,
        capacity: form.capacity,
        description: form.description || undefined,
        imageUrl: form.imageUrl || undefined,
        managerId: form.managerId || undefined,
      };
      await labsApi.updateLab(editLabId.value, data);
      ElMessage.success("更新成功");
    } else {
      const data: LabCreateRequest = {
        name: form.name,
        location: form.location,
        capacity: form.capacity,
        description: form.description || undefined,
        imageUrl: form.imageUrl || undefined,
        managerId: form.managerId || undefined,
      };
      await labsApi.createLab(data);
      ElMessage.success("创建成功");
    }
    dialogVisible.value = false;
    fetchLabs();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(lab: Lab) {
  try {
    await ElMessageBox.confirm(`确定要删除实验室「${lab.name}」吗？此操作不可恢复。`, "删除确认", {
      confirmButtonText: "确定删除",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  await labsApi.deleteLab(Number(lab.id));
  ElMessage.success("删除成功");
  fetchLabs();
}

async function handleToggleStatus(lab: Lab) {
  await labsApi.toggleLabStatus(Number(lab.id));
  ElMessage.success("状态已切换");
  fetchLabs();
}

function statusTagType(status: string): "success" | "warning" | "danger" {
  if (status === "AVAILABLE") return "success";
  if (status === "MAINTENANCE") return "warning";
  return "danger";
}

// ---- Tab 2: Open Hours ----
const activeTab = ref("crud");
const hoursLoading = ref(false);
const allLabs = ref<Lab[]>([]);
const selectedLabId = ref<number | null>(null);
const selectedLabName = ref("");

interface DayHours {
  dayOfWeek: number;
  openTime: string;
  closeTime: string;
}

const hoursData = reactive<Record<number, DayHours>>({
  1: { dayOfWeek: 1, openTime: "", closeTime: "" },
  2: { dayOfWeek: 2, openTime: "", closeTime: "" },
  3: { dayOfWeek: 3, openTime: "", closeTime: "" },
  4: { dayOfWeek: 4, openTime: "", closeTime: "" },
  5: { dayOfWeek: 5, openTime: "", closeTime: "" },
  6: { dayOfWeek: 6, openTime: "", closeTime: "" },
  7: { dayOfWeek: 7, openTime: "", closeTime: "" },
});

async function loadAllLabs() {
  try {
    const result = await labsApi.getLabs({ pageNum: 1, pageSize: 200 });
    allLabs.value = result.records;
  } catch {
    // error handled by interceptor
  }
}

async function loadHoursForLab(labId: number) {
  hoursLoading.value = true;
  try {
    const existingHours = await labsApi.getLabHours(labId);
    for (let d = 1; d <= 7; d++) {
      hoursData[d] = { dayOfWeek: d, openTime: "", closeTime: "" };
    }
    for (const h of existingHours) {
      const entry = hoursData[h.dayOfWeek];
      if (entry) {
        entry.openTime = h.openTime;
        entry.closeTime = h.closeTime;
      }
    }
  } finally {
    hoursLoading.value = false;
  }
}

async function handleLabSelect(labId: number) {
  selectedLabId.value = labId;
  const lab = allLabs.value.find((l) => Number(l.id) === labId);
  selectedLabName.value = lab?.name || "";
  await loadHoursForLab(labId);
}

function getDayHours(d: number): DayHours {
  return hoursData[d]!;
}

async function saveHours() {
  if (!selectedLabId.value) return;
  const hours: Omit<LabHours, "id" | "labId">[] = [];
  for (let d = 1; d <= 7; d++) {
    const h = getDayHours(d);
    if (h.openTime && h.closeTime) {
      hours.push({ dayOfWeek: d, openTime: h.openTime, closeTime: h.closeTime });
    }
  }
  if (hours.length === 0) {
    ElMessage.warning("请至少设置一天的开放时间");
    return;
  }
  try {
    await labsApi.replaceLabHours(selectedLabId.value, hours);
    ElMessage.success("开放时间保存成功");
  } catch {
    // error handled by interceptor
  }
}

function handleTabChange(tab: string) {
  if (tab === "hours" && allLabs.value.length === 0) {
    loadAllLabs();
  }
}

onMounted(() => {
  fetchLabs();
});
</script>

<template>
  <div class="admin-labs">
    <h1>实验室管理</h1>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- Tab 1: CRUD -->
      <el-tab-pane label="实验室管理" name="crud">
        <div class="tab-crud">
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
              <el-button type="primary" style="margin-left: 12px" @click="handleSearch"
                >查询</el-button
              >
              <el-button @click="handleReset">重置</el-button>
            </div>
            <el-button type="primary" @click="openCreate">创建实验室</el-button>
          </div>

          <el-table :data="labs" v-loading="loading" stripe>
            <el-table-column prop="name" label="名称" min-width="180" />
            <el-table-column prop="location" label="位置" min-width="160">
              <template #default="{ row }">{{ row.location || "-" }}</template>
            </el-table-column>
            <el-table-column prop="capacity" label="容量" width="80" align="center" />
            <el-table-column prop="equipmentNum" label="设备数" width="80" align="center" />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">
                  {{
                    LAB_STATUS_LABELS[row.status as keyof typeof LAB_STATUS_LABELS] ?? row.status
                  }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="managerName" label="管理员" width="120">
              <template #default="{ row }">{{ row.managerName || "-" }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button size="small" text type="warning" @click="handleToggleStatus(row)">
                  {{ row.status === "AVAILABLE" ? "关闭" : "开放" }}
                </el-button>
                <el-button size="small" text type="danger" @click="handleDelete(row)"
                  >删除</el-button
                >
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
        </div>
      </el-tab-pane>

      <!-- Tab 2: Open Hours -->
      <el-tab-pane label="开放时间管理" name="hours">
        <div class="tab-hours">
          <div class="hours-header">
            <span class="hours-label">选择实验室：</span>
            <el-select
              v-model="selectedLabId"
              placeholder="请选择实验室"
              style="width: 280px"
              @change="handleLabSelect"
            >
              <el-option
                v-for="lab in allLabs"
                :key="lab.id"
                :label="lab.name"
                :value="Number(lab.id)"
              />
            </el-select>
          </div>

          <template v-if="selectedLabId">
            <h3>{{ selectedLabName }} — 开放时间设置</h3>
            <div v-loading="hoursLoading" class="hours-editor">
              <div v-for="d in 7" :key="d" class="hours-edit-row">
                <span class="day-col">{{ DAY_OF_WEEK_LABELS[d] }}</span>
                <el-input
                  v-model="getDayHours(d).openTime"
                  placeholder="开始 (HH:mm)"
                  style="width: 130px"
                />
                <span class="dash">—</span>
                <el-input
                  v-model="getDayHours(d).closeTime"
                  placeholder="结束 (HH:mm)"
                  style="width: 130px"
                />
              </div>
              <el-button type="primary" style="margin-top: 20px" @click="saveHours"
                >保存开放时间</el-button
              >
            </div>
          </template>
          <el-empty v-else description="请先选择实验室" :image-size="80" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="550px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="1-100个字符" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" placeholder="教学楼X-XXX" />
        </el-form-item>
        <el-form-item label="容量" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
        <el-form-item label="图片URL" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="选填" />
        </el-form-item>
        <el-form-item label="管理员ID" prop="managerId">
          <el-input v-model="form.managerId" placeholder="选填" />
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
.admin-labs {
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

.tab-crud,
.tab-hours {
  min-height: 300px;
}

.hours-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.hours-label {
  margin-right: 12px;
  font-weight: 600;
}

.hours-editor {
  max-width: 400px;
}

.hours-edit-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.day-col {
  width: 60px;
  font-weight: 600;
  color: #303133;
}

.dash {
  margin: 0 8px;
  color: #909399;
}

.tab-hours h3 {
  margin: 0 0 16px;
  font-size: 16px;
}
</style>
