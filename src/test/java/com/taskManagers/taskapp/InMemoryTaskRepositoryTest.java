package com.taskManagers.taskapp;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskRepositoryTest {

    private final InMemoryTaskRepository repository = new InMemoryTaskRepository();

    @Test
    void seededDataIncludesHighPriorityDueToday() {
        List<TaskItem> today = repository.findToday(LocalDate.now());
        assertFalse(today.isEmpty());
        assertEquals(PriorityLevel.P1, today.get(0).priority());
    }

    @Test
    void insertAndUpdateLifecycle() throws SQLException {
        TaskItem draft = TaskItem.builder()
                .title("Edge Case Task with Long Title to Check Wrapping Behavior")
                .priority(PriorityLevel.P2)
                .dueDate(LocalDate.now().plusDays(3))
                .status(TaskStatus.OPEN)
                .tags("edge,wrap")
                .build();
        TaskItem saved = repository.insert(draft);
        assertTrue(saved.id() > 0);

        TaskItem updated = saved.toBuilder()
                .priority(PriorityLevel.P1)
                .status(TaskStatus.DONE)
                .build();
        assertTrue(repository.update(updated));

        assertTrue(repository.markDone(saved.id()));
        assertTrue(repository.delete(saved.id()));
    }
}
