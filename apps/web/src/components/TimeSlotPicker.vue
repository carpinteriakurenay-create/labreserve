<script setup lang="ts">
import { ref, watch } from "vue";
import type { TimeSlot } from "@labreserve/shared";
import * as bookingsApi from "@/api/bookings";

const props = withDefaults(
  defineProps<{
    labId: number | string | null;
    date: string | null;
  }>(),
  {
    labId: null,
    date: null,
  },
);

const emit = defineEmits<{
  "update:selectedSlot": [value: { startTime: string; endTime: string } | null];
}>();

const slots = ref<TimeSlot[]>([]);
const loading = ref(false);
const selectedStartTime = ref<string | null>(null);
const hasValidInput = ref(false);

async function fetchSlots() {
  if (!props.labId || !props.date) {
    hasValidInput.value = false;
    slots.value = [];
    return;
  }
  hasValidInput.value = true;
  loading.value = true;
  try {
    const result = await bookingsApi.getAvailableSlots(props.labId, props.date);
    slots.value = result;
  } catch {
    slots.value = [];
  } finally {
    loading.value = false;
  }
}

function handleSlotClick(slot: TimeSlot) {
  if (!slot.available) return;
  selectedStartTime.value = slot.startTime;
  emit("update:selectedSlot", { startTime: slot.startTime, endTime: slot.endTime });
}

watch([() => props.labId, () => props.date], () => {
  selectedStartTime.value = null;
  emit("update:selectedSlot", null);
  fetchSlots();
});
</script>

<template>
  <div class="time-slot-picker" v-loading="loading">
    <el-empty v-if="!hasValidInput" description="请先选择实验室和日期" :image-size="80" />
    <template v-else-if="slots.length > 0">
      <div class="slot-grid">
        <div
          v-for="slot in slots"
          :key="slot.startTime"
          class="slot-block"
          :class="{
            available: slot.available,
            unavailable: !slot.available,
            selected: slot.available && slot.startTime === selectedStartTime,
          }"
          @click="handleSlotClick(slot)"
        >
          {{ slot.startTime }} - {{ slot.endTime }}
        </div>
      </div>
    </template>
    <el-empty v-else description="该日期暂无可用时段" :image-size="80" />
  </div>
</template>

<style scoped>
.time-slot-picker {
  min-height: 80px;
}

.slot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

@media (max-width: 600px) {
  .slot-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.slot-block {
  padding: 10px 8px;
  border-radius: 6px;
  text-align: center;
  font-size: 13px;
  font-weight: 500;
  user-select: none;
  transition: all 0.15s;
}

.slot-block.available {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #c2e7b0;
  cursor: pointer;
}

.slot-block.available:hover {
  background: #e1f3d8;
  border-color: #67c23a;
}

.slot-block.available.selected {
  background: #ecf5ff;
  color: #409eff;
  border-color: #409eff;
}

.slot-block.unavailable {
  background: #f5f7fa;
  color: #c0c4cc;
  border: 1px solid #e4e7ed;
  cursor: not-allowed;
}
</style>
