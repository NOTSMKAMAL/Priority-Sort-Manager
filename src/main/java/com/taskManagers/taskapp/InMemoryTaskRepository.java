package com.taskManagers.taskapp;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryTaskRepository implements TaskStore {

    private final Map<Long, TaskItem> tasks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    public InMemoryTaskRepository() {
        seedDefaults();
    }

    @Override
    public Optional<TaskItem> findById(long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<TaskItem> findToday(LocalDate today) {
        List<TaskItem> results = tasks.values().stream()
                .filter(item -> (item.dueDate() != null && item.dueDate().isEqual(today))
                        || (item.priority() == PriorityLevel.P1 && item.status() != TaskStatus.DONE))
                .collect(Collectors.toCollection(ArrayList::new));
        TaskOrdering.sort(results);
        return results;
    }

    @Override
    public List<TaskItem> find(TaskQuery query) {
        LocalDate today = LocalDate.now();
        List<TaskItem> results = tasks.values().stream()
                .filter(item -> query.priority().map(item.priority()::equals).orElse(true))
                .filter(item -> query.status().map(item.status()::equals).orElse(true))
                .filter(item -> matchesSearch(item, query.search()))
                .filter(item -> !query.dueToday() || (item.dueDate() != null && item.dueDate().isEqual(today)))
                .filter(item -> !query.dueThisWeek() || isDueThisWeek(item, today))
                .collect(Collectors.toCollection(ArrayList::new));
        TaskOrdering.sort(results);
        return results;
    }

    @Override
    public TaskItem insert(TaskItem item) throws SQLException {
        long id = nextId();
        TaskItem saved = item.toBuilder().id(id).build();
        tasks.put(id, saved);
        return saved;
    }

    @Override
    public boolean update(TaskItem item) throws SQLException {
        if (!tasks.containsKey(item.id())) {
            return false;
        }
        tasks.put(item.id(), item);
        return true;
    }

    @Override
    public boolean markDone(long id) throws SQLException {
        return findById(id)
                .map(task -> task.toBuilder().status(TaskStatus.DONE).build())
                .map(updated -> {
                    tasks.put(id, updated);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public boolean delete(long id) throws SQLException {
        return tasks.remove(id) != null;
    }

    private boolean matchesSearch(TaskItem item, Optional<String> search) {
        if (search.isEmpty()) {
            return true;
        }
        String text = search.get().toLowerCase(Locale.US);
        return (item.title() != null && item.title().toLowerCase(Locale.US).contains(text))
                || (item.tags() != null && item.tags().toLowerCase(Locale.US).contains(text));
    }

    private boolean isDueThisWeek(TaskItem item, LocalDate today) {
        if (item.dueDate() == null) {
            return false;
        }
        LocalDate end = today.plusDays(7);
        return !item.dueDate().isBefore(today) && !item.dueDate().isAfter(end);
    }

    private long nextId() {
        return sequence.incrementAndGet();
    }

    private void seedDefaults() {
        LocalDate today = LocalDate.now();
        save(TaskItem.builder()
                .title("Submit quarterly compliance report")
                .priority(PriorityLevel.P1)
                .dueDate(today)
                .dueTime(LocalTime.of(14, 0))
                .status(TaskStatus.OPEN)
                .tags("finance,urgent")
                .build());

        save(TaskItem.builder()
                .title("Call supplier about delayed shipment")
                .priority(PriorityLevel.P1)
                .dueDate(today.plusDays(1))
                .dueTime(LocalTime.of(9, 30))
                .status(TaskStatus.OPEN)
                .tags("operations")
                .build());

        save(TaskItem.builder()
                .title("Plan team retrospective agenda")
                .priority(PriorityLevel.P2)
                .dueDate(today.plusDays(2))
                .status(TaskStatus.OPEN)
                .tags("team,planning")
                .build());

        save(TaskItem.builder()
                .title("Archive completed design specs")
                .priority(PriorityLevel.P3)
                .dueDate(today.minusDays(1))
                .status(TaskStatus.DONE)
                .tags("cleanup")
                .build());

        save(TaskItem.builder()
                .title("Research automation tools for QA")
                .priority(PriorityLevel.P2)
                .status(TaskStatus.OPEN)
                .tags("research,long-term")
                .build());
    }

    private void save(TaskItem item) {
        long id = nextId();
        tasks.put(id, item.toBuilder().id(id).build());
    }
}
