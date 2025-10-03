package com.taskManagers.taskapp;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskStore extends AutoCloseable {

    Optional<TaskItem> findById(long id);

    List<TaskItem> findToday(LocalDate today);

    List<TaskItem> find(TaskQuery query);

    TaskItem insert(TaskItem item) throws SQLException;

    boolean update(TaskItem item) throws SQLException;

    boolean markDone(long id) throws SQLException;

    boolean delete(long id) throws SQLException;

    @Override
    default void close() throws Exception {
        // no-op by default
    }
}
