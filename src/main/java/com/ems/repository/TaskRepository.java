package com.ems.repository;

import com.ems.entity.Priority;
import com.ems.entity.Task;
import com.ems.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByTaskId(String taskId);

    @Query("""
        SELECT t
        FROM Task t
        JOIN FETCH t.assignedTo
        JOIN FETCH t.assignedBy
        WHERE t.assignedTo.id = :employeeId
        ORDER BY t.createdAt DESC
    """)
    List<Task> findMyTasksWithDetails(@Param("employeeId") Long employeeId);

    long countByStatus(TaskStatus status);

    long countByAssignedToId(Long employeeId);

    long countByAssignedToIdAndStatus(Long employeeId, TaskStatus status);

    @Query("""
        SELECT t
        FROM Task t
        JOIN FETCH t.assignedTo
        JOIN FETCH t.assignedBy
        WHERE (:status IS NULL OR t.status = :status)
        AND (:priority IS NULL OR t.priority = :priority)
        AND (:employeeId IS NULL OR t.assignedTo.id = :employeeId)
        AND (
            :search IS NULL
            OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.taskId) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        ORDER BY t.createdAt DESC
    """)
    List<Task> search(
            @Param("status") TaskStatus status,
            @Param("priority") Priority priority,
            @Param("employeeId") Long employeeId,
            @Param("search") String search
    );
}