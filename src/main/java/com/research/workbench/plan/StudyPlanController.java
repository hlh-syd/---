package com.research.workbench.plan;

import com.research.workbench.api.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/plans")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    public StudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> listTasks() {
        return ApiResponse.ok(studyPlanService.listTasks());
    }

    @PostMapping("/tasks")
    public ApiResponse<Map<String, Object>> createTask(@RequestBody TaskRequest request) {
        return ApiResponse.ok(studyPlanService.createTask(request.toUpsert()));
    }

    @PutMapping("/tasks/{id}")
    public ApiResponse<Map<String, Object>> updateTask(@PathVariable Long id, @RequestBody TaskRequest request) {
        return ApiResponse.ok(studyPlanService.updateTask(id, request.toUpsert()));
    }

    @DeleteMapping("/tasks/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable Long id) {
        studyPlanService.deleteTask(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/calendar")
    public ApiResponse<List<Map<String, Object>>> listCalendar() {
        return ApiResponse.ok(studyPlanService.listCalendar());
    }

    @PostMapping("/calendar")
    public ApiResponse<Map<String, Object>> createCalendar(@RequestBody CalendarRequest request) {
        return ApiResponse.ok(studyPlanService.createCalendarEvent(request.toUpsert()));
    }

    @PutMapping("/calendar/{id}")
    public ApiResponse<Map<String, Object>> updateCalendar(@PathVariable Long id, @RequestBody CalendarRequest request) {
        return ApiResponse.ok(studyPlanService.updateCalendarEvent(id, request.toUpsert()));
    }

    @DeleteMapping("/calendar/{id}")
    public ApiResponse<Void> deleteCalendar(@PathVariable Long id) {
        studyPlanService.deleteCalendarEvent(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/pomodoro")
    public ApiResponse<List<Map<String, Object>>> listPomodoro() {
        return ApiResponse.ok(studyPlanService.listPomodoro());
    }

    @PostMapping("/pomodoro")
    public ApiResponse<Map<String, Object>> createPomodoro(@RequestBody PomodoroRequest request) {
        return ApiResponse.ok(studyPlanService.createPomodoro(request.toUpsert()));
    }

    @DeleteMapping("/pomodoro/{id}")
    public ApiResponse<Void> deletePomodoro(@PathVariable Long id) {
        studyPlanService.deletePomodoro(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/usage/today")
    public ApiResponse<Map<String, Object>> getTodayUsage() {
        return ApiResponse.ok(studyPlanService.getTodayUsage());
    }

    @PostMapping("/usage/ping")
    public ApiResponse<Map<String, Object>> incrementTodayUsage() {
        return ApiResponse.ok(studyPlanService.incrementTodayUsage());
    }

    @GetMapping("/checkins")
    public ApiResponse<List<Map<String, Object>>> listCheckins() {
        return ApiResponse.ok(studyPlanService.listCheckins());
    }

    @PostMapping("/checkins")
    public ApiResponse<Map<String, Object>> createCheckin(@RequestBody CheckinRequest request) {
        return ApiResponse.ok(studyPlanService.createOrUpdateCheckin(request.date(), request.focusMinutes(), request.completedTaskCount(), request.summary()));
    }

    @DeleteMapping("/checkins/{id}")
    public ApiResponse<Void> deleteCheckin(@PathVariable Long id) {
        studyPlanService.deleteCheckin(id);
        return ApiResponse.ok(null);
    }

    public record TaskRequest(Long projectId, @NotBlank String title, String description, Integer priorityLevel, LocalDateTime dueTime, String taskStatus) {
        StudyPlanService.TaskUpsert toUpsert() {
            return new StudyPlanService.TaskUpsert(projectId, title, description, priorityLevel, dueTime, taskStatus);
        }
    }

    public record CalendarRequest(Long projectId, @NotBlank String title, String description, String eventType, @NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime) {
        StudyPlanService.CalendarUpsert toUpsert() {
            return new StudyPlanService.CalendarUpsert(projectId, title, description, eventType, startTime, endTime);
        }
    }

    public record PomodoroRequest(
            Integer focusMinutes,
            Boolean completed,
            LocalDateTime sessionTime,
            LocalDateTime finishedAt,
            String desc
    ) {
        StudyPlanService.PomodoroUpsert toUpsert() {
            return new StudyPlanService.PomodoroUpsert(focusMinutes, completed, sessionTime, finishedAt, desc);
        }
    }

    public record CheckinRequest(LocalDate date, Integer focusMinutes, Integer completedTaskCount, String summary) {
    }
}
