<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AnnouncementCard from '../components/common/AnnouncementCard.vue';
import EmptyState from '../components/common/EmptyState.vue';
import PermissionButton from '../components/common/PermissionButton.vue';
import { listBuildings } from '../api/user';
import { useAnnouncementStore } from '../stores/announcementStore';
import type { AnnouncementCategory } from '../types/announcement';

const announcementStore = useAnnouncementStore();
const buildings = ref<string[]>([]);
const form = reactive({
  title: '',
  content: '',
  category: 'notice' as AnnouncementCategory,
  top: false,
  targetBuildings: [] as string[],
});

async function fetchBuildings() {
  buildings.value = await listBuildings();
}

async function read(id: number) {
  await announcementStore.read(id);
}

async function publish() {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写公告标题和内容');
    return;
  }
  const buildingsStr = form.targetBuildings.length > 0 ? form.targetBuildings.join(',') : '';
  await announcementStore.publish({
    title: form.title,
    content: form.content,
    category: form.category,
    top: form.top,
    buildings: buildingsStr || undefined,
  });
  Object.assign(form, { title: '', content: '', category: 'notice' as AnnouncementCategory, top: false, targetBuildings: [] });
  ElMessage.success('公告已发布');
}

onMounted(() => {
  announcementStore.fetchAnnouncements();
  fetchBuildings();
});
</script>

<template>
  <div class="page-grid two-col">
    <section class="section-panel">
      <div class="section-title">
        <h2>公告列表</h2>
        <span class="announcement-count">共 {{ announcementStore.announcements.length }} 条</span>
      </div>
      <div v-if="announcementStore.announcements.length" class="list-stack">
        <AnnouncementCard v-for="announcement in announcementStore.announcements" :key="announcement.id" :announcement="announcement" @read="read" />
      </div>
      <EmptyState v-else title="暂无公告" />
    </section>

    <section class="section-panel">
      <div class="section-title">
        <h2>物业发布</h2>
      </div>
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="80" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option label="通知" value="notice" />
            <el-option label="活动" value="event" />
            <el-option label="紧急" value="urgent" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收范围">
          <el-select
            v-model="form.targetBuildings"
            multiple
            filterable
            placeholder="不选则为全体公告"
            style="width: 100%"
          >
            <el-option
              v-for="building in buildings"
              :key="building"
              :label="building"
              :value="building"
            />
          </el-select>
          <div class="form-hint">
            {{ form.targetBuildings.length > 0 ? `定向发送给 ${form.targetBuildings.length} 个楼栋` : '全体业主可见' }}
          </div>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.top">置顶公告</el-checkbox>
        </el-form-item>
        <PermissionButton permission="announcement:publish" @click="publish">发布公告</PermissionButton>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.announcement-count {
  color: #778273;
  font-size: 13px;
}

.form-hint {
  margin-top: 6px;
  color: #778273;
  font-size: 12px;
}
</style>
