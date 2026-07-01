<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { EQUIPMENT_STATUS_LABELS } from "@labreserve/shared";
import type { Equipment, EquipmentCreateRequest, EquipmentUpdateRequest } from "@labreserve/shared";
import * as equipmentApi from "@/api/equipment";
import * as labsApi from "@/api/labs";
import type { Lab } from "@labreserve/shared";

const loading = ref(false);
const equipments = ref<Equipment[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);

const filterLabId = ref<number | undefined>(undefined);
const filterStatus = ref("");
const filterName = ref("");

const labs = ref<Lab[]>([]);

const dialogVisible = ref(false);
const dialogTitle = ref("登记设备");
const isEdit = ref(false);
const editId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  labId: undefined as number | undefined,
  name: "",
  model: "",
  serialNumber: "",
  description: "",
});

const rules: FormRules = {
  labId: [{ required: true, message: "请选择所属实验室", trigger: "change" }],
  name: [{ required: true, message: "请输入设备名称", trigger: "blur" }],
  serialNumber: [
    { required: true, message: "请输入序列号", trigger: "blur" },
    {
      validator: (_rule, value, callback) => {
        if (!value || String(value).trim().length < 2) {
          callback(new Error("序列号至少 2 个字符"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
};

async function fetchEquipments() {
  loading.value = true;
  try {
    const params: equipmentApi.EquipmentQueryParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (filterLabId.value) params.labId = filterLabId.value;
    if (filterStatus.value) params.status = filterStatus.value;
    if (filterName.value) params.name = filterName.value;
    const result = await equipmentApi.getEquipments(params);
    equipments.value = result.records;
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
  fetchEquipments();
}

function handleReset() {
  filterLabId.value = undefined;
  filterStatus.value = "";
  filterName.value = "";
  pageNum.value = 1;
  fetchEquipments();
}

function handlePageChange(p: number) {
  pageNum.value = p;
  fetchEquipments();
}

function handleSizeChange(s: number) {
  pageSize.value = s;
  pageNum.value = 1;
  fetchEquipments();
}

function openCreate() {
  isEdit.value = false;
  editId.value = null;
  dialogTitle.value = "登记设备";
  form.labId = undefined;
  form.name = "";
  form.model = "";
  form.serialNumber = "";
  form.description = "";
  dialogVisible.value = true;
}

function openEdit(equipment: Equipment) {
  isEdit.value = true;
  editId.value = Number(equipment.id);
  dialogTitle.value = "编辑设备";
  form.labId = Number(equipment.labId);
  form.name = equipment.name;
  form.model = equipment.model ?? "";
  form.serialNumber = equipment.serialNumber;
  form.description = equipment.description ?? "";
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    if (isEdit.value && editId.value) {
      const data: EquipmentUpdateRequest = {
        labId: String(form.labId ?? ""),
        name: form.name,
        model: form.model || undefined,
        serialNumber: form.serialNumber,
        description: form.description || undefined,
      };
      await equipmentApi.updateEquipment(editId.value, data);
      ElMessage.success("更新成功");
    } else {
      const data: EquipmentCreateRequest = {
        labId: String(form.labId ?? ""),
        name: form.name,
        model: form.model || undefined,
        serialNumber: form.serialNumber,
        description: form.description || undefined,
      };
      await equipmentApi.createEquipment(data);
      ElMessage.success("创建成功");
    }
    dialogVisible.value = false;
    fetchEquipments();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(equipment: Equipment) {
  try {
    await ElMessageBox.confirm(
      `确定要删除设备「${equipment.name}」吗？此操作不可恢复。`,
      "删除确认",
      { confirmButtonText: "确定删除", cancelButtonText: "取消", type: "warning" },
    );
  } catch {
    return;
  }
  await equipmentApi.deleteEquipment(Number(equipment.id));
  ElMessage.success("删除成功");
  fetchEquipments();
}

async function handleMarkMaintenance(equipment: Equipment) {
  try {
    await ElMessageBox.confirm(`确定要将设备「${equipment.name}」标记为"维修中"吗？`, "报修确认", {
      confirmButtonText: "确认",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  await equipmentApi.updateEquipmentStatus(Number(equipment.id), "MAINTENANCE");
  ElMessage.success("设备已标记为维修中");
  fetchEquipments();
}

async function handleRestoreAvailable(equipment: Equipment) {
  await equipmentApi.updateEquipmentStatus(Number(equipment.id), "AVAILABLE");
  ElMessage.success("设备已恢复为可用");
  fetchEquipments();
}

function statusTagType(status: string) {
  return status === "AVAILABLE" ? "success" : status === "BORROWED" ? "warning" : "danger";
}

onMounted(() => {
  fetchLabs();
  fetchEquipments();
});
</script>

<template>
  <div class="admin-equipment">
    <h1>设备管理</h1>

    <div class="toolbar">
      <div class="filters">
        <el-select
          v-model="filterLabId"
          placeholder="所属实验室"
          clearable
          style="width: 180px"
          @change="handleSearch"
        >
          <el-option v-for="lab in labs" :key="lab.id" :label="lab.name" :value="Number(lab.id)" />
        </el-select>
        <el-select
          v-model="filterStatus"
          placeholder="设备状态"
          clearable
          style="width: 140px; margin-left: 12px"
          @change="handleSearch"
        >
          <el-option
            v-for="(label, key) in EQUIPMENT_STATUS_LABELS"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-input
          v-model="filterName"
          placeholder="搜索设备名称"
          clearable
          style="width: 200px; margin-left: 12px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button style="margin-left: 12px" type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-button type="primary" @click="openCreate">登记设备</el-button>
    </div>

    <el-table :data="equipments" v-loading="loading" stripe>
      <el-table-column prop="name" label="设备名称" min-width="150" />
      <el-table-column prop="model" label="型号" min-width="120">
        <template #default="{ row }">{{ row.model || "-" }}</template>
      </el-table-column>
      <el-table-column prop="serialNumber" label="序列号" min-width="140" />
      <el-table-column prop="labName" label="所属实验室" min-width="150">
        <template #default="{ row }">{{ row.labName || "-" }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{
              EQUIPMENT_STATUS_LABELS[row.status as keyof typeof EQUIPMENT_STATUS_LABELS] ??
              row.status
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
          <el-button
            v-if="row.status !== 'MAINTENANCE' && row.status !== 'BORROWED'"
            size="small"
            text
            type="warning"
            @click="handleMarkMaintenance(row)"
          >
            报修
          </el-button>
          <el-button
            v-if="row.status === 'MAINTENANCE'"
            size="small"
            text
            type="success"
            @click="handleRestoreAvailable(row)"
          >
            恢复可用
          </el-button>
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
      width="500px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
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
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="如：FPGA 开发板" />
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="form.model" placeholder="如：Xilinx Artix-7" />
        </el-form-item>
        <el-form-item label="序列号" prop="serialNumber">
          <el-input v-model="form.serialNumber" placeholder="唯一序列号" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="设备描述（选填）"
          />
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
.admin-equipment {
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
