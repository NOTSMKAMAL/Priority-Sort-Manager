package com.taskManagers.taskapp;

import java.util.Optional;

public final class TaskQuery {
    private final Optional<String> search;
    private final Optional<PriorityLevel> priority;
    private final Optional<TaskStatus> status;
    private final boolean dueToday;
    private final boolean dueThisWeek;

    private TaskQuery(Builder builder) {
        this.search = Optional.ofNullable(builder.search);
        this.priority = Optional.ofNullable(builder.priority);
        this.status = Optional.ofNullable(builder.status);
        this.dueToday = builder.dueToday;
        this.dueThisWeek = builder.dueThisWeek;
    }

    public Optional<String> search() {
        return search;
    }

    public Optional<PriorityLevel> priority() {
        return priority;
    }

    public Optional<TaskStatus> status() {
        return status;
    }

    public boolean dueToday() {
        return dueToday;
    }

    public boolean dueThisWeek() {
        return dueThisWeek;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String search;
        private PriorityLevel priority;
        private TaskStatus status;
        private boolean dueToday;
        private boolean dueThisWeek;

        private Builder() {
        }

        public Builder search(String search) {
            if (search == null || search.isBlank()) {
                this.search = null;
            } else {
                this.search = search.trim();
            }
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

        public Builder dueToday(boolean dueToday) {
            this.dueToday = dueToday;
            return this;
        }

        public Builder dueThisWeek(boolean dueThisWeek) {
            this.dueThisWeek = dueThisWeek;
            return this;
        }

        public TaskQuery build() {
            if (dueToday && dueThisWeek) {
                throw new IllegalArgumentException("dueToday and dueThisWeek cannot both be true");
            }
            return new TaskQuery(this);
        }
    }
}
