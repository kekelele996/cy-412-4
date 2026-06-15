<script setup lang="ts">
import { onMounted, ref, watch, computed } from 'vue';
import * as echarts from 'echarts';
import StatCard from '../components/common/StatCard.vue';
import RepairStatusBadge from '../components/common/RepairStatusBadge.vue';
import AnnouncementCard from '../components/common/AnnouncementCard.vue';
import EmptyState from '../components/common/EmptyState.vue';
import { useRepairStore } from '../stores/repairStore';
import { usePaymentStore } from '../stores/paymentStore';
import { useAnnouncementStore } from '../stores/announcementStore';
import { useRepairStats } from '../hooks/useRepairStats';

const repairStore = useRepairStore();
const paymentStore = usePaymentStore();
const announcementStore = useAnnouncementStore();
const chartEl = ref<HTMLDivElement | null>(null);
const repairStats = useRepairStats(ref(repairStore.repairs));

const recentAnnouncements = computed(() => announcementStore.announcements.slice(0, 3));

async function refresh() {
  await Promise.all([
    repairStore.fetchRepairs(),
    paymentStore.fetchPayments(),
    announcementStore.fetchAnnouncements(),
  ]);
}

function renderChart() {
  if (!chartEl.value) return;
  const chart = echarts.init(chartEl.value);
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 32, right: 18, bottom: 28, top: 24 },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六'] },
    yAxis: { type: 'value' },
    series: [
      {
        name: '工单',
        type: 'line',
        smooth: true,
        data: [4, 7, 5, 9, 6, repairStore.repairs.length],
        areaStyle: { color: 'rgba(69, 98, 79, 0.12)' },
        itemStyle: { color: '#45624f' },
      },
    ],
  });
}

async function readAnnouncement(id: number) {
  await announcementStore.read(id);
}

onMounted(async () => {
  await refresh();
  renderChart();
});

watch(() => repairStore.repairs.length, renderChart);
</script>

<template>
  <section>
    <div class="stats-row">
      <StatCard label="待分配工单" :value="repairStats.pending" hint="需物业派单" tone="amber" />
      <StatCard label="处理中工单" :value="repairStats.processing" hint="含已分配" tone="blue" />
      <StatCard label="本月待收" :value="`¥${paymentStore.unpaidAmount.toFixed(2)}`" hint="支付宝沙箱模拟" tone="green" />
      <StatCard label="公告阅读" :value="announcementStore.announcements.reduce((sum, item) => sum + item.readCount, 0)" hint="累计阅读数" tone="red" />
    </div>

    <div class="page-grid two-col">
      <section class="section-panel">
        <div class="section-title">
          <h2>工单趋势</h2>
          <el-button size="small" @click="refresh">刷新</el-button>
        </div>
        <div ref="chartEl" class="chart"></div>
        <div class="mini-repairs">
          <div v-for="repair in repairStore.repairs.slice(0, 3)" :key="repair.id">
            <span>{{ repair.title }}</span>
            <RepairStatusBadge :status="repair.status" />
          </div>
        </div>
      </section>

      <section class="section-panel">
        <div class="section-title">
          <h2>最新公告</h2>
        </div>
        <div v-if="recentAnnouncements.length" class="list-stack">
          <AnnouncementCard
            v-for="announcement in recentAnnouncements"
            :key="announcement.id"
            :announcement="announcement"
            @read="readAnnouncement"
          />
        </div>
        <EmptyState v-else title="暂无公告" />
      </section>
    </div>
  </section>
</template>

<style scoped>
.chart {
  width: 100%;
  height: 280px;
}

.mini-repairs {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.mini-repairs div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid #e5ecdf;
}
</style>
