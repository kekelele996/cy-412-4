<script setup lang="ts">
import type { Announcement } from '../../types/announcement';
import CategoryTag from './CategoryTag.vue';

defineProps<{ announcement: Announcement }>();
defineEmits<{ read: [id: number] }>();
</script>

<template>
  <article class="announcement-card" :class="{ 'is-unread': !announcement.read }" @click="$emit('read', announcement.id)">
    <div class="announcement-card__head">
      <div class="announcement-card__tags">
        <span v-if="!announcement.read" class="unread-dot"></span>
        <CategoryTag :category="announcement.category" />
        <el-tag v-if="announcement.top" type="danger" effect="plain">置顶</el-tag>
        <el-tag v-if="announcement.buildings" type="warning" effect="light">{{ announcement.buildings }} 定向</el-tag>
      </div>
    </div>
    <h3>{{ announcement.title }}</h3>
    <p>{{ announcement.content }}</p>
    <span>{{ announcement.publishAt }} · 已读 {{ announcement.readCount }}</span>
  </article>
</template>

<style scoped>
.announcement-card {
  cursor: pointer;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #dfe7d8;
  background: #fff;
  transition: all 0.2s;
}

.announcement-card:hover {
  border-color: #b8c9a8;
  box-shadow: 0 2px 8px rgba(69, 98, 79, 0.08);
}

.announcement-card.is-unread {
  background: #f7faf4;
  border-color: #c8d9bc;
}

.announcement-card__head {
  display: flex;
  justify-content: space-between;
}

.announcement-card__tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e74c3c;
  flex-shrink: 0;
}

.announcement-card h3 {
  margin: 12px 0 8px;
  font-size: 17px;
}

.announcement-card.is-unread h3 {
  font-weight: 600;
}

.announcement-card p {
  color: #3e4b44;
  line-height: 1.6;
}

.announcement-card span {
  color: #778273;
  font-size: 13px;
}
</style>
