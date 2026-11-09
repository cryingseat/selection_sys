package com.labselection.mapper;

import com.labselection.domain.entity.Student;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentMapper {
    Student findById(@Param("id") Long id);

    Student findByUserId(@Param("userId") Long userId);

    Student findByStudentNo(@Param("studentNo") String studentNo);

    List<Student> findAll();

    List<Student> search(@Param("keyword") String keyword,
                         @Param("grade") String grade,
                         @Param("major") String major,
                         @Param("offset") int offset,
                         @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword,
                     @Param("grade") String grade,
                     @Param("major") String major);

    int insert(Student student);

    int update(Student student);

    long count();
}
