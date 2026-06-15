import { request } from '../utils/request';
import type { Announcement } from '../types/announcement';

export const listAnnouncements = () => request.get<never, Announcement[]>('/announcements');
export const getUnreadCount = () => request.get<never, number>('/announcements/unread-count');
export const publishAnnouncement = (data: Pick<Announcement, 'title' | 'content' | 'category' | 'top'> & { buildings?: string }) =>
  request.post<never, Announcement>('/announcements', data);
export const markAnnouncementRead = (id: number) => request.post<never, Announcement>(`/announcements/${id}/read`, {});
