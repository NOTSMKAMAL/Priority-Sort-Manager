package com.taskManagers.taskapp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TaskRepositoryTest {

    private static DataSource dataSource;
    private static TaskRepository repository;

    @BeforeAll
    static void setupDataSource() throws SQLException {
        TaskAppConfig config = new TaskAppConfig(
                "jdbc:h2:mem:tasks;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                "",
                "",
                "",
                true,
                ""
        );
        dataSource = TaskDataSourceFactory.create(config);
        repository = new TaskRepository(dataSource);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS tasks");
            statement.execute("CREATE TABLE tasks (" +
                    "TaskNumber INTEGER AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "dueDate DATE, " +
                    "priority VARCHAR(12), " +
                    "status VARCHAR(12), " +
                    "type VARCHAR(255))");
        }
    }

    @AfterAll
    static void shutdown() throws Exception {
        if (repository != null) {
            repository.close();
        }
        if (dataSource instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    @BeforeEach
    void clean() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM tasks");
        }
    }

    @Test
    void roundTripCrudOperations() throws SQLException {
        TaskItem draft = TaskItem.builder()
                .title("Write docs")
                .dueDate(LocalDate.now())
                .priority(PriorityLevel.P1)
                .status(TaskStatus.OPEN)
                .tags("work")
                .build();

        TaskItem saved = repository.insert(draft);
        assertTrue(saved.id() > 0);

        Optional<TaskItem> loaded = repository.findById(saved.id());
        assertTrue(loaded.isPresent());
        assertEquals("Write docs", loaded.get().title());

        TaskItem updated = saved.toBuilder()
                .title("Write docs v2")
                .priority(PriorityLevel.P2)
                .build();
        assertTrue(repository.update(updated));

        Optional<TaskItem> reloaded = repository.findById(saved.id());
        assertTrue(reloaded.isPresent());
        assertEquals("Write docs v2", reloaded.get().title());
        assertEquals(PriorityLevel.P2, reloaded.get().priority());

        assertTrue(repository.markDone(saved.id()));
        assertEquals(TaskStatus.DONE, repository.findById(saved.id()).orElseThrow().status());

        assertTrue(repository.delete(saved.id()));
        assertTrue(repository.findById(saved.id()).isEmpty());
    }

    @Test
    void findTodayReturnsDueAndHighPriorityItems() throws SQLException {
        LocalDate today = LocalDate.now();
        TaskItem todayTask = TaskItem.builder()
                .title("Submit report")
                .dueDate(today)
                .priority(PriorityLevel.P2)
                .status(TaskStatus.OPEN)
                .build();
        TaskItem urgentTask = TaskItem.builder()
                .title("Prepare presentation")
                .dueDate(today.plusDays(2))
                .priority(PriorityLevel.P1)
                .status(TaskStatus.OPEN)
                .build();
        repository.insert(todayTask);
        repository.insert(urgentTask);

        List<TaskItem> results = repository.findToday(today);
        assertEquals(2, results.size());
        assertEquals("Prepare presentation", results.get(0).title());
        assertEquals("Submit report", results.get(1).title());
    }
}
