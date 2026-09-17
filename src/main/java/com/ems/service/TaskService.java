package com.ems.service;

import com.ems.dto.*;
import com.ems.entity.*;
import com.ems.exception.BadRequestException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.UnauthorizedActionException;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.TaskRepository;
import com.ems.repository.TaskUpdateRepository;
import com.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskUpdateRepository taskUpdateRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse createTask(TaskRequest request, Long adminUserId) {
        Employee assignee = employeeRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned employee not found"));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        long count = taskRepository.count() + 1;
        String taskId = String.format("TASK-%04d", count);
        while (taskRepository.findByTaskId(taskId).isPresent()) {
            count++;
            taskId = String.format("TASK-%04d", count);
        }

        Task task = Task.builder()
                .taskId(taskId)
                .title(request.getTitle())
                .description(request.getDescription())
                .project(request.getProject())
                .assignedTo(assignee)
                .assignedBy(admin)
                .priority(parsePriority(request.getPriority()))
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .status(TaskStatus.ASSIGNED)
                .progress(0)
                .approved(false)
                .build();
        task = taskRepository.save(task);

        if (assignee.getUser() != null) {
            notificationService.notifyUser(assignee.getUser().getId(),
                    "New task assigned: " + task.getTitle(), NotificationType.TASK_ASSIGNED);
        }

        return toResponse(task);
    }

    public List<TaskResponse> getAllTasks(String status, String priority, Long employeeId, String search) {
        TaskStatus statusEnum = (status == null || status.isBlank()) ? null : TaskStatus.valueOf(status.toUpperCase());
        Priority priorityEnum = (priority == null || priority.isBlank()) ? null : Priority.valueOf(priority.toUpperCase());
        String s = (search == null || search.isBlank()) ? null : search;
        return taskRepository.search(statusEnum, priorityEnum, employeeId, s).stream().map(this::toResponse).toList();
    }

    public TaskResponse getTaskById(Long id) {
        return toResponse(getTaskEntity(id));
    }

    public Task getTaskEntity(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    public List<TaskResponse> getMyTasks(Long employeeId) {
        return taskRepository.findMyTasksWithDetails(employeeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse updateTaskAdmin(Long id, TaskAdminUpdateRequest request) {
        Task task = getTaskEntity(id);

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getProject() != null) task.setProject(request.getProject());
        if (request.getAssignedToId() != null) {
            Employee assignee = employeeRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned employee not found"));
            task.setAssignedTo(assignee);
            if (assignee.getUser() != null) {
                notificationService.notifyUser(assignee.getUser().getId(),
                        "Task reassigned to you: " + task.getTitle(), NotificationType.TASK_ASSIGNED);
            }
        }
        if (request.getPriority() != null) task.setPriority(parsePriority(request.getPriority()));
        if (request.getStartDate() != null) task.setStartDate(request.getStartDate());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getStatus() != null) task.setStatus(parseStatus(request.getStatus()));
        if (request.getAdminComment() != null) task.setAdminComment(request.getAdminComment());
        if (request.getApproved() != null) {
            task.setApproved(request.getApproved());
            if (request.getApproved() && task.getAssignedTo().getUser() != null) {
                notificationService.notifyUser(task.getAssignedTo().getUser().getId(),
                        "Your task was approved: " + task.getTitle(), NotificationType.TASK_APPROVED);
            }
        }

        task = taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = getTaskEntity(id);
        taskUpdateRepository.findByTaskIdOrderByCreatedAtDesc(id).forEach(taskUpdateRepository::delete);
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse updateStatusByEmployee(Long taskId, Long employeeId, TaskStatusUpdateRequest request) {
        Task task = getTaskEntity(taskId);
        if (!task.getAssignedTo().getId().equals(employeeId)) {
            throw new UnauthorizedActionException("You cannot modify another employee's task");
        }

        TaskStatus newStatus = parseStatus(request.getStatus());
        task.setStatus(newStatus);
        if (request.getProgress() != null) {
            validateProgress(request.getProgress());
            task.setProgress(request.getProgress());
        }
        if (newStatus == TaskStatus.COMPLETED) {
            task.setProgress(100);
            task.setApproved(false);
        }
        task = taskRepository.save(task);

        logUpdate(task, employeeId, newStatus, task.getProgress(), request.getComment(), request.getAttachmentUrl());

        notifyAdminOfUpdate(task, newStatus);

        return toResponse(task);
    }

    @Transactional
    public TaskUpdateResponse addTaskUpdate(Long taskId, Long employeeId, TaskUpdateRequest request) {
        Task task = getTaskEntity(taskId);
        if (!task.getAssignedTo().getId().equals(employeeId)) {
            throw new UnauthorizedActionException("You cannot modify another employee's task");
        }

        TaskStatus status = task.getStatus();
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            status = parseStatus(request.getStatus());
            task.setStatus(status);
        }
        Integer progress = task.getProgress();
        if (request.getProgress() != null) {
            validateProgress(request.getProgress());
            progress = request.getProgress();
            task.setProgress(progress);
        }
        if (status == TaskStatus.COMPLETED) {
            progress = 100;
            task.setProgress(100);
            task.setApproved(false);
        }
        taskRepository.save(task);

        TaskUpdate update = logUpdate(task, employeeId, status, progress, request.getComment(), request.getAttachmentUrl());

        notifyAdminOfUpdate(task, status);

        return toUpdateResponse(update);
    }

    public List<TaskUpdateResponse> getTaskHistory(Long taskId) {
        return taskUpdateRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream().map(this::toUpdateResponse).toList();
    }

    private TaskUpdate logUpdate(Task task, Long employeeId, TaskStatus status, Integer progress, String comment, String attachmentUrl) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        TaskUpdate update = TaskUpdate.builder()
                .task(task)
                .employee(employee)
                .status(status)
                .progress(progress == null ? task.getProgress() : progress)
                .comment(comment)
                .attachmentUrl(attachmentUrl)
                .build();
        return taskUpdateRepository.save(update);
    }

    private void notifyAdminOfUpdate(Task task, TaskStatus status) {
        String msg = status == TaskStatus.COMPLETED
                ? task.getAssignedTo().getName() + " completed task: " + task.getTitle()
                : task.getAssignedTo().getName() + " updated task: " + task.getTitle() + " (" + status + ")";
        NotificationType type = status == TaskStatus.COMPLETED ? NotificationType.TASK_COMPLETED : NotificationType.TASK_UPDATED;
        notificationService.notifyAllAdmins(msg, type);
    }

    private void validateProgress(Integer progress) {
        if (progress < 0 || progress > 100) {
            throw new BadRequestException("Progress must be between 0 and 100");
        }
    }

    private Priority parsePriority(String value) {
        try {
            return Priority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid priority: " + value);
        }
    }

    private TaskStatus parseStatus(String value) {
        try {
            return TaskStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid task status: " + value);
        }
    }

    private TaskResponse toResponse(Task t) {
        return TaskResponse.builder()
                .id(t.getId())
                .taskId(t.getTaskId())
                .title(t.getTitle())
                .description(t.getDescription())
                .project(t.getProject())
                .assignedToId(t.getAssignedTo().getId())
                .assignedToEmployeeCode(t.getAssignedTo().getEmployeeId())
                .assignedToName(t.getAssignedTo().getName())
                .assignedByName(t.getAssignedBy().getUsername())
                .priority(t.getPriority().name())
                .startDate(t.getStartDate())
                .dueDate(t.getDueDate())
                .status(t.getStatus().name())
                .progress(t.getProgress())
                .adminComment(t.getAdminComment())
                .approved(t.isApproved())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private TaskUpdateResponse toUpdateResponse(TaskUpdate u) {
        return TaskUpdateResponse.builder()
                .id(u.getId())
                .taskId(u.getTask().getId())
                .taskTitle(u.getTask().getTitle())
                .employeeName(u.getEmployee().getName())
                .status(u.getStatus().name())
                .progress(u.getProgress())
                .comment(u.getComment())
                .attachmentUrl(u.getAttachmentUrl())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
