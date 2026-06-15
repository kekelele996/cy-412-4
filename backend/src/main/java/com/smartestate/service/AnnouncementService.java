package com.smartestate.service;

import com.smartestate.dto.AnnouncementRequest;
import com.smartestate.entity.Announcement;

import java.util.List;

public interface AnnouncementService {
    List<Announcement> list(Long userId, String role);
    Announcement publish(Long publisherId, String role, AnnouncementRequest request);
    Announcement read(Long id, Long userId, String role);
    Integer unreadCount(Long userId, String role);
}
