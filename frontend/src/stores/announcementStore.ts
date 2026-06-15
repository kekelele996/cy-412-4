import { defineStore } from 'pinia';
import { ref } from 'vue';
import { getUnreadCount, listAnnouncements, markAnnouncementRead, publishAnnouncement } from '../api/announcement';
import type { Announcement, AnnouncementCategory } from '../types/announcement';

export const useAnnouncementStore = defineStore('announcement', () => {
  const announcements = ref<Announcement[]>([]);
  const unreadCount = ref(0);
  const loading = ref(false);

  async function fetchAnnouncements() {
    loading.value = true;
    try {
      announcements.value = await listAnnouncements();
    } finally {
      loading.value = false;
    }
  }

  async function fetchUnreadCount() {
    try {
      unreadCount.value = await getUnreadCount();
    } catch {
      unreadCount.value = 0;
    }
  }

  async function read(id: number) {
    await markAnnouncementRead(id);
    const announcement = announcements.value.find((item) => item.id === id);
    if (announcement) {
      announcement.read = true;
      announcement.readCount += 1;
    }
    if (unreadCount.value > 0) {
      unreadCount.value -= 1;
    }
  }

  async function publish(data: { title: string; content: string; category: AnnouncementCategory; top: boolean; buildings?: string }) {
    const created = await publishAnnouncement(data);
    announcements.value.unshift(created);
    await fetchUnreadCount();
    return created;
  }

  return { announcements, unreadCount, loading, fetchAnnouncements, fetchUnreadCount, read, publish };
});
