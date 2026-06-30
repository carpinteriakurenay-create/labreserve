<script setup lang="ts">
import { reactive, ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import type { Lab, BookingCreateRequest } from "@labreserve/shared";
import * as labsApi from "@/api/labs";
import * as bookingsApi from "@/api/bookings";
import TimeSlotPicker from "@/components/TimeSlotPicker.vue";

const route = useRoute();
const router = useRouter();

const submitting = ref(false);
const labs = ref<Lab[]>([]);
const formRef = ref<FormInstance>();
const selectedSlot = ref<{ startTime: string; endTime: string } | null>(null);
const slotRefreshKey = ref(0);

const form = reactive({
  labId: null as number | null,
  date: "",
  purpose: "",
  personCount: 1,
});

const rules: FormRules = {
  labId: [{ required: true, message: "请选择实验室", trigger: "change" }],
  date: [{ required: true, message: "请选择日期", trigger: "change" }],
  purpose: [
    { required: true, message: "请输入预约用途", trigger: "blur" },
    { max: 500, message: "用途不能超过500个字符", trigger: "blur" },
  ],
};

const hasSelectedSlot = computed(() => selectedSlot.value !== null);

const slotRule: FormRules = {
  selectedSlot: [
    {
      validator: (_rule, _value, callback) => {
        if (hasSelectedSlot.value) callback();
        else callback(new Error("请选择时间段"));
      },
      trigger: "change",
    },
  ],
};

const mergedRules = computed(() => ({ ...rules, ...slotRule }));

function disabledDate(time: Date): boolean {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return time.getTime() < today.getTime();
}

async function fetchLabs() {
  try {
    const result = await labsApi.getLabs({ pageNum: 1, pageSize: 200 });
    labs.value = result.records;
  } catch {
    // error handled by interceptor
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid || !selectedSlot.value) return;

  if (!form.labId) {
    ElMessage.warning("请选择实验室");
    return;
  }

  submitting.value = true;
  try {
    const data: BookingCreateRequest = {
      labId: String(form.labId),
      date: form.date,
      startTime: selectedSlot.value.startTime,
      endTime: selectedSlot.value.endTime,
      purpose: form.purpose,
      personCount: form.personCount,
    };
    await bookingsApi.createBooking(data);
    ElMessage.success("预约提交成功");
    router.push("/bookings/mine");
  } catch {
    slotRefreshKey.value++;
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  fetchLabs();
  const labIdParam = route.query.labId;
  if (labIdParam && typeof labIdParam === "string") {
    form.labId = Number(labIdParam);
  }
});
</script>

<template>
  <div class="booking-create">
    <h1>预约实验室</h1>

    <el-card>
      <el-form ref="formRef" :model="form" :rules="mergedRules" label-width="100px">
        <el-form-item label="实验室" prop="labId">
          <el-select
            v-model="form.labId"
            placeholder="请选择实验室"
            style="width: 100%; max-width: 400px"
          >
            <el-option
              v-for="lab in labs"
              :key="lab.id"
              :label="`${lab.name} (${lab.location || '-'})`"
              :value="Number(lab.id)"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="预约日期" prop="date">
          <el-date-picker
            v-model="form.date"
            type="date"
            placeholder="请选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
          />
        </el-form-item>

        <el-form-item label="时间段" required>
          <div class="slot-picker-wrapper">
            <TimeSlotPicker
              :key="slotRefreshKey"
              :lab-id="form.labId"
              :date="form.date"
              v-model:selected-slot="selectedSlot"
            />
            <div v-if="!hasSelectedSlot" class="slot-hint">请点击上方绿色时段进行选择</div>
          </div>
        </el-form-item>

        <el-form-item label="预约用途" prop="purpose">
          <el-input
            v-model="form.purpose"
            type="textarea"
            :maxlength="500"
            show-word-limit
            :rows="4"
            placeholder="请描述预约用途、实验内容等"
          />
        </el-form-item>

        <el-form-item label="使用人数">
          <el-input-number v-model="form.personCount" :min="1" :max="999" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ submitting ? "提交中..." : "提交预约" }}
          </el-button>
          <el-button @click="router.push('/labs')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.booking-create {
  padding: 24px;
  max-width: 720px;
}

h1 {
  margin: 0 0 20px;
  font-size: 24px;
}

.slot-picker-wrapper {
  width: 100%;
}

.slot-hint {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
