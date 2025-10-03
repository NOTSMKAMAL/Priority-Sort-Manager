package com.taskManagers.taskapp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

final class TaskOrdering {

    private TaskOrdering() {
    }

    static void sort(List<TaskItem> items) {
        items.sort(TaskOrdering::compare);
    }

    private static int compare(TaskItem left, TaskItem right) {
        int priorityCompare = Integer.compare(left.priority().weight(), right.priority().weight());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        LocalDate leftDate = left.dueDate();
        LocalDate rightDate = right.dueDate();
        if (leftDate != null && rightDate != null) {
            int dateCompare = leftDate.compareTo(rightDate);
            if (dateCompare != 0) {
                return dateCompare;
            }
        }
        LocalTime leftTime = left.dueTime();
        LocalTime rightTime = right.dueTime();
        if (leftTime != null && rightTime != null) {
            int timeCompare = leftTime.compareTo(rightTime);
            if (timeCompare != 0) {
                return timeCompare;
            }
        }
        return Long.compare(left.id(), right.id());
    }
}
