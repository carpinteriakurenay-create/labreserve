package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.NoticeCreateRequest;
import com.labreserve.dto.NoticeUpdateRequest;
import com.labreserve.dto.NoticeVO;
import com.labreserve.entity.Lab;
import com.labreserve.entity.Notice;
import com.labreserve.entity.User;
import com.labreserve.enums.NoticePriority;
import com.labreserve.enums.NoticeType;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.NoticeMapper;
import com.labreserve.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final UserMapper userMapper;
    private final LabMapper labMapper;

    public NoticeService(NoticeMapper noticeMapper, UserMapper userMapper, LabMapper labMapper) {
        this.noticeMapper = noticeMapper;
        this.userMapper = userMapper;
        this.labMapper = labMapper;
    }

    public IPage<NoticeVO> listNotices(String typeStr, String priorityStr, int pageNum, int pageSize) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();

        if (typeStr != null && !typeStr.isBlank()) {
            try {
                wrapper.eq(Notice::getType, NoticeType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_STATUS", "无效的通知类型");
            }
        }
        if (priorityStr != null && !priorityStr.isBlank()) {
            try {
                wrapper.eq(Notice::getPriority, NoticePriority.valueOf(priorityStr));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_STATUS", "无效的优先级");
            }
        }

        wrapper.orderByDesc(Notice::getCreatedAt);

        Page<Notice> page = new Page<>(pageNum, pageSize);
        IPage<Notice> result = noticeMapper.selectPage(page, wrapper);
        IPage<NoticeVO> voPage = result.convert(this::toNoticeVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    public NoticeVO getNoticeById(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("NOT_FOUND", "通知不存在");
        }
        NoticeVO vo = toNoticeVO(notice);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    @Transactional
    public NoticeVO createNotice(NoticeCreateRequest request, Long publisherId) {
        Notice notice = new Notice();
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setType(request.getType() != null && !request.getType().isBlank()
                ? NoticeType.valueOf(request.getType()) : NoticeType.GENERAL);
        notice.setPriority(request.getPriority() != null && !request.getPriority().isBlank()
                ? NoticePriority.valueOf(request.getPriority()) : NoticePriority.NORMAL);
        notice.setPublisherId(publisherId);
        notice.setLabId(request.getLabId());

        noticeMapper.insert(notice);
        return toNoticeVO(notice);
    }

    @Transactional
    public NoticeVO updateNotice(Long id, NoticeUpdateRequest request) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("NOT_FOUND", "通知不存在");
        }

        LambdaUpdateWrapper<Notice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notice::getId, id);

        if (request.getTitle() != null) {
            wrapper.set(Notice::getTitle, request.getTitle());
        }
        if (request.getContent() != null) {
            wrapper.set(Notice::getContent, request.getContent());
        }
        if (request.getType() != null) {
            wrapper.set(Notice::getType, NoticeType.valueOf(request.getType()));
        }
        if (request.getPriority() != null) {
            wrapper.set(Notice::getPriority, NoticePriority.valueOf(request.getPriority()));
        }
        if (request.getLabId() != null) {
            wrapper.set(Notice::getLabId, request.getLabId());
        }

        noticeMapper.update(wrapper);

        Notice updated = noticeMapper.selectById(id);
        NoticeVO vo = toNoticeVO(updated);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    @Transactional
    public void deleteNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("NOT_FOUND", "通知不存在");
        }
        noticeMapper.deleteById(id);
    }

    private NoticeVO toNoticeVO(Notice notice) {
        return NoticeVO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .type(notice.getType().name())
                .priority(notice.getPriority().name())
                .publisherId(notice.getPublisherId())
                .labId(notice.getLabId())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }

    private void populateJoinedFields(List<NoticeVO> vos) {
        if (vos.isEmpty()) {
            return;
        }

        Set<Long> publisherIds = vos.stream()
                .map(NoticeVO::getPublisherId)
                .collect(Collectors.toSet());

        Set<Long> labIds = vos.stream()
                .map(NoticeVO::getLabId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> userIdToName = Collections.emptyMap();
        if (!publisherIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(publisherIds);
            userIdToName = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getRealName));
        }

        Map<Long, String> labIdToName = Collections.emptyMap();
        if (!labIds.isEmpty()) {
            List<Lab> labs = labMapper.selectBatchIds(labIds);
            labIdToName = labs.stream()
                    .collect(Collectors.toMap(Lab::getId, Lab::getName));
        }

        for (NoticeVO vo : vos) {
            vo.setPublisherName(userIdToName.get(vo.getPublisherId()));
            if (vo.getLabId() != null) {
                vo.setLabName(labIdToName.get(vo.getLabId()));
            }
        }
    }
}
