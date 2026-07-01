package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labreserve.dto.BorrowCreateRequest;
import com.labreserve.dto.BorrowVO;
import com.labreserve.entity.Borrow;
import com.labreserve.entity.Equipment;
import com.labreserve.enums.BorrowStatus;
import com.labreserve.enums.EquipmentStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.BorrowMapper;
import com.labreserve.mapper.EquipmentMapper;
import com.labreserve.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock private BorrowMapper borrowMapper;
    @Mock private EquipmentMapper equipmentMapper;
    @Mock private UserMapper userMapper;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;

    @InjectMocks
    private BorrowService borrowService;

    @BeforeEach
    void setUp() throws InterruptedException {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(rLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    @Nested
    class CreateBorrow {

        @Test
        void shouldCreateBorrowSuccessfully() {
            Equipment equipment = new Equipment();
            equipment.setId(1L);
            equipment.setName("Test Equipment");
            equipment.setStatus(EquipmentStatus.AVAILABLE);

            when(equipmentMapper.selectById(1L)).thenReturn(equipment);

            BorrowCreateRequest request = new BorrowCreateRequest();
            request.setEquipmentId(1L);
            request.setBorrowDate(LocalDate.now().toString());
            request.setExpectedReturn(LocalDate.now().plusDays(5).toString());
            request.setPurpose("Testing");

            BorrowVO result = borrowService.createBorrow(request, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getEquipmentId());
            assertEquals(1L, result.getUserId());
            assertEquals("PENDING", result.getStatus());
        }

        @Test
        void shouldThrowWhenEquipmentNotFound() {
            when(equipmentMapper.selectById(999L)).thenReturn(null);

            BorrowCreateRequest request = new BorrowCreateRequest();
            request.setEquipmentId(999L);
            request.setBorrowDate(LocalDate.now().toString());
            request.setExpectedReturn(LocalDate.now().plusDays(5).toString());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> borrowService.createBorrow(request, 1L));
            assertEquals("NOT_FOUND", ex.getCode());
        }

        @Test
        void shouldThrowWhenEquipmentUnavailable() {
            Equipment equipment = new Equipment();
            equipment.setId(1L);
            equipment.setStatus(EquipmentStatus.BORROWED);

            when(equipmentMapper.selectById(1L)).thenReturn(equipment);

            BorrowCreateRequest request = new BorrowCreateRequest();
            request.setEquipmentId(1L);
            request.setBorrowDate(LocalDate.now().toString());
            request.setExpectedReturn(LocalDate.now().plusDays(5).toString());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> borrowService.createBorrow(request, 1L));
            assertEquals("EQUIPMENT_UNAVAILABLE", ex.getCode());
        }
    }

    @Nested
    class ApproveBorrow {

        @Test
        void shouldApproveBorrowAndSetEquipmentStatus() {
            Borrow borrow = new Borrow();
            borrow.setId(1L);
            borrow.setEquipmentId(1L);
            borrow.setStatus(BorrowStatus.PENDING);

            when(borrowMapper.selectById(1L)).thenReturn(borrow);

            BorrowVO result = borrowService.approveBorrow(1L, true, null, 2L);

            assertNotNull(result);
            verify(equipmentMapper).update(any());
        }

        @Test
        void shouldRejectBorrowWithReason() {
            Borrow borrow = new Borrow();
            borrow.setId(1L);
            borrow.setStatus(BorrowStatus.PENDING);

            when(borrowMapper.selectById(1L)).thenReturn(borrow);

            BorrowVO result = borrowService.approveBorrow(1L, false, "Equipment not available", 2L);

            assertNotNull(result);
        }

        @Test
        void shouldThrowWhenRejectWithoutReason() {
            Borrow borrow = new Borrow();
            borrow.setId(1L);
            borrow.setStatus(BorrowStatus.PENDING);

            when(borrowMapper.selectById(1L)).thenReturn(borrow);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> borrowService.approveBorrow(1L, false, "", 2L));
            assertEquals("REJECT_REASON_REQUIRED", ex.getCode());
        }

        @Test
        void shouldThrowWhenAlreadyProcessed() {
            Borrow borrow = new Borrow();
            borrow.setId(1L);
            borrow.setStatus(BorrowStatus.BORROWING);

            when(borrowMapper.selectById(1L)).thenReturn(borrow);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> borrowService.approveBorrow(1L, true, null, 2L));
            assertEquals("ALREADY_PROCESSED", ex.getCode());
        }
    }

    @Nested
    class ReturnBorrow {

        @Test
        void shouldReturnBorrowAndRestoreEquipment() {
            Borrow borrow = new Borrow();
            borrow.setId(1L);
            borrow.setEquipmentId(1L);
            borrow.setStatus(BorrowStatus.BORROWING);

            when(borrowMapper.selectById(1L)).thenReturn(borrow);

            borrowService.returnBorrow(1L, LocalDate.now().toString());

            verify(borrowMapper).update(any());
            verify(equipmentMapper).update(any());
        }

        @Test
        void shouldThrowWhenNotBorrowing() {
            Borrow borrow = new Borrow();
            borrow.setId(1L);
            borrow.setStatus(BorrowStatus.PENDING);

            when(borrowMapper.selectById(1L)).thenReturn(borrow);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> borrowService.returnBorrow(1L, LocalDate.now().toString()));
            assertEquals("INVALID_STATUS", ex.getCode());
        }
    }
}
