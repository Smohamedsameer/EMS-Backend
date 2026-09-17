package com.ems.repository;

import com.ems.entity.TaskUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskUpdateRepository extends JpaRepository<TaskUpdate, Long> {
    List<TaskUpdate> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<TaskUpdate> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
