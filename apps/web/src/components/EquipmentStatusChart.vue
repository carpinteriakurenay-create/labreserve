<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import * as echarts from "echarts/core";
import { PieChart } from "echarts/charts";
import { TitleComponent, TooltipComponent, LegendComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";

echarts.use([PieChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer]);
import { EquipmentStatus, EQUIPMENT_STATUS_LABELS } from "@labreserve/shared";
import { getEquipments } from "@/api/equipment";

const chartRef = ref<HTMLDivElement>();
const loading = ref(false);
let chart: echarts.ECharts | null = null;

const STATUS_COLORS: Record<string, string> = {
  [EquipmentStatus.AVAILABLE]: "#67C23A",
  [EquipmentStatus.BORROWED]: "#409EFF",
  [EquipmentStatus.MAINTENANCE]: "#E6A23C",
};

async function fetchAndRender() {
  loading.value = true;
  try {
    const page = await getEquipments({ pageSize: 999 });
    const counts: Record<string, number> = {
      [EquipmentStatus.AVAILABLE]: 0,
      [EquipmentStatus.BORROWED]: 0,
      [EquipmentStatus.MAINTENANCE]: 0,
    };

    for (const eq of page.records) {
      const key = eq.status as keyof typeof counts;
      if (key in counts) {
        counts[key] = (counts[key] ?? 0) + 1;
      }
    }

    const pieData = Object.entries(counts)
      .filter(([, count]) => count > 0)
      .map(([status, count]) => ({
        name: EQUIPMENT_STATUS_LABELS[status as EquipmentStatus] ?? status,
        value: count,
        itemStyle: { color: STATUS_COLORS[status] ?? "#999" },
      }));

    if (!chart) {
      if (!chartRef.value) return;
      chart = echarts.init(chartRef.value);
      window.addEventListener("resize", handleResize);
    }

    chart.setOption({
      tooltip: {
        trigger: "item",
        formatter: "{b}: {c} 台 ({d}%)",
      },
      legend: {
        bottom: 0,
      },
      series: [
        {
          type: "pie",
          radius: ["45%", "75%"],
          center: ["50%", "50%"],
          avoidLabelOverlap: false,
          label: {
            show: true,
            formatter: "{b}\n{c} 台",
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 16,
              fontWeight: "bold",
            },
          },
          data: pieData,
        },
      ],
    });
  } finally {
    loading.value = false;
  }
}

function handleResize() {
  chart?.resize();
}

onMounted(() => {
  fetchAndRender();
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  chart?.dispose();
});
</script>

<template>
  <div v-loading="loading" ref="chartRef" class="equipment-status-chart"></div>
</template>

<style scoped>
.equipment-status-chart {
  width: 100%;
  height: 350px;
}
</style>
