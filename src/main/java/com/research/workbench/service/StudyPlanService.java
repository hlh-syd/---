package com.research.workbench.service;

import com.research.workbench.service.CurrentUserService;
import com.research.workbench.domain.CalendarEvent;
import com.research.workbench.domain.CheckinRecord;
import com.research.workbench.domain.PomodoroSessionLog;
import com.research.workbench.domain.StudyTask;
import com.research.workbench.domain.UserDailyUsage;
import com.research.workbench.domain.Workspace;
import com.research.workbench.repository.CalendarEventRepository;
import com.research.workbench.repository.CheckinRecordRepository;
import com.research.workbench.repository.PomodoroSessionLogRepository;
import com.research.workbench.repository.StudyTaskRepository;
import com.research.workbench.repository.UserDailyUsageRepository;
import com.research.workbench.repository.WorkspaceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudyPlanService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int USAGE_INCREMENT_MINUTES = 30;

    private final StudyTaskRepository studyTaskRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final PomodoroSessionLogRepository pomodoroSessionLogRepository;
    private final CheckinRecordRepository checkinRecordRepository;
    private final UserDailyUsageRepository userDailyUsageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final CurrentUserService currentUserService;

    public StudyPlanService(
            StudyTaskRepository studyTaskRepository,
            CalendarEventRepository calendarEventRepository,
            PomodoroSessionLogRepository pomodoroSessionLogRepository,
            CheckinRecordRepository checkinRecordRepository,
            UserDailyUsageRepository userDailyUsageRepository,
            WorkspaceRepository workspaceRepository,
            CurrentUserService currentUserService
    ) {
        this.studyTaskRepository = studyTaskRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.pomodoroSessionLogRepository = pomodoroSessionLogRepository;
        this.checkinRecordRepository = checkinRecordRepository;
        this.userDailyUsageRepository = userDailyUsageRepository;
        this.workspaceRepository = workspaceRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTasks() {
        return studyTaskRepository.findTop20ByUserIdOrderByDueTimeAsc(currentUserService.requireCurrentUserId())
                .stream()
                .map(this::toTaskDto)
                .toList();
    }

    public Map<String, Object> createTask(TaskUpsert request) {
        StudyTask task = new StudyTask();
        task.setUserId(currentUserService.requireCurrentUserId());
        task.setWorkspaceId(requireWorkspaceId());
        task.setProjectId(request.projectId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriorityLevel(request.priorityLevel() == null ? 2 : request.priorityLevel());
        task.setTaskStatus(request.taskStatus() == null || request.taskStatus().isBlank() ? "TODO" : request.taskStatus());
        task.setDueTime(request.dueTime());
        return toTaskDto(studyTaskRepository.save(task));
    }

    public Map<String, Object> updateTask(Long id, TaskUpsert request) {
        StudyTask task = requireTask(id);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriorityLevel(request.priorityLevel() == null ? task.getPriorityLevel() : request.priorityLevel());
        task.setTaskStatus(request.taskStatus() == null || request.taskStatus().isBlank() ? task.getTaskStatus() : request.taskStatus());
        task.setDueTime(request.dueTime());
        if ("DONE".equalsIgnoreCase(task.getTaskStatus()) && task.getFinishedAt() == null) {
            task.setFinishedAt(LocalDateTime.now());
        }
        return toTaskDto(studyTaskRepository.save(task));
    }

    public void deleteTask(Long id) {
        studyTaskRepository.delete(requireTask(id));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCalendar() {
        return calendarEventRepository
                .findTop10ByUserIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(currentUserService.requireCurrentUserId(), LocalDate.now().minusDays(30).atStartOfDay())
                .stream()
                .map(this::toCalendarDto)
                .toList();
    }

    public Map<String, Object> createCalendarEvent(CalendarUpsert request) {
        CalendarEvent event = new CalendarEvent();
        event.setUserId(currentUserService.requireCurrentUserId());
        event.setWorkspaceId(requireWorkspaceId());
        event.setProjectId(request.projectId());
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventType(request.eventType() == null || request.eventType().isBlank() ? "PLAN" : request.eventType());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        return toCalendarDto(calendarEventRepository.save(event));
    }

    public Map<String, Object> updateCalendarEvent(Long id, CalendarUpsert request) {
        CalendarEvent event = requireCalendar(id);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventType(request.eventType() == null || request.eventType().isBlank() ? event.getEventType() : request.eventType());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        return toCalendarDto(calendarEventRepository.save(event));
    }

    public void deleteCalendarEvent(Long id) {
        calendarEventRepository.delete(requireCalendar(id));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPomodoro() {
        return pomodoroSessionLogRepository.findTop20ByUserIdOrderBySessionTimeDesc(currentUserService.requireCurrentUserId())
                .stream()
                .map(this::toPomodoroDto)
                .toList();
    }

    public Map<String, Object> createPomodoro(PomodoroUpsert request) {
        Long userId = currentUserService.requireCurrentUserId();
        LocalDateTime sessionTime = request.sessionTime() == null ? LocalDateTime.now() : request.sessionTime();
        boolean completed = Boolean.TRUE.equals(request.completed());
        PomodoroSessionLog log = new PomodoroSessionLog();
        log.setUserId(userId);
        log.setSessionTime(sessionTime);
        log.setSessionCount((int) pomodoroSessionLogRepository.countByUserId(userId) + 1);
        LocalDateTime dayStart = sessionTime.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        log.setDailySequence((int) pomodoroSessionLogRepository.countByUserIdAndSessionTimeBetween(userId, dayStart, dayEnd) + 1);
        log.setFocusMinutes(completed ? (request.focusMinutes() == null ? 25 : request.focusMinutes()) : null);
        log.setFinishedAt(request.finishedAt());
        log.setDescription(completed ? normalizeDesc(request.desc(), "已完成") : "未完成");
        log.setCreatedAt(LocalDateTime.now());
        return toPomodoroDto(pomodoroSessionLogRepository.save(log));
    }

    public void deletePomodoro(Long id) {
        pomodoroSessionLogRepository.delete(requirePomodoro(id));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTodayUsage() {
        Long userId = currentUserService.requireCurrentUserId();
        UserDailyUsage usage = userDailyUsageRepository.findByUserIdAndUsageDate(userId, LocalDate.now())
                .orElseGet(() -> newUsageRecord(userId, LocalDate.now()));
        return toUsageDto(usage, false);
    }

    public Map<String, Object> incrementTodayUsage() {
        Long userId = currentUserService.requireCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        UserDailyUsage usage = userDailyUsageRepository.findByUserIdAndUsageDate(userId, today)
                .orElseGet(() -> newUsageRecord(userId, today));
        boolean incremented = usage.getLastIncrementAt() == null
                || !usage.getLastIncrementAt().plusMinutes(USAGE_INCREMENT_MINUTES).isAfter(now);
        if (incremented) {
            usage.setTodayTime((usage.getTodayTime() == null ? 0 : usage.getTodayTime()) + USAGE_INCREMENT_MINUTES);
            usage.setLastIncrementAt(now);
        }
        return toUsageDto(userDailyUsageRepository.save(usage), incremented);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCheckins() {
        return checkinRecordRepository.findTop20ByUserIdOrderByCheckinDateDesc(currentUserService.requireCurrentUserId())
                .stream()
                .map(this::toCheckinDto)
                .toList();
    }

    public Map<String, Object> createOrUpdateCheckin(LocalDate date, Integer focusMinutes, Integer completedTaskCount, String summaryText) {
        Long userId = currentUserService.requireCurrentUserId();
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        CheckinRecord existing = checkinRecordRepository.findTop20ByUserIdOrderByCheckinDateDesc(userId).stream()
                .filter(record -> targetDate.equals(record.getCheckinDate()))
                .findFirst()
                .orElse(null);
        CheckinRecord record = existing == null ? new CheckinRecord() : existing;
        if (existing == null) {
            record.setUserId(userId);
            record.setCreatedAt(LocalDateTime.now());
            record.setCheckinDate(targetDate);
        }
        record.setFocusMinutes(focusMinutes == null ? 0 : focusMinutes);
        record.setCompletedTaskCount(completedTaskCount == null ? 0 : completedTaskCount);
        record.setSummaryText(summaryText);
        record.setMoodCode("GOOD");
        return toCheckinDto(checkinRecordRepository.save(record));
    }

    public void deleteCheckin(Long id) {
        checkinRecordRepository.delete(requireCheckin(id));
    }

    private StudyTask requireTask(Long id) {
        StudyTask task = studyTaskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        if (!currentUserService.requireCurrentUserId().equals(task.getUserId())) {
            throw new IllegalArgumentException("无权访问该任务");
        }
        return task;
    }

    private CalendarEvent requireCalendar(Long id) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("日历事件不存在: " + id));
        if (!currentUserService.requireCurrentUserId().equals(event.getUserId())) {
            throw new IllegalArgumentException("无权访问该日历事件");
        }
        return event;
    }

    private PomodoroSessionLog requirePomodoro(Long id) {
        PomodoroSessionLog record = pomodoroSessionLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("番茄钟记录不存在: " + id));
        if (!currentUserService.requireCurrentUserId().equals(record.getUserId())) {
            throw new IllegalArgumentException("无权访问该番茄钟记录");
        }
        return record;
    }

    private CheckinRecord requireCheckin(Long id) {
        CheckinRecord record = checkinRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("打卡记录不存在: " + id));
        if (!currentUserService.requireCurrentUserId().equals(record.getUserId())) {
            throw new IllegalArgumentException("无权访问该打卡记录");
        }
        return record;
    }

    private Long requireWorkspaceId() {
        Workspace workspace = workspaceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(currentUserService.requireCurrentUserId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("当前用户尚未初始化工作区"));
        return workspace.getId();
    }

    private Map<String, Object> toTaskDto(StudyTask task) {
        return Map.of(
                "id", task.getId(),
                "title", task.getTitle(),
                "description", task.getDescription() == null ? "" : task.getDescription(),
                "priority", priorityLabel(task.getPriorityLevel()),
                "priorityLevel", task.getPriorityLevel() == null ? 2 : task.getPriorityLevel(),
                "deadline", task.getDueTime() == null ? "" : task.getDueTime().format(DATE_FORMATTER),
                "taskStatus", task.getTaskStatus(),
                "done", "DONE".equalsIgnoreCase(task.getTaskStatus())
        );
    }

    private Map<String, Object> toCalendarDto(CalendarEvent event) {
        return Map.of(
                "id", event.getId(),
                "title", event.getTitle(),
                "description", event.getDescription() == null ? "" : event.getDescription(),
                "eventType", event.getEventType(),
                "startTime", event.getStartTime().format(DATE_FORMATTER),
                "endTime", event.getEndTime().format(DATE_FORMATTER),
                "day", event.getStartTime().toLocalDate().toString(),
                "text", event.getTitle()
        );
    }

    private Map<String, Object> toPomodoroDto(PomodoroSessionLog record) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", record.getId());
        dto.put("focusMinutes", record.getFocusMinutes());
        dto.put("sessionTime", record.getSessionTime().format(DATE_FORMATTER));
        dto.put("finishedAt", record.getFinishedAt() == null ? "" : record.getFinishedAt().format(DATE_FORMATTER));
        dto.put("sessionCount", record.getSessionCount());
        dto.put("dailySequence", record.getDailySequence());
        dto.put("desc", record.getDescription() == null ? "" : record.getDescription());
        return dto;
    }

    private String normalizeDesc(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private Map<String, Object> toUsageDto(UserDailyUsage usage, boolean incremented) {
        int todayTime = usage.getTodayTime() == null ? 0 : usage.getTodayTime();
        return Map.of(
                "date", usage.getUsageDate().toString(),
                "todayTime", todayTime,
                "todayTimeText", formatMinutes(todayTime),
                "incremented", incremented
        );
    }

    private Map<String, Object> toCheckinDto(CheckinRecord record) {
        return Map.of(
                "id", record.getId(),
                "date", record.getCheckinDate().toString(),
                "focusMinutes", record.getFocusMinutes(),
                "completedTaskCount", record.getCompletedTaskCount(),
                "summary", record.getSummaryText() == null ? "" : record.getSummaryText()
        );
    }

    private String priorityLabel(Integer priority) {
        if (priority == null) {
            return "中优先级";
        }
        return switch (priority) {
            case 1 -> "高优先级";
            case 3 -> "低优先级";
            default -> "中优先级";
        };
    }

    private UserDailyUsage newUsageRecord(Long userId, LocalDate usageDate) {
        UserDailyUsage usage = new UserDailyUsage();
        usage.setUserId(userId);
        usage.setUsageDate(usageDate);
        usage.setTodayTime(0);
        return usage;
    }

    private String formatMinutes(int minutes) {
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return String.format("%02d:%02d", hours, remainingMinutes);
    }

    public record TaskUpsert(Long projectId, String title, String description, Integer priorityLevel, LocalDateTime dueTime, String taskStatus) {
    }

    public record CalendarUpsert(Long projectId, String title, String description, String eventType, LocalDateTime startTime, LocalDateTime endTime) {
    }

    public record PomodoroUpsert(
            Integer focusMinutes,
            Boolean completed,
            LocalDateTime sessionTime,
            LocalDateTime finishedAt,
            String desc
    ) {
    }
}
