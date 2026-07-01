package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.StudentCreateRequest;
import com.labreserve.dto.StudentUpdateRequest;
import com.labreserve.dto.StudentVO;
import com.labreserve.entity.Lab;
import com.labreserve.entity.Student;
import com.labreserve.entity.User;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.StudentMapper;
import com.labreserve.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentMapper studentMapper;
    private final LabMapper labMapper;
    private final UserMapper userMapper;

    public StudentService(StudentMapper studentMapper, LabMapper labMapper, UserMapper userMapper) {
        this.studentMapper = studentMapper;
        this.labMapper = labMapper;
        this.userMapper = userMapper;
    }

    public IPage<StudentVO> listStudents(Long labId, String name, int pageNum, int pageSize) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (labId != null) {
            wrapper.eq(Student::getLabId, labId);
        }
        if (name != null && !name.isBlank()) {
            wrapper.like(Student::getName, name);
        }
        wrapper.orderByDesc(Student::getCreatedAt);

        Page<Student> page = new Page<>(pageNum, pageSize);
        IPage<Student> result = studentMapper.selectPage(page, wrapper);
        IPage<StudentVO> voPage = result.convert(this::toStudentVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    public StudentVO getStudentById(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException("NOT_FOUND", "学生信息不存在");
        }
        StudentVO vo = toStudentVO(student);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    @Transactional
    public StudentVO createStudent(StudentCreateRequest request, Long creatorId) {
        Student student = new Student();
        student.setLabId(request.getLabId());
        student.setName(request.getName());
        student.setGender(request.getGender());
        student.setAge(request.getAge());
        student.setAddress(request.getAddress());
        student.setCreatorId(creatorId);
        studentMapper.insert(student);
        return toStudentVO(student);
    }

    @Transactional
    public StudentVO updateStudent(Long id, StudentUpdateRequest request) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException("NOT_FOUND", "学生信息不存在");
        }

        LambdaUpdateWrapper<Student> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Student::getId, id);

        if (request.getLabId() != null) {
            wrapper.set(Student::getLabId, request.getLabId());
        }
        if (request.getName() != null) {
            wrapper.set(Student::getName, request.getName());
        }
        if (request.getGender() != null) {
            wrapper.set(Student::getGender, request.getGender());
        }
        if (request.getAge() != null) {
            wrapper.set(Student::getAge, request.getAge());
        }
        if (request.getAddress() != null) {
            wrapper.set(Student::getAddress, request.getAddress());
        }

        studentMapper.update(wrapper);

        Student updated = studentMapper.selectById(id);
        return toStudentVO(updated);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException("NOT_FOUND", "学生信息不存在");
        }
        studentMapper.deleteById(id);
    }

    private StudentVO toStudentVO(Student student) {
        return StudentVO.builder()
                .id(student.getId())
                .labId(student.getLabId())
                .name(student.getName())
                .gender(student.getGender())
                .age(student.getAge())
                .address(student.getAddress())
                .creatorId(student.getCreatorId())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }

    private void populateJoinedFields(List<StudentVO> vos) {
        Set<Long> labIds = vos.stream()
                .map(StudentVO::getLabId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> creatorIds = vos.stream()
                .map(StudentVO::getCreatorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> labIdToName = Collections.emptyMap();
        if (!labIds.isEmpty()) {
            List<Lab> labs = labMapper.selectBatchIds(labIds);
            labIdToName = labs.stream().collect(Collectors.toMap(Lab::getId, Lab::getName));
        }

        Map<Long, String> userIdToName = Collections.emptyMap();
        if (!creatorIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(creatorIds);
            userIdToName = users.stream().collect(Collectors.toMap(User::getId, User::getRealName));
        }

        for (StudentVO vo : vos) {
            if (vo.getLabId() != null) {
                vo.setLabName(labIdToName.get(vo.getLabId()));
            }
            if (vo.getCreatorId() != null) {
                vo.setCreatorName(userIdToName.get(vo.getCreatorId()));
            }
        }
    }
}
