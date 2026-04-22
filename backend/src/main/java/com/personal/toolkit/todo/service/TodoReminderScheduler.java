package com.personal.toolkit.todo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定时触发 Todo 提醒扫描任务。
 */
@Component
public class TodoReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(TodoReminderScheduler.class);

    private final TodoReminderService todoReminderService;
    private final AtomicBoolean missingTableWarningLogged = new AtomicBoolean(false);
    private final AtomicBoolean scanDisabled = new AtomicBoolean(false);

    public TodoReminderScheduler(TodoReminderService todoReminderService) {
        this.todoReminderService = todoReminderService;
    }

    @Scheduled(fixedDelayString = "${app.todo.reminder.scan-delay:60000}")
    public void scanAndSendReminders() {
        if (scanDisabled.get()) {
            return;
        }

        try {
            todoReminderService.generateDueReminderEvents();
            missingTableWarningLogged.set(false);
        } catch (InvalidDataAccessResourceUsageException ex) {
            scanDisabled.set(true);
            if (missingTableWarningLogged.compareAndSet(false, true)) {
                log.warn("Skipping reminder scan because reminder event table is unavailable. Run backend/sql/alter_todo_item_phase7_reminder_events.sql to enable reminder delivery.");
            }
        }
    }
}
