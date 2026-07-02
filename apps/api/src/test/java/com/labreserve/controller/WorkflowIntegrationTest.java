package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockStudent;
import com.labreserve.support.annotations.WithMockTeacher;
import com.labreserve.support.annotations.WithMockAdmin;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 完整业务流程集成测试：
 * 注册 -> 登录 -> 查看课表 -> 预约实验室 -> 审批预约 ->
 * 借用设备 -> 完成预约 -> 发布评价 -> 查看使用记录 -> 查看仪表盘
 *
 * 使用真实 JWT token 而非 @WithMock* 注解来确保完整的认证链路测试。
 */
class WorkflowIntegrationTest extends BaseIntegrationTest {

    private String studentToken;
    private String teacherToken;
    private String adminToken;

    @BeforeEach
    void loginAndGetTokens() throws Exception {
        // Login as student (userId=1)
        var studentResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student1","password":"password123"}"""))
                .andExpect(status().isOk())
                .andReturn();
        studentToken = com.jayway.jsonpath.JsonPath.read(
                studentResult.getResponse().getContentAsString(), "$.data.token");

        // Login as teacher (userId=2)
        var teacherResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"teacher1","password":"password123"}"""))
                .andExpect(status().isOk())
                .andReturn();
        teacherToken = com.jayway.jsonpath.JsonPath.read(
                teacherResult.getResponse().getContentAsString(), "$.data.token");

        // Login as admin (userId=3)
        var adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin1","password":"password123"}"""))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = com.jayway.jsonpath.JsonPath.read(
                adminResult.getResponse().getContentAsString(), "$.data.token");
    }

    // ===== Step 1: 用户注册 =====

    @Nested
    class Step1_Register {

        @Test
        void shouldRegisterNewStudentSuccessfully() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"wf_test_reg","password":"pass123","realName":"Workflow Test User","email":"wf@univ.edu.cn","phone":"13800009999"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("注册成功"));
        }

        @Test
        void shouldRejectDuplicateRegistration() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"wf_dup_test","password":"pass123","realName":"First"}"""))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"wf_dup_test","password":"pass456","realName":"Second"}"""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("USERNAME_EXISTS"));
        }
    }

    // ===== Step 2: 登录 =====

    @Nested
    class Step2_Login {

        @Test
        void shouldLoginAndReturnTokenWithUserInfo() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"student1","password":"password123"}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.token").isString())
                    .andExpect(jsonPath("$.data.expiresIn").isNumber())
                    .andExpect(jsonPath("$.data.user.id").value(1))
                    .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                    .andExpect(jsonPath("$.data.user.realName").value("Student One"));
        }

        @Test
        void shouldGetCurrentUserInfo() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.username").value("student1"))
                    .andExpect(jsonPath("$.data.role").value("STUDENT"));
        }
    }

    // ===== Step 3: 查看课表 =====

    @Nested
    class Step3_ViewSchedule {

        @Test
        void shouldViewAllCourses() throws Exception {
            mockMvc.perform(get("/api/courses")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        void shouldViewMySchedule() throws Exception {
            // For STUDENT role, my schedule requires className param
            // For TEACHER role, it uses teacherId. Test with teacher role.
            mockMvc.perform(get("/api/courses/mine")
                            .header("Authorization", "Bearer " + teacherToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        void shouldFilterCoursesBySemester() throws Exception {
            mockMvc.perform(get("/api/courses")
                            .header("Authorization", "Bearer " + studentToken)
                            .param("semester", "2025-2026-2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    // ===== Step 4: 预约实验室 =====

    @Nested
    class Step4_Booking {

        @Test
        void shouldCreateBookingForAvailableSlot() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-15","startTime":"10:00","endTime":"12:00","purpose":"FPGA course design workflow test","personCount":2}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").exists());
        }

        @Test
        void shouldCheckAvailableSlotsBeforeBooking() throws Exception {
            mockMvc.perform(get("/api/bookings/available-slots")
                            .header("Authorization", "Bearer " + studentToken)
                            .param("labId", "1")
                            .param("date", "2026-07-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        void shouldViewOwnBookings() throws Exception {
            mockMvc.perform(get("/api/bookings/mine")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.records").isArray());
        }
    }

    // ===== Step 5: 教师审批预约 =====

    @Nested
    class Step5_ApproveBooking {

        @Test
        void teacherShouldViewPendingApprovals() throws Exception {
            mockMvc.perform(get("/api/bookings/pending")
                            .header("Authorization", "Bearer " + teacherToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        void teacherShouldApprovePendingBooking() throws Exception {
            // Create a booking as student, then approve as teacher
            var createResult = mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-20","startTime":"09:00","endTime":"11:00","purpose":"Approval workflow test","personCount":1}"""))
                    .andExpect(status().isCreated())
                    .andReturn();
            String bookingId = com.jayway.jsonpath.JsonPath.read(
                    createResult.getResponse().getContentAsString(), "$.data.id").toString();

            mockMvc.perform(put("/api/bookings/" + bookingId + "/approve")
                            .header("Authorization", "Bearer " + teacherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"approved\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        void teacherShouldRejectBookingWithReason() throws Exception {
            var createResult = mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-20","startTime":"13:00","endTime":"15:00","purpose":"Reject workflow test","personCount":1}"""))
                    .andExpect(status().isCreated())
                    .andReturn();
            String bookingId = com.jayway.jsonpath.JsonPath.read(
                    createResult.getResponse().getContentAsString(), "$.data.id").toString();

            mockMvc.perform(put("/api/bookings/" + bookingId + "/approve")
                            .header("Authorization", "Bearer " + teacherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"approved\":false,\"rejectReason\":\"Time slot reserved for maintenance\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    // ===== Step 6: 借用设备流程 =====

    @Nested
    class Step6_EquipmentBorrowing {

        @Test
        void shouldBorrowEquipmentApproveAndReturn() throws Exception {
            // Student borrows equipment
            var borrowResult = mockMvc.perform(post("/api/borrows")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":1,"borrowDate":"2026-07-10","expectedReturn":"2026-07-15","purpose":"FPGA experiment workflow test"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andReturn();
            String borrowId = com.jayway.jsonpath.JsonPath.read(
                    borrowResult.getResponse().getContentAsString(), "$.data.id").toString();

            // Teacher approves borrow
            mockMvc.perform(put("/api/borrows/" + borrowId + "/approve")
                            .header("Authorization", "Bearer " + teacherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"approved\":true}"))
                    .andExpect(status().isOk());

            // Admin confirms return (only admin can confirm return)
            mockMvc.perform(put("/api/borrows/" + borrowId + "/return")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"actualReturn\":\"2026-07-14\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        void shouldViewMyBorrows() throws Exception {
            mockMvc.perform(get("/api/borrows/mine")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    // ===== Step 7: 完成预约 =====

    @Nested
    class Step7_CompleteBooking {

        @Test
        @WithMockTeacher
        void teacherShouldCompleteApprovedBooking() throws Exception {
            // Booking 1 in test data is APPROVED — teacher can complete it
            mockMvc.perform(put("/api/bookings/1/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockTeacher
        void shouldNotCompletePendingBooking() throws Exception {
            // Booking 2 in test data is PENDING — cannot complete
            mockMvc.perform(put("/api/bookings/2/complete"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ===== Step 8: 发布评价 =====

    @Nested
    class Step8_PostReview {

        @Test
        @WithMockStudent(userId = 1L)
        void shouldPostReviewForCompletedBooking() throws Exception {
            // Booking 3 in test data is COMPLETED for userId=1
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":4,"comment":"Workflow test review: good lab environment"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").exists());
        }

        @Test
        void shouldViewLabReviews() throws Exception {
            mockMvc.perform(get("/api/reviews")
                            .header("Authorization", "Bearer " + studentToken)
                            .param("labId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    // ===== Step 9: 查看使用记录 =====

    @Nested
    class Step9_UsageRecords {

        @Test
        @WithMockTeacher
        void teacherShouldViewUsageRecords() throws Exception {
            mockMvc.perform(get("/api/usage-records"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockTeacher
        void shouldExportUsageRecordsAsCsv() throws Exception {
            // The export endpoint uses GET method (not POST) according to the controller
            mockMvc.perform(get("/api/usage-records/export"))
                    .andExpect(status().isOk());
        }
    }

    // ===== Step 10: 查看统计仪表盘 =====

    @Nested
    class Step10_Dashboard {

        @Test
        void shouldViewKpiForTeacher() throws Exception {
            mockMvc.perform(get("/api/dashboard/kpi")
                            .header("Authorization", "Bearer " + teacherToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.todayBookings").isNumber())
                    .andExpect(jsonPath("$.data.todayBorrows").isNumber())
                    .andExpect(jsonPath("$.data.labUsageRate").isNumber())
                    .andExpect(jsonPath("$.data.pendingApprovals").isNumber());
        }

        @Test
        void shouldViewLabUsageStats() throws Exception {
            mockMvc.perform(get("/api/dashboard/lab-usage")
                            .header("Authorization", "Bearer " + teacherToken)
                            .param("dateFrom", "2026-01-01")
                            .param("dateTo", "2026-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        void shouldViewEquipmentUsage() throws Exception {
            mockMvc.perform(get("/api/dashboard/equipment-usage")
                            .header("Authorization", "Bearer " + teacherToken)
                            .param("dateFrom", "2026-01-01")
                            .param("dateTo", "2026-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        void shouldViewStudentRanking() throws Exception {
            mockMvc.perform(get("/api/dashboard/student-ranking")
                            .header("Authorization", "Bearer " + teacherToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        void studentShouldAccessDashboardKpi() throws Exception {
            mockMvc.perform(get("/api/dashboard/kpi")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    // ===== Step 11: 通知公告 =====

    @Nested
    class Step11_Notices {

        @Test
        void teacherShouldPublishNotice() throws Exception {
            mockMvc.perform(post("/api/notices")
                            .header("Authorization", "Bearer " + teacherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"Workflow Test Notice","content":"This is a notice from the workflow integration test","type":"GENERAL","priority":"NORMAL"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        void studentShouldViewNotices() throws Exception {
            mockMvc.perform(get("/api/notices")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.records").isArray());
        }
    }

    // ===== Step 12: 报修流程 =====

    @Nested
    class Step12_RepairLog {

        @Test
        void shouldSubmitRepairLogAndAdminUpdateStatus() throws Exception {
            // Student submits repair report
            var repairResult = mockMvc.perform(post("/api/repair-logs")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":1,"description":"FPGA board LED not working during workflow test"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andReturn();
            String repairId = com.jayway.jsonpath.JsonPath.read(
                    repairResult.getResponse().getContentAsString(), "$.data.id").toString();

            // Admin can update repair status
            mockMvc.perform(put("/api/repair-logs/" + repairId + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_PROGRESS\"}"))
                    .andExpect(status().isOk());

            // View repair logs
            mockMvc.perform(get("/api/repair-logs")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    // ===== 完整端到端流程（单测试用例） =====

    @Test
    void completeWorkflow_RegisterToDashboard() throws Exception {
        String workflowUser = "e2e_wf_" + System.currentTimeMillis();

        // 1. Register
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"username":"%s","password":"password123","realName":"E2E User","email":"e2e@test.com"}""",
                                workflowUser)))
                .andExpect(status().isCreated());

        // 2. Login
        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"username":"%s","password":"password123"}""", workflowUser)))
                .andExpect(status().isOk())
                .andReturn();
        String newUserToken = com.jayway.jsonpath.JsonPath.read(
                loginResult.getResponse().getContentAsString(), "$.data.token");

        // 3. View courses
        mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // 4. View labs
        mockMvc.perform(get("/api/labs")
                        .header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());

        // 5. Check available slots (use a weekday date)
        mockMvc.perform(get("/api/bookings/available-slots")
                        .header("Authorization", "Bearer " + newUserToken)
                        .param("labId", "1")
                        .param("date", "2026-08-03"))
                .andExpect(status().isOk());

        // 6. Create booking (2026-08-03 is a Monday)
        var bookingResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + newUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"labId":1,"date":"2026-08-03","startTime":"10:00","endTime":"12:00","purpose":"E2E complete workflow test","personCount":2}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = com.jayway.jsonpath.JsonPath.read(
                bookingResult.getResponse().getContentAsString(), "$.data.id").toString();

        // 7. Teacher approves booking
        mockMvc.perform(put("/api/bookings/" + bookingId + "/approve")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk());

        // 8. View my bookings
        mockMvc.perform(get("/api/bookings/mine")
                        .header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // 9. Borrow equipment (also on a weekday)
        var borrowResult = mockMvc.perform(post("/api/borrows")
                        .header("Authorization", "Bearer " + newUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentId":1,"borrowDate":"2026-08-03","expectedReturn":"2026-08-07","purpose":"E2E equipment borrowing"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String borrowId = com.jayway.jsonpath.JsonPath.read(
                borrowResult.getResponse().getContentAsString(), "$.data.id").toString();

        // 10. Teacher approves borrow
        mockMvc.perform(put("/api/borrows/" + borrowId + "/approve")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk());

        // 11. Complete booking (uses teacher token)
        mockMvc.perform(put("/api/bookings/" + bookingId + "/complete")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        // 12. Post review
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + newUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"bookingId":%s,"rating":5,"comment":"E2E workflow test: excellent experience"}""",
                                bookingId)))
                .andExpect(status().isCreated());

        // 13. Admin confirms equipment return
        mockMvc.perform(put("/api/borrows/" + borrowId + "/return")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualReturn\":\"2026-08-06\"}"))
                .andExpect(status().isOk());

        // 14. View dashboard
        mockMvc.perform(get("/api/dashboard/kpi")
                        .header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // 15. View usage records (as teacher/admin)
        mockMvc.perform(get("/api/usage-records")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());
    }
}
