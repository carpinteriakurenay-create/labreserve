package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.ReviewCreateRequest;
import com.labreserve.dto.ReviewVO;
import com.labreserve.entity.Booking;
import com.labreserve.entity.Lab;
import com.labreserve.entity.Review;
import com.labreserve.entity.User;
import com.labreserve.enums.BookingStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.BookingMapper;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.ReviewMapper;
import com.labreserve.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final BookingMapper bookingMapper;
    private final UserMapper userMapper;
    private final LabMapper labMapper;

    public ReviewService(ReviewMapper reviewMapper, BookingMapper bookingMapper,
                         UserMapper userMapper, LabMapper labMapper) {
        this.reviewMapper = reviewMapper;
        this.bookingMapper = bookingMapper;
        this.userMapper = userMapper;
        this.labMapper = labMapper;
    }

    @Transactional
    public ReviewVO createReview(ReviewCreateRequest request, Long userId) {
        Booking booking = bookingMapper.selectById(request.getBookingId());
        if (booking == null) {
            throw new BusinessException("NOT_FOUND", "预约不存在");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException("BOOKING_NOT_COMPLETED", "只能评价已完成的预约");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "只能评价自己的预约");
        }

        Review existing = reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getBookingId, request.getBookingId()));
        if (existing != null) {
            throw new BusinessException("ALREADY_REVIEWED", "该预约已评价");
        }

        Review review = new Review();
        review.setBookingId(request.getBookingId());
        review.setUserId(userId);
        review.setLabId(booking.getLabId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        reviewMapper.insert(review);
        return toReviewVO(review);
    }

    public ReviewVO getReviewById(Long id) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("NOT_FOUND", "评价不存在");
        }
        ReviewVO vo = toReviewVO(review);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    public ReviewVO getReviewByBookingId(Long bookingId, Long userId) {
        Review review = reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getBookingId, bookingId)
                        .eq(Review::getUserId, userId));
        if (review == null) {
            return null;
        }
        ReviewVO vo = toReviewVO(review);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    public IPage<ReviewVO> listReviews(Long labId, Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (labId != null) {
            wrapper.eq(Review::getLabId, labId);
        }
        if (userId != null) {
            wrapper.eq(Review::getUserId, userId);
        }
        wrapper.orderByDesc(Review::getCreatedAt);

        Page<Review> page = new Page<>(pageNum, pageSize);
        IPage<Review> result = reviewMapper.selectPage(page, wrapper);
        IPage<ReviewVO> voPage = result.convert(this::toReviewVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    @Transactional
    public void deleteReview(Long id, Long userId) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("NOT_FOUND", "评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "只能删除自己的评价");
        }
        reviewMapper.deleteById(id);
    }

    private ReviewVO toReviewVO(Review review) {
        return ReviewVO.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .userId(review.getUserId())
                .labId(review.getLabId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private void populateJoinedFields(List<ReviewVO> vos) {
        if (vos.isEmpty()) {
            return;
        }

        Set<Long> labIds = vos.stream()
                .map(ReviewVO::getLabId)
                .collect(Collectors.toSet());
        Set<Long> userIds = vos.stream()
                .map(ReviewVO::getUserId)
                .collect(Collectors.toSet());

        Map<Long, String> labIdToName = Collections.emptyMap();
        if (!labIds.isEmpty()) {
            List<Lab> labs = labMapper.selectBatchIds(labIds);
            labIdToName = labs.stream()
                    .collect(Collectors.toMap(Lab::getId, Lab::getName));
        }

        Map<Long, String> userIdToName = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userIdToName = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getRealName));
        }

        for (ReviewVO vo : vos) {
            vo.setLabName(labIdToName.get(vo.getLabId()));
            vo.setUserName(userIdToName.get(vo.getUserId()));
        }
    }
}
