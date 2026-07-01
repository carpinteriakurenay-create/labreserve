<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from "vue";
import * as echarts from "echarts";
import type { LabUsageStat } from "@labreserve/shared";

const props = defineProps<{
  data: LabUsageStat[];
}>();

const chartRef = ref<HTMLDivElement>();
let chart: echarts.ECharts | null = null;

function initChart() {
  if (!chartRef.value) return;
  chart = echarts.init(chartRef.value);

  chart.setOption({
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      formatter: (params: { name: string; value: number; data: LabUsageStat }[]) => {
        const p = params[0];
        if (!p) return "";
        return `<b>${p.name}</b><br/>
          预约次数：${p.value} 次<br/>
          使用时长：${p.data.usageHours.toFixed(1)} 小时`;
      },
    },
    grid: { left: 40, right: 20, top: 20, bottom: 60 },
    xAxis: {
      type: "category",
      data: props.data.map((d) => d.labName),
      axisLabel: { rotate: 30, fontSize: 12 },
    },
    yAxis: {
      type: "value",
      name: "预约次数",
      minInterval: 1,
    },
    series: [
      {
        type: "bar",
        data: props.data.map((d) => ({
          value: d.bookingCount,
          ...d,
        })),
        itemStyle: {
          color: "#409EFF",
          borderRadius: [4, 4, 0, 0],
        },
        barMaxWidth: 50,
      },
    ],
  });

  window.addEventListener("resize", handleResize);
}

function handleResize() {
  chart?.resize();
}

function updateChart() {
  if (!chart) return;
  chart.setOption({
    xAxis: {
      data: props.data.map((d) => d.labName),
    },
    series: [
      {
        data: props.data.map((d) => ({
          value: d.bookingCount,
          ...d,
        })),
      },
    ],
  });
}

watch(() => props.data, updateChart, { deep: true });

onMounted(() => {
  initChart();
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  chart?.dispose();
});
</script>

<template>
  <div ref="chartRef" class="lab-usage-chart"></div>
</template>

<style scoped>
.lab-usage-chart {
  width: 100%;
  height: 350px;
}
</style>
