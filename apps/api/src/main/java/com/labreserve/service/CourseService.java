package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.CourseCreateRequest;
import com.labreserve.dto.CourseUpdateRequest;
import com.labreserve.dto.CourseVO;
import com.labreserve.entity.Course;
import com.labreserve.entity.Lab;
import com.labreserve.entity.User;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.CourseMapper;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final Pattern SEMESTER_PATTERN = Pattern.compile("\\d{4}-\\d{4}-[12]");

    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final LabMapper labMapper;

    public CourseService(CourseMapper courseMapper, UserMapper userMapper, LabMapper labMapper) {
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
        this.labMapper = labMapper;
    }

    public IPage<CourseVO> listCourses(String semester, Long teacherId, Long labId, String className,
                                       int pageNum, int pageSize) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();

        if (semester != null && !semester.isBlank()) {
            wrapper.eq(Course::getSemester, semester);
        }
        if (teacherId != null) {
            wrapper.eq(Course::getTeacherId, teacherId);
        }
        if (labId != null) {
            wrapper.eq(Course::getLabId, labId);
        }
        if (className != null && !className.isBlank()) {
            wrapper.eq(Course::getClassName, className);
        }

        wrapper.orderByDesc(Course::getCreatedAt);

        Page<Course> page = new Page<>(pageNum, pageSize);
        IPage<Course> result = courseMapper.selectPage(page, wrapper);
        IPage<CourseVO> voPage = result.convert(this::toCourseVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    public IPage<CourseVO> listMyCourses(Long userId, String role, String className,
                                         int pageNum, int pageSize) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();

        if ("STUDENT".equals(role)) {
            if (className == null || className.isBlank()) {
                throw new BusinessException("CLASS_REQUIRED", "学生查询课表需要提供班级");
            }
            wrapper.eq(Course::getClassName, className);
        } else if ("TEACHER".equals(role)) {
            wrapper.eq(Course::getTeacherId, userId);
        }
        // ADMIN: no extra filter, returns all

        wrapper.orderByAsc(Course::getDayOfWeek)
                .orderByAsc(Course::getStartTime);

        Page<Course> page = new Page<>(pageNum, pageSize);
        IPage<Course> result = courseMapper.selectPage(page, wrapper);
        IPage<CourseVO> voPage = result.convert(this::toCourseVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    public CourseVO getCourseById(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("NOT_FOUND", "课程不存在");
        }
        CourseVO vo = toCourseVO(course);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    @Transactional
    public CourseVO createCourse(CourseCreateRequest request) {
        validateSemester(request.getSemester());
        validateDayOfWeek(request.getDayOfWeek());
        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateDateRange(request.getStartDate(), request.getEndDate());

        Course course = new Course();
        course.setName(request.getName());
        course.setLabId(request.getLabId());
        course.setTeacherId(request.getTeacherId());
        course.setSemester(request.getSemester());
        course.setDayOfWeek(request.getDayOfWeek());
        course.setStartTime(request.getStartTime());
        course.setEndTime(request.getEndTime());
        course.setStartDate(LocalDate.parse(request.getStartDate()));
        course.setEndDate(LocalDate.parse(request.getEndDate()));
        course.setClassName(request.getClassName());

        courseMapper.insert(course);
        return toCourseVO(course);
    }

    @Transactional
    public CourseVO updateCourse(Long id, CourseUpdateRequest request) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("NOT_FOUND", "课程不存在");
        }

        LambdaUpdateWrapper<Course> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Course::getId, id);

        if (request.getName() != null) {
            wrapper.set(Course::getName, request.getName());
        }
        if (request.getLabId() != null) {
            wrapper.set(Course::getLabId, request.getLabId());
        }
        if (request.getTeacherId() != null) {
            wrapper.set(Course::getTeacherId, request.getTeacherId());
        }
        if (request.getDayOfWeek() != null) {
            validateDayOfWeek(request.getDayOfWeek());
            wrapper.set(Course::getDayOfWeek, request.getDayOfWeek());
        }
        if (request.getStartTime() != null) {
            String endTime = request.getEndTime() != null ? request.getEndTime() : course.getEndTime();
            validateTimeRange(request.getStartTime(), endTime);
            wrapper.set(Course::getStartTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            String startTime = request.getStartTime() != null ? request.getStartTime() : course.getStartTime();
            validateTimeRange(startTime, request.getEndTime());
            wrapper.set(Course::getEndTime, request.getEndTime());
        }
        if (request.getStartDate() != null) {
            wrapper.set(Course::getStartDate, LocalDate.parse(request.getStartDate()));
        }
        if (request.getEndDate() != null) {
            wrapper.set(Course::getEndDate, LocalDate.parse(request.getEndDate()));
        }
        if (request.getClassName() != null) {
            wrapper.set(Course::getClassName, request.getClassName());
        }

        courseMapper.update(wrapper);

        Course updated = courseMapper.selectById(id);
        CourseVO vo = toCourseVO(updated);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("NOT_FOUND", "课程不存在");
        }
        courseMapper.deleteById(id);
    }

    private void validateSemester(String semester) {
        if (!SEMESTER_PATTERN.matcher(semester).matches()) {
            throw new BusinessException("INVALID_SEMESTER", "学期格式无效，应为 YYYY-YYYY-[12]");
        }
    }

    private void validateDayOfWeek(Integer dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new BusinessException("INVALID_DAY_OF_WEEK", "星期必须在 1-7 之间");
        }
    }

    private void validateTimeRange(String startTime, String endTime) {
        if (startTime.compareTo(endTime) >= 0) {
            throw new BusinessException("INVALID_TIME_RANGE", "开始时间必须早于结束时间");
        }
    }

    private void validateDateRange(String startDate, String endDate) {
        if (LocalDate.parse(startDate).isAfter(LocalDate.parse(endDate))) {
            throw new BusinessException("INVALID_DATE_RANGE", "开始日期不能晚于结束日期");
        }
    }

    private CourseVO toCourseVO(Course course) {
        return CourseVO.builder()
                .id(course.getId())
                .name(course.getName())
                .labId(course.getLabId())
                .teacherId(course.getTeacherId())
                .semester(course.getSemester())
                .dayOfWeek(course.getDayOfWeek())
                .startTime(course.getStartTime())
                .endTime(course.getEndTime())
                .startDate(course.getStartDate().toString())
                .endDate(course.getEndDate().toString())
                .className(course.getClassName())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private void populateJoinedFields(List<CourseVO> vos) {
        if (vos.isEmpty()) {
            return;
        }

        Set<Long> labIds = vos.stream()
                .map(CourseVO::getLabId)
                .collect(Collectors.toSet());

        Set<Long> teacherIds = vos.stream()
                .map(CourseVO::getTeacherId)
                .collect(Collectors.toSet());

        Map<Long, String> labIdToName = Collections.emptyMap();
        if (!labIds.isEmpty()) {
            List<Lab> labs = labMapper.selectBatchIds(labIds);
            labIdToName = labs.stream()
                    .collect(Collectors.toMap(Lab::getId, Lab::getName));
        }

        Map<Long, String> userIdToName = Collections.emptyMap();
        if (!teacherIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(teacherIds);
            userIdToName = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getRealName));
        }

        for (CourseVO vo : vos) {
            vo.setLabName(labIdToName.get(vo.getLabId()));
            vo.setTeacherName(userIdToName.get(vo.getTeacherId()));
        }
    }
}
