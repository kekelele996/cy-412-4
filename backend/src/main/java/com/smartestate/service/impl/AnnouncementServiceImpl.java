package com.smartestate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartestate.common.ErrorCode;
import com.smartestate.constants.LogTemplates;
import com.smartestate.constants.UserConstants;
import com.smartestate.dto.AnnouncementRequest;
import com.smartestate.entity.Announcement;
import com.smartestate.entity.AnnouncementRead;
import com.smartestate.entity.User;
import com.smartestate.mapper.AnnouncementMapper;
import com.smartestate.mapper.AnnouncementReadMapper;
import com.smartestate.mapper.UserMapper;
import com.smartestate.service.AnnouncementService;
import com.smartestate.service.OperationLogService;
import com.smartestate.utils.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {
    private final AnnouncementMapper announcementMapper;
    private final AnnouncementReadMapper announcementReadMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;

    public AnnouncementServiceImpl(AnnouncementMapper announcementMapper, AnnouncementReadMapper announcementReadMapper,
                                   UserMapper userMapper, OperationLogService operationLogService) {
        this.announcementMapper = announcementMapper;
        this.announcementReadMapper = announcementReadMapper;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<Announcement> list(Long userId, String role) {
        LogUtil.info(LogTemplates.ANNOUNCEMENT_LIST, role);
        LambdaQueryWrapper<Announcement> queryWrapper = new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getTop)
                .orderByDesc(Announcement::getPublishAt);

        if (UserConstants.RESIDENT.equals(role)) {
            User user = userMapper.selectById(userId);
            if (user != null && StringUtils.hasText(user.getBuilding())) {
                String building = user.getBuilding();
                queryWrapper.and(wrapper -> wrapper
                        .isNull(Announcement::getBuildings)
                        .or()
                        .apply("FIND_IN_SET({0}, buildings) > 0", building));
            } else {
                queryWrapper.isNull(Announcement::getBuildings);
            }
        }

        List<Announcement> announcements = announcementMapper.selectList(queryWrapper);

        List<AnnouncementRead> readList = announcementReadMapper.selectList(new LambdaQueryWrapper<AnnouncementRead>()
                .eq(AnnouncementRead::getUserId, userId));
        Set<Long> readIds = readList.stream()
                .map(AnnouncementRead::getAnnouncementId)
                .collect(Collectors.toSet());

        for (Announcement announcement : announcements) {
            announcement.setRead(readIds.contains(announcement.getId()));
        }

        return announcements;
    }

    @Override
    public Announcement publish(Long publisherId, String role, AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setCategory(request.getCategory() == null ? "notice" : request.getCategory());
        announcement.setPublisherId(publisherId);
        announcement.setPublishAt(LocalDateTime.now());
        announcement.setTop(Boolean.TRUE.equals(request.getTop()));
        announcement.setReadCount(0);
        announcement.setBuildings(StringUtils.hasText(request.getBuildings()) ? request.getBuildings() : null);
        announcementMapper.insert(announcement);

        AnnouncementRead read = new AnnouncementRead();
        read.setAnnouncementId(announcement.getId());
        read.setUserId(publisherId);
        read.setReadAt(LocalDateTime.now());
        announcementReadMapper.insert(read);
        announcement.setReadCount(1);
        announcement.setRead(true);

        operationLogService.record(publisherId, role, "announcement.publish", "Announcement", announcement.getId(),
                String.format(LogTemplates.ANNOUNCEMENT_CREATE, announcement.getId(), announcement.getCategory(), role));
        return announcement;
    }

    @Override
    public Announcement read(Long id, Long userId, String role) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new IllegalArgumentException(ErrorCode.ANNOUNCEMENT_NOT_FOUND.format(id, role));
        }
        AnnouncementRead existing = announcementReadMapper.selectOne(new LambdaQueryWrapper<AnnouncementRead>()
                .eq(AnnouncementRead::getAnnouncementId, id)
                .eq(AnnouncementRead::getUserId, userId));
        if (existing == null) {
            AnnouncementRead read = new AnnouncementRead();
            read.setAnnouncementId(id);
            read.setUserId(userId);
            read.setReadAt(LocalDateTime.now());
            announcementReadMapper.insert(read);
            announcementMapper.update(null, new LambdaUpdateWrapper<Announcement>()
                    .eq(Announcement::getId, id)
                    .setSql("read_count = read_count + 1"));
            announcement.setReadCount(announcement.getReadCount() + 1);
        }
        announcement.setRead(true);
        operationLogService.record(userId, role, "announcement.read", "Announcement", id,
                String.format(LogTemplates.ANNOUNCEMENT_READ, id, userId, role));
        return announcement;
    }

    @Override
    public Integer unreadCount(Long userId, String role) {
        LambdaQueryWrapper<Announcement> announcementQuery = new LambdaQueryWrapper<>();

        if (UserConstants.RESIDENT.equals(role)) {
            User user = userMapper.selectById(userId);
            if (user != null && StringUtils.hasText(user.getBuilding())) {
                String building = user.getBuilding();
                announcementQuery.and(wrapper -> wrapper
                        .isNull(Announcement::getBuildings)
                        .or()
                        .apply("FIND_IN_SET({0}, buildings) > 0", building));
            } else {
                announcementQuery.isNull(Announcement::getBuildings);
            }
        }

        List<Announcement> announcements = announcementMapper.selectList(announcementQuery);
        if (announcements.isEmpty()) {
            return 0;
        }

        List<Long> announcementIds = announcements.stream()
                .map(Announcement::getId)
                .collect(Collectors.toList());

        Long readCount = announcementReadMapper.selectCount(new LambdaQueryWrapper<AnnouncementRead>()
                .eq(AnnouncementRead::getUserId, userId)
                .in(AnnouncementRead::getAnnouncementId, announcementIds));

        return announcementIds.size() - readCount.intValue();
    }
}
