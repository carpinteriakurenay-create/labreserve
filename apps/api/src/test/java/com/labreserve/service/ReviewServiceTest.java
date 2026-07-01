package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labreserve.dto.ReviewCreateRequest;
import com.labreserve.dto.ReviewVO;
import com.labreserve.entity.Booking;
import com.labreserve.entity.Review;
import com.labreserve.enums.BookingStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.BookingMapper;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.ReviewMapper;
import com.labreserve.mapper.UserMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewMapper reviewMapper;
    @Mock private BookingMapper bookingMapper;
    @Mock private UserMapper userMapper;
    @Mock private LabMapper labMapper;

    @InjectMocks
    private ReviewService reviewService;

    @Nested
    class CreateReview {

        @Test
        void shouldCreateReviewForCompletedBooking() {
            Booking booking = new Booking();
            booking.setId(42L);
            booking.setLabId(1L);
            booking.setUserId(1L);
            booking.setStatus(BookingStatus.COMPLETED);

            when(bookingMapper.selectById(42L)).thenReturn(booking);
            when(reviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setBookingId(42L);
            request.setRating(4);
            request.setComment("Great lab!");

            ReviewVO result = reviewService.createReview(request, 1L);

            assertNotNull(result);
            assertEquals(4, result.getRating());
            assertEquals("Great lab!", result.getComment());
            assertEquals(1L, result.getLabId());
        }

        @Test
        void shouldThrowWhenBookingNotFound() {
            when(bookingMapper.selectById(999L)).thenReturn(null);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setBookingId(999L);
            request.setRating(4);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(request, 1L));
            assertEquals("NOT_FOUND", ex.getCode());
        }

        @Test
        void shouldThrowWhenBookingNotCompleted() {
            Booking booking = new Booking();
            booking.setId(42L);
            booking.setStatus(BookingStatus.APPROVED);

            when(bookingMapper.selectById(42L)).thenReturn(booking);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setBookingId(42L);
            request.setRating(4);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(request, 1L));
            assertEquals("BOOKING_NOT_COMPLETED", ex.getCode());
        }

        @Test
        void shouldThrowWhenNotOwnBooking() {
            Booking booking = new Booking();
            booking.setId(42L);
            booking.setUserId(2L);
            booking.setStatus(BookingStatus.COMPLETED);

            when(bookingMapper.selectById(42L)).thenReturn(booking);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setBookingId(42L);
            request.setRating(4);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(request, 1L));
            assertEquals("FORBIDDEN", ex.getCode());
        }

        @Test
        void shouldThrowWhenAlreadyReviewed() {
            Booking booking = new Booking();
            booking.setId(42L);
            booking.setUserId(1L);
            booking.setStatus(BookingStatus.COMPLETED);

            Review existingReview = new Review();
            existingReview.setId(1L);
            existingReview.setBookingId(42L);

            when(bookingMapper.selectById(42L)).thenReturn(booking);
            when(reviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingReview);

            ReviewCreateRequest request = new ReviewCreateRequest();
            request.setBookingId(42L);
            request.setRating(4);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(request, 1L));
            assertEquals("ALREADY_REVIEWED", ex.getCode());
        }
    }

    @Nested
    class DeleteReview {

        @Test
        void shouldDeleteOwnReview() {
            Review review = new Review();
            review.setId(1L);
            review.setUserId(1L);

            when(reviewMapper.selectById(1L)).thenReturn(review);

            reviewService.deleteReview(1L, 1L);

            verify(reviewMapper).deleteById(1L);
        }

        @Test
        void shouldThrowWhenDeletingOthersReview() {
            Review review = new Review();
            review.setId(1L);
            review.setUserId(2L);

            when(reviewMapper.selectById(1L)).thenReturn(review);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.deleteReview(1L, 1L));
            assertEquals("FORBIDDEN", ex.getCode());
        }

        @Test
        void shouldThrowWhenReviewNotFound() {
            when(reviewMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.deleteReview(999L, 1L));
            assertEquals("NOT_FOUND", ex.getCode());
        }
    }
}
