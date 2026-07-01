<script setup lang="ts">
import { computed, reactive, ref, onMounted } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import {
  EQUIPMENT_STATUS_LABELS,
  type Equipment,
  type BorrowCreateRequest,
} from "@labreserve/shared";
import * as equipmentApi from "@/api/equipment";
import * as borrowsApi from "@/api/borrows";

const loading = ref(false);
const equipments = ref<Equipment[]>([]);
const submitting = ref(false);
const formRef = ref<FormInstance>();

const form = reactive({
  equipmentId: undefined as number | undefined,
  borrowDate: "",
  expectedReturn: "",
  purpose: "",
});

const rules: FormRules = {
  equipmentId: [{ required: true, message: "请选择设备", trigger: "change" }],
  borrowDate: [{ required: true, message: "请选择借用日期", trigger: "change" }],
  expectedReturn: [{ required: true, message: "请选择预计归还日期", trigger: "change" }],
  purpose: [{ required: true, message: "请填写借用用途", trigger: "blur" }],
};

const selectedEquipment = computed(() => {
  if (!form.equipmentId) return null;
  return equipments.value.find((e) => Number(e.id) === form.equipmentId) ?? null;
});

const borrowDays = computed(() => {
  if (!form.borrowDate || !form.expectedReturn) return null;
  const start = new Date(form.borrowDate);
  const end = new Date(form.expectedReturn);
  return Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
});

function disabledBorrowDate(date: Date) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date < today;
}

function disabledReturnDate(date: Date) {
  if (!form.borrowDate) return false;
  const borrowDate = new Date(form.borrowDate);
  borrowDate.setHours(0, 0, 0, 0);
  return date <= borrowDate;
}

function onBorrowDateChange() {
  if (form.expectedReturn && form.borrowDate) {
    if (new Date(form.expectedReturn) <= new Date(form.borrowDate)) {
      form.expectedReturn = "";
    }
  }
}

async function fetchAvailableEquipments() {
  loading.value = true;
  try {
    const result = await equipmentApi.getEquipments({ status: "AVAILABLE", pageSize: 1000 });
    equipments.value = result.records;
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  if (!form.borrowDate || !form.expectedReturn) {
    ElMessage.warning("请选择借用日期和预计归还日期");
    return;
  }

  if (form.expectedReturn < form.borrowDate) {
    ElMessage.warning("预计归还日期不能早于借用日期");
    return;
  }

  submitting.value = true;
  try {
    const data: BorrowCreateRequest = {
      equipmentId: String(form.equipmentId ?? ""),
      borrowDate: form.borrowDate,
      expectedReturn: form.expectedReturn,
      purpose: form.purpose,
    };
    await borrowsApi.createBorrow(data);
    ElMessage.success("借用申请已提交");
    form.equipmentId = undefined;
    form.borrowDate = "";
    form.expectedReturn = "";
    form.purpose = "";
    formRef.value?.resetFields();
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  fetchAvailableEquipments();
});
</script>

<template>
  <div class="borrow-request">
    <h1>设备借用申请</h1>

    <el-card style="max-width: 600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="选择设备" prop="equipmentId">
          <el-select
            v-model="form.equipmentId"
            style="width: 100%"
            placeholder="请选择可借用的设备"
          >
            <el-option
              v-for="eq in equipments"
              :key="eq.id"
              :label="`${eq.name} (${eq.model ?? '-'})`"
              :value="Number(eq.id)"
            >
              <span>{{ eq.name }}</span>
              <span style="color: #909399; margin-left: 8px; font-size: 13px">
                {{ eq.model ?? "" }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="借用日期" prop="borrowDate">
          <el-date-picker
            v-model="form.borrowDate"
            type="date"
            placeholder="选择借用日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledBorrowDate"
            style="width: 100%"
            @change="onBorrowDateChange"
          />
        </el-form-item>
        <el-form-item label="预计归还日期" prop="expectedReturn">
          <el-date-picker
            v-model="form.expectedReturn"
            type="date"
            placeholder="选择预计归还日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledReturnDate"
            style="width: 100%"
          />
        </el-form-item>
        <template v-if="borrowDays !== null && borrowDays > 0">
          <div class="borrow-hint">预计借用 {{ borrowDays }} 天</div>
        </template>
        <el-form-item label="借用用途" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="3" placeholder="请说明借用用途" />
        </el-form-item>

        <el-card v-if="selectedEquipment" class="equipment-card" shadow="never">
          <template #header>设备信息</template>
          <div class="equipment-detail">
            <p><span class="label">名称：</span>{{ selectedEquipment.name }}</p>
            <p v-if="selectedEquipment.model">
              <span class="label">型号：</span>{{ selectedEquipment.model }}
            </p>
            <p><span class="label">序列号：</span>{{ selectedEquipment.serialNumber }}</p>
            <p v-if="selectedEquipment.labName">
              <span class="label">所属实验室：</span>{{ selectedEquipment.labName }}
            </p>
            <el-tag
              size="small"
              :type="selectedEquipment.status === 'AVAILABLE' ? 'success' : 'info'"
            >
              {{ EQUIPMENT_STATUS_LABELS[selectedEquipment.status] ?? selectedEquipment.status }}
            </el-tag>
          </div>
        </el-card>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ submitting ? "提交中..." : "提交申请" }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.borrow-request {
  padding: 24px;
}

h1 {
  margin: 0 0 20px;
  font-size: 24px;
}

.borrow-hint {
  color: #909399;
  font-size: 13px;
  margin-bottom: 18px;
  padding-left: 120px;
}

.equipment-card {
  margin-bottom: 18px;
}

.equipment-detail p {
  margin: 0 0 6px;
  font-size: 14px;
  line-height: 1.6;
}

.equipment-detail .label {
  color: #909399;
}
</style>
