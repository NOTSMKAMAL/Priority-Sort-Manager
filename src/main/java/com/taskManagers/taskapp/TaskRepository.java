package com.taskManagers.taskapp;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskRepository implements TaskStore {

    private static final Logger LOGGER = Logger.getLogger(TaskRepository.class.getName());
    private static final String BASE_SELECT = "SELECT TaskNumber AS id, name AS title, dueDate AS due_date, " +
            "priority, status, type AS tags FROM tasks";

    private final DataSource dataSource;

    public TaskRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<TaskItem> findById(long id) {
        String sql = BASE_SELECT + " WHERE TaskNumber = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Failed to load task {0}", new Object[]{id});
            LOGGER.log(Level.FINE, ex, () -> "Error while loading task " + id);
        }
        return Optional.empty();
    }

    @Override
    public List<TaskItem> findToday(LocalDate today) {
        String sql = BASE_SELECT + " WHERE dueDate = ? OR (priority = ? AND status <> ?)";
        List<TaskItem> items = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, today);
            ps.setString(2, PriorityLevel.P1.dbValue());
            ps.setString(3, TaskStatus.DONE.dbValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Failed to load today''s tasks: {0}", ex.getMessage());
            LOGGER.log(Level.FINE, ex, () -> "Error while loading today's tasks");
        }
        TaskOrdering.sort(items);
        return items;
    }

    @Override
    public List<TaskItem> find(TaskQuery query) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        query.priority().ifPresent(priority -> {
            sql.append(" AND priority = ?");
            params.add(priority.dbValue());
        });

        query.status().ifPresent(status -> {
            sql.append(" AND status = ?");
            params.add(status.dbValue());
        });

        query.search().ifPresent(text -> {
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(type) LIKE ?)");
            String pattern = "%" + text.toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
        });

        if (query.dueToday()) {
            sql.append(" AND dueDate = ?");
            params.add(LocalDate.now());
        }

        if (query.dueThisWeek()) {
            sql.append(" AND dueDate BETWEEN ? AND ?");
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusDays(7);
            params.add(start);
            params.add(end);
        }

        sql.append(" ORDER BY CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, dueDate ASC, TaskNumber ASC");

        List<TaskItem> items = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof LocalDate date) {
                    ps.setObject(i + 1, date);
                } else {
                    ps.setObject(i + 1, param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Failed to execute query: {0}", ex.getMessage());
            LOGGER.log(Level.FINE, ex, () -> "Error executing task query");
        }
        TaskOrdering.sort(items);
        return items;
    }

    @Override
    public TaskItem insert(TaskItem item) throws SQLException {
        String sql = "INSERT INTO tasks (name, dueDate, priority, status, type) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.title());
            ps.setObject(2, item.dueDate());
            ps.setString(3, item.priority().dbValue());
            ps.setString(4, item.status().dbValue());
            ps.setString(5, item.tags());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                long id = item.id();
                if (keys.next()) {
                    id = keys.getLong(1);
                }
                return item.toBuilder().id(id).build();
            }
        }
    }

    @Override
    public boolean update(TaskItem item) throws SQLException {
        String sql = "UPDATE tasks SET name = ?, dueDate = ?, priority = ?, status = ?, type = ? WHERE TaskNumber = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, item.title());
            ps.setObject(2, item.dueDate());
            ps.setString(3, item.priority().dbValue());
            ps.setString(4, item.status().dbValue());
            ps.setString(5, item.tags());
            ps.setLong(6, item.id());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean markDone(long id) throws SQLException {
        String sql = "UPDATE tasks SET status = ? WHERE TaskNumber = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, TaskStatus.DONE.dbValue());
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE TaskNumber = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static TaskItem mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String title = rs.getString("title");
        LocalDate dueDate = rs.getObject("due_date", LocalDate.class);
        LocalTime dueTime = extractLocalTime(rs, "due_time");
        PriorityLevel priority = PriorityLevel.fromDb(rs.getString("priority"));
        TaskStatus status = TaskStatus.fromDb(rs.getString("status"));
        String tags = Optional.ofNullable(rs.getString("tags")).orElse("");

        return TaskItem.builder()
                .id(id)
                .title(title)
                .dueDate(dueDate)
                .dueTime(dueTime)
                .priority(priority)
                .status(status)
                .tags(tags)
                .build();
    }

    private static LocalTime extractLocalTime(ResultSet rs, String column) {
        try {
            Time time = rs.getTime(column);
            if (time != null) {
                return time.toLocalTime();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.FINEST, "Column {0} not available: {1}", new Object[]{column, ex.getMessage()});
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        if (dataSource instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
