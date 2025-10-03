package com.taskManagers.taskapp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public final class TaskItem {
    private final long id;
    private final String title;
    private final LocalDate dueDate;
    private final LocalTime dueTime;
    private final PriorityLevel priority;
    private final TaskStatus status;
    private final String tags;

    private TaskItem(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.dueDate = builder.dueDate;
        this.dueTime = builder.dueTime;
        this.priority = builder.priority;
        this.status = builder.status;
        this.tags = builder.tags;
    }

    public long id() {
        return id;
    }

    public String title() {
        return title;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public LocalTime dueTime() {
        return dueTime;
    }

    public PriorityLevel priority() {
        return priority;
    }

    public TaskStatus status() {
        return status;
    }

    public String tags() {
        return tags;
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .title(title)
                .dueDate(dueDate)
                .dueTime(dueTime)
                .priority(priority)
                .status(status)
                .tags(tags);
    }

    @Override
    public String toString() {
        return "TaskItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", dueDate=" + dueDate +
                ", dueTime=" + dueTime +
                ", priority=" + priority +
                ", status=" + status +
                ", tags='" + tags + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaskItem taskItem = (TaskItem) o;
        return id == taskItem.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private String title;
        private LocalDate dueDate;
        private LocalTime dueTime;
        private PriorityLevel priority = PriorityLevel.P3;
        private TaskStatus status = TaskStatus.OPEN;
        private String tags = "";

        private Builder() {
        }

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder dueTime(LocalTime dueTime) {
            this.dueTime = dueTime;
            return this;
        }

        public Builder priority(PriorityLevel priority) {
            this.priority = priority;
            return this;
        }

        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        public Builder tags(String tags) {
            this.tags = tags;
            return this;
        }

        public TaskItem build() {
            return new TaskItem(this);
        }
    }
}
