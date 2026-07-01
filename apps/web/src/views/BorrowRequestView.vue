<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import type { Equipment, BorrowCreateRequest } from "@labreserve/shared";
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
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预计归还日期" prop="expectedReturn">
          <el-date-picker
            v-model="form.expectedReturn"
            type="date"
            placeholder="选择预计归还日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="借用用途" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="3" placeholder="请说明借用用途" />
        </el-form-item>
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
</style>
