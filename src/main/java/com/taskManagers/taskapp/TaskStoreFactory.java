package com.taskManagers.taskapp;

import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TaskStoreFactory {

    private static final Logger LOGGER = Logger.getLogger(TaskStoreFactory.class.getName());

    private TaskStoreFactory() {
    }

    public static TaskStore create(TaskAppConfig config) {
        String jdbcUrl = config.dbUrl();
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            LOGGER.fine("Database URL not configured; using in-memory task repository.");
            return new InMemoryTaskRepository();
        }

        DataSource dataSource = null;
        try {
            dataSource = TaskDataSourceFactory.create(config);
            LOGGER.info(() -> "Connected to database: " + TaskDataSourceFactory.maskSensitiveInfo(jdbcUrl));
            return new TaskRepository(dataSource);
        } catch (Exception ex) {
            if (dataSource instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception closeEx) {
                    LOGGER.log(Level.FINE, "Failed to close datasource after error", closeEx);
                }
            }
            LOGGER.log(Level.WARNING, () -> "Database unavailable; using in-memory task repository (" + ex.getMessage() + ")");
            return new InMemoryTaskRepository();
        }
    }
}

