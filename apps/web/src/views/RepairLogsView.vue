<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { REPAIR_STATUS_LABELS } from "@labreserve/shared";
import type { RepairLog, RepairLogCreateRequest } from "@labreserve/shared";
import * as repairLogsApi from "@/api/repairLogs";
import * as equipmentApi from "@/api/equipment";
import type { Equipment } from "@labreserve/shared";

const loading = ref(false);
const repairLogs = ref<RepairLog[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);

const filterEquipmentId = ref<number | undefined>(undefined);
const filterStatus = ref("");
const equipments = ref<Equipment[]>([]);

const dialogVisible = ref(false);
const dialogTitle = ref("提交报修");
const isEdit = ref(false);
const editId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  equipmentId: undefined as number | undefined,
  description: "",
});

const rules: FormRules = {
  equipmentId: [{ required: true, message: "请选择设备", trigger: "change" }],
  description: [{ required: true, message: "请输入故障描述", trigger: "blur" }],
};

const statusTagType: Record<string, string> = {
  PENDING: "danger",
  IN_PROGRESS: "warning",
  COMPLETED: "success",
};

async function fetchRepairLogs() {
  loading.value = true;
  try {
    const params: repairLogsApi.RepairLogQueryParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (filterEquipmentId.value) params.equipmentId = filterEquipmentId.value;
    if (filterStatus.value) params.status = filterStatus.value;
    const result = await repairLogsApi.getRepairLogs(params);
    repairLogs.value = result.records;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

async function fetchEquipments() {
  try {
    const result = await equipmentApi.getEquipments({ pageSize: 1000 });
    equipments.value = result.records;
  } catch {
    // ignore
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchRepairLogs();
}

function handleReset() {
  filterEquipmentId.value = undefined;
  filterStatus.value = "";
  pageNum.value = 1;
  fetchRepairLogs();
}

function showCreateDialog() {
  dialogTitle.value = "提交报修";
  isEdit.value = false;
  editId.value = null;
  form.equipmentId = undefined;
  form.description = "";
  dialogVisible.value = true;
}

function showEditDialog(row: RepairLog) {
  dialogTitle.value = "编辑报修";
  isEdit.value = true;
  editId.value = Number(row.id);
  form.equipmentId = Number(row.equipmentId);
  form.description = row.description;
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    if (isEdit.value && editId.value) {
      await repairLogsApi.updateRepairLog(editId.value, { description: form.description });
      ElMessage.success("更新成功");
    } else {
      const data: RepairLogCreateRequest = {
        equipmentId: String(form.equipmentId!),
        description: form.description,
      };
      await repairLogsApi.createRepairLog(data);
      ElMessage.success("报修提交成功");
    }
    dialogVisible.value = false;
    fetchRepairLogs();
  } finally {
    submitting.value = false;
  }
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
async function handleDelete(_rowId: number) {
  // Call update with empty to effectively remove (repair logs don't have a dedicated delete endpoint; we delete via the update endpoint with admin role)
  // Actually, the RepaireLogController has no DELETE. Users can only update status or description.
  // For now we just provide this as a placeholder if needed - but since there's no DELETE endpoint,
  // let's just skip it. Actually the wireframe shows edit/delete. Let's keep it simple.
  ElMessage.warning("报修记录无法删除，请更新状态标记完成");
}

async function handleStatusChange(row: RepairLog) {
  const nextStatus = row.status === "PENDING" ? "IN_PROGRESS" : "COMPLETED";
  const label = REPAIR_STATUS_LABELS[nextStatus as keyof typeof REPAIR_STATUS_LABELS] || nextStatus;
  try {
    await ElMessageBox.confirm(`确定将状态更新为「${label}」吗？`, "更新状态", { type: "info" });
    await repairLogsApi.updateRepairLogStatus(Number(row.id), nextStatus);
    ElMessage.success("状态更新成功");
    fetchRepairLogs();
  } catch {
    // cancelled
  }
}

function handlePageChange(page: number) {
  pageNum.value = page;
  fetchRepairLogs();
}

const equipmentNameMap = ref<Record<string, string>>({});

onMounted(async () => {
  await fetchEquipments();
  equipmentNameMap.value = {};
  equipments.value.forEach((e) => {
    equipmentNameMap.value[String(e.id)] = e.name;
  });
  fetchRepairLogs();
});
</script>

<template>
  <div class="repair-logs">
    <div class="search-bar">
      <el-select v-model="filterEquipmentId" placeholder="设备" clearable style="width: 200px">
        <el-option v-for="eq in equipments" :key="eq.id" :label="eq.name" :value="Number(eq.id)" />
      </el-select>
      <el-select
        v-model="filterStatus"
        placeholder="状态"
        clearable
        style="width: 140px; margin-left: 12px"
      >
        <el-option label="待处理" value="PENDING" />
        <el-option label="处理中" value="IN_PROGRESS" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
      <el-button type="primary" style="margin-left: 12px" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">清空</el-button>
    </div>

    <div class="action-bar">
      <el-button type="primary" @click="showCreateDialog">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="repairLogs" border stripe style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column label="设备名称" min-width="160">
        <template #default="{ row }">
          {{ row.equipmentName || equipmentNameMap[String(row.equipmentId)] || "-" }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="报修时间" width="180" />
      <el-table-column prop="description" label="故障描述" min-width="200" />
      <el-table-column prop="reporterName" label="报修人" width="100" />
      <el-table-column label="维修状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType[row.status] || 'info'" size="small">
            {{
              REPAIR_STATUS_LABELS[row.status as keyof typeof REPAIR_STATUS_LABELS] || row.status
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(Number(row.id))"
            >删除</el-button
          >
          <el-button
            v-if="row.status !== 'COMPLETED'"
            type="warning"
            link
            size="small"
            @click="handleStatusChange(row)"
          >
            {{ row.status === "PENDING" ? "开始维修" : "完成维修" }}
          </el-button>
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
        <el-form-item label="设备" prop="equipmentId">
          <el-select
            v-model="form.equipmentId"
            placeholder="请选择设备"
            style="width: 100%"
            :disabled="isEdit"
          >
            <el-option
              v-for="eq in equipments"
              :key="eq.id"
              :label="eq.name"
              :value="Number(eq.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入问题故障现象"
          />
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
.repair-logs {
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
