import type { UserInfo, LoginResponse } from "@labreserve/shared";

export const mockStudentUser: UserInfo = {
  id: "1",
  username: "student1",
  realName: "Student One",
  role: "STUDENT",
  email: "student1@test.com",
  phone: "13800000001",
  enabled: true,
};

export const mockTeacherUser: UserInfo = {
  id: "2",
  username: "teacher1",
  realName: "Teacher One",
  role: "TEACHER",
  email: "teacher1@test.com",
  phone: "13800000002",
  enabled: true,
};

export const mockAdminUser: UserInfo = {
  id: "3",
  username: "admin1",
  realName: "Admin One",
  role: "ADMIN",
  email: "admin1@test.com",
  phone: "13800000003",
  enabled: true,
};

export const mockLoginResponse: LoginResponse = {
  token: "eyJhbGciOiJIUzI1NiJ9.test.mock",
  expiresIn: 86400,
  user: mockStudentUser,
};

export const mockBooking = {
  id: "1",
  labId: "1",
  labName: "Computer Lab A",
  userId: "1",
  userName: "Student One",
  date: "2026-07-01",
  startTime: "10:00",
  endTime: "12:00",
  purpose: "Testing",
  personCount: 2,
  status: "PENDING",
};

export const mockLab = {
  id: "1",
  name: "Computer Lab A",
  location: "Building A-301",
  capacity: 40,
  status: "AVAILABLE",
};

export const mockDashboardKpi = {
  todayBookings: 12,
  todayBorrows: 3,
  labUsageRate: 0.65,
  pendingApprovals: 5,
};
