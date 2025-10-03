package com.taskManagers.taskapp;

import java.io.BufferedReader;
import java.io.Console;
import java.nio.charset.Charset;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Task {

    private static final Logger LOGGER = Logger.getLogger(Task.class.getName());
    private static final int PAGE_SIZE = 8;
    private static final int MIN_BOX_WIDTH = 120;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final TaskAppConfig config;
    private final TaskStore taskStore;
    private final WeatherService weatherService;
    private final boolean colorEnabled;
    private final BufferedReader reader;
    private final boolean unicodeBorders;
    private final String borderVertical;
    private final String borderTopLeft;
    private final String borderTopRight;
    private final String borderBottomLeft;
    private final String borderBottomRight;
    private final String borderSeparatorLeft;
    private final String borderSeparatorRight;
    private final char borderHorizontal;
    private final String topPadding;
    private final CommandParser parser = new CommandParser();

    private boolean running;
    private ViewMode currentView = ViewMode.TODAY;
    private List<TaskItem> currentItems = List.of();
    private int currentPage = 0;
    private String currentHeading = "Today";
    private String lastSearchText = "";
    private TaskQuery lastFilterQuery;
    private int boxWidth = MIN_BOX_WIDTH;

    public Task(TaskAppConfig config, TaskStore taskStore, WeatherService weatherService) {
        this.config = config;
        this.taskStore = taskStore;
        this.weatherService = weatherService;
        this.colorEnabled = config.colorEnabled();
        this.unicodeBorders = detectUnicodeBorderSupport();
        if (unicodeBorders) {
            this.borderVertical = Character.toString((char) 0x2502);
            this.borderTopLeft = Character.toString((char) 0x250C);
            this.borderTopRight = Character.toString((char) 0x2510);
            this.borderBottomLeft = Character.toString((char) 0x2514);
            this.borderBottomRight = Character.toString((char) 0x2518);
            this.borderSeparatorLeft = Character.toString((char) 0x251C);
            this.borderSeparatorRight = Character.toString((char) 0x2524);
            this.borderHorizontal = (char) 0x2500;
            String horizontal = Character.toString((char) 0x2500);
            this.topPadding = horizontal + horizontal;
        } else {
            this.borderVertical = "|";
            this.borderTopLeft = "+";
            this.borderTopRight = "+";
            this.borderBottomLeft = "+";
            this.borderBottomRight = "+";
            this.borderSeparatorLeft = "+";
            this.borderSeparatorRight = "+";
            this.borderHorizontal = '-';
            this.topPadding = "--";
        }
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void run() {
        running = true;
        renderToday();
        while (running) {
            Command command = nextCommand();
            handleCommand(command);
        }
    }

    private Command nextCommand() {
        System.out.print(AnsiColors.format(AnsiColors.SUBTLE, "command> ", colorEnabled));
        System.out.flush();
        try {
            String line = readLineWithFallback();
            if (line == null) {
                running = false;
                return new Command(CommandType.QUIT);
            }
            return parser.parse(line);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to read input", ex);
            running = false;
            return new Command(CommandType.QUIT);
        }
    }

    private String readLineWithFallback() throws IOException {
        Console console = System.console();
        if (console != null) {
            return console.readLine();
        }
        return reader.readLine();
    }

    private boolean detectUnicodeBorderSupport() {
        Console console = System.console();
        if (console != null) {
            try {
                String name = console.charset().name();
                if (name != null && name.toLowerCase(Locale.US).contains("utf")) {
                    return true;
                }
            } catch (Exception ignore) {
                // ignore and fallback to other heuristics
            }
        }
        String stdoutEncoding = System.getProperty("sun.stdout.encoding");
        if (stdoutEncoding != null && stdoutEncoding.toLowerCase(Locale.US).contains("utf")) {
            return true;
        }
        return Charset.defaultCharset().name().toLowerCase(Locale.US).contains("utf");
    }

    private void handleCommand(Command command) {
        switch (command.type()) {
            case ADD -> handleAdd();
            case EDIT -> handleEdit();
            case DONE -> handleDone();
            case SEARCH -> handleSearch(command);
            case FILTER -> handleFilter(command);
            case TODAY -> renderToday();
            case HELP -> showHelp();
            case QUIT -> exit();
            case NEXT_PAGE -> nextPage();
            case PREVIOUS_PAGE -> previousPage();
            case BACK -> renderCurrentView();
            case UNKNOWN -> {
                showInfo("Unknown command: " + command.text());
                printFooter();
            }
        }
    }

    private void renderToday() {
        currentView = ViewMode.TODAY;
        currentHeading = "Today";
        currentPage = 0;
        currentItems = taskStore.findToday(LocalDate.now());
        renderCurrentView();
    }

    private void renderCurrentView() {
        clearScreen();
        switch (currentView) {
            case TODAY -> renderTodayPage();
            default -> renderListPage();
        }
    }

    private void renderTodayPage() {
        String dateText = LocalDate.now().format(DATE_FORMAT);
        String weather = weatherService.fetch();
        String dateLine = String.format("Date: %s  | %s", dateText, weather);
        String headingLine = "Priorities Due Today";

        List<TaskItem> pageItems = pageItems(currentItems);
        List<String> taskLines = new ArrayList<>();
        if (pageItems.isEmpty()) {
            taskLines.add("No priorities for today. Enjoy the focus!");
        } else {
            for (int i = 0; i < pageItems.size(); i++) {
                TaskItem item = pageItems.get(i);
                taskLines.add(formatTaskLine(item, currentPage * PAGE_SIZE + i + 1));
            }
        }

        adjustTodayBoxWidth(dateLine, headingLine, taskLines);

        printBoxTop("Today");
        printBoxLine(dateLine);
        printBoxSeparator();
        printBoxLine(headingLine);
        for (String line : taskLines) {
            printBoxLine(line);
        }
        printBoxBottom();
        printPagination(currentItems.size());
        printFooter();
    }

    private void renderListPage() {
        printHeader(currentHeading);
        List<TaskItem> pageItems = pageItems(currentItems);
        if (pageItems.isEmpty()) {
            System.out.println("No tasks match your criteria.");
        } else {
            for (int i = 0; i < pageItems.size(); i++) {
                TaskItem item = pageItems.get(i);
                String text = formatTaskLine(item, currentPage * PAGE_SIZE + i + 1);
                System.out.println(text);
            }
        }
        printPagination(currentItems.size());
        printFooter();
    }

    private void nextPage() {
        if (currentItems.size() <= PAGE_SIZE) {
            return;
        }
        int totalPages = totalPages(currentItems.size());
        if (currentPage + 1 >= totalPages) {
            return;
        }
        currentPage++;
        renderCurrentView();
    }

    private void previousPage() {
        if (currentPage == 0) {
            return;
        }
        currentPage--;
        renderCurrentView();
    }

    private void showHelp() {
        clearScreen();
        printHeader("Help");
        System.out.println("A / add           Add a new task");
        System.out.println("E / edit          Edit a task by its number");
        System.out.println("D / done          Mark a task as done");
        System.out.println("S / search        Search tasks, e.g. search report");
        System.out.println("filter ...        Apply filters, e.g. filter priority=P1 due=week");
        System.out.println("T / today         Return to the Today screen");
        System.out.println("? / help          Show this help");
        System.out.println("Q / quit          Exit the application");
        System.out.println();
        System.out.println("Filters: priority=P1|P2|P3  status=open|done  due=today|week");
        System.out.println("Search: matches title and tags");
        printFooter();
    }

    private void exit() {
        showInfo("Goodbye!");
        running = false;
    }

    private void handleAdd() {
        clearScreen();
        printHeader("Add Task");
        System.out.println("Leave blank at any prompt to cancel.");
        System.out.println();

        String title = prompt("Title: ", true);
        if (title == null || title.isBlank()) {
            showInfo("Add cancelled.");
            sleep(400);
            renderCurrentView();
            return;
        }

        System.out.println();
        LocalDate dueDate = promptDate("Due date (yyyy-MM-dd, blank=today): ", LocalDate.now());

        System.out.println();
        LocalTime dueTime = promptTime("Due time (HH:mm, optional): ");

        System.out.println();
        PriorityLevel priority = promptPriority("Priority [P1/P2/P3] (default P2): ", PriorityLevel.P2);

        System.out.println();
        String tags = prompt("Tags (comma separated, optional): ", true);
        if (tags == null) {
            tags = "";
        }

        TaskItem draft = TaskItem.builder()
                .title(title)
                .dueDate(dueDate)
                .dueTime(dueTime)
                .priority(priority)
                .status(TaskStatus.OPEN)
                .tags(tags)
                .build();

        TaskItem saved = executeWithRetry("Save task", () -> taskStore.insert(draft));
        if (saved != null) {
            showSuccess("Saved.");
            sleep(400);
            renderToday();
        } else {
            renderCurrentView();
        }
    }

    private void handleEdit() {
        Long id = promptForId("Task number to edit (blank cancels): ");
        if (id == null) {
            showInfo("Edit cancelled.");
            renderCurrentView();
            return;
        }
        Optional<TaskItem> existing = taskStore.findById(id);
        if (existing.isEmpty()) {
            showError("Task #" + id + " not found.");
            renderCurrentView();
            return;
        }
        TaskItem item = existing.get();
        showInfo("Editing #" + id + " (" + item.title() + ")");
        String title = promptWithDefault("Title", item.title());
        LocalDate dueDate = promptDateWithDefault("Due date (yyyy-MM-dd)", item.dueDate());
        LocalTime dueTime = promptTimeWithDefault("Due time (HH:mm)", item.dueTime());
        PriorityLevel priority = promptPriority("Priority [P1/P2/P3]", item.priority());
        TaskStatus status = promptStatus("Status [open/done]", item.status());
        String tags = promptWithDefault("Tags", item.tags());

        TaskItem updated = item.toBuilder()
                .title(title)
                .dueDate(dueDate)
                .dueTime(dueTime)
                .priority(priority)
                .status(status)
                .tags(tags)
                .build();

        Boolean success = executeWithRetry("Update task", () -> taskStore.update(updated));
        if (Boolean.TRUE.equals(success)) {
            showSuccess("Saved.");
            refreshCurrentView();
        } else {
            renderCurrentView();
        }
    }

    private void handleDone() {
        Long id = promptForId("Task number to mark done (blank cancels): ");
        if (id == null) {
            renderCurrentView();
            return;
        }
        if (!confirm("Mark task #" + id + " as done? (y/N): ")) {
            renderCurrentView();
            return;
        }
        Boolean success = executeWithRetry("Mark done", () -> taskStore.markDone(id));
        if (Boolean.TRUE.equals(success)) {
            showSuccess("Saved.");
            refreshCurrentView();
        } else {
            renderCurrentView();
        }
    }

    private void handleSearch(Command command) {
        String text = command.text();
        if (text == null || text.isBlank()) {
            text = prompt("Search text (blank cancels): ", true);
            if (text == null || text.isBlank()) {
                renderCurrentView();
                return;
            }
        }
        text = text.trim();
        lastSearchText = text;
        TaskQuery query = TaskQuery.builder().search(text).build();
        currentItems = taskStore.find(query);
        currentView = ViewMode.SEARCH_RESULTS;
        currentHeading = "Search: \"" + text + "\"";
        currentPage = 0;
        renderCurrentView();
    }

    private void handleFilter(Command command) {
        Map<String, String> options = command.options();
        if (options.isEmpty()) {
            showInfo("Usage: filter priority=P1 status=open due=today|week");
            return;
        }
        TaskQuery.Builder builder = TaskQuery.builder();
        StringBuilder heading = new StringBuilder("Filter: ");
        options.forEach((key, value) -> {
            if ("priority".equals(key)) {
                PriorityLevel level = PriorityLevel.fromDb(value);
                builder.priority(level);
                heading.append("priority=").append(level.name()).append(' ');
            } else if ("status".equals(key)) {
                TaskStatus status = parseStatus(value);
                if (status != null) {
                    builder.status(status);
                    heading.append("status=").append(status.name()).append(' ');
                }
            } else if ("due".equals(key)) {
                String normalized = value.toLowerCase(Locale.US);
                if ("today".equals(normalized)) {
                    builder.dueToday(true);
                    heading.append("due=today ");
                } else if ("week".equals(normalized) || "thisweek".equals(normalized)) {
                    builder.dueThisWeek(true);
                    heading.append("due=week ");
                }
            }
        });
        TaskQuery query;
        try {
            query = builder.build();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        currentItems = taskStore.find(query);
        currentView = ViewMode.FILTER_RESULTS;
        lastFilterQuery = query;
        currentPage = 0;
        currentHeading = heading.toString().trim();
        renderCurrentView();
    }

    private void refreshCurrentView() {
        switch (currentView) {
            case TODAY -> currentItems = taskStore.findToday(LocalDate.now());
            case SEARCH_RESULTS -> {
                if (lastSearchText != null && !lastSearchText.isBlank()) {
                    currentItems = taskStore.find(TaskQuery.builder().search(lastSearchText).build());
                }
            }
            case FILTER_RESULTS -> {
                if (lastFilterQuery != null) {
                    currentItems = taskStore.find(lastFilterQuery);
                }
            }
            default -> {
                // leave as is
            }
        }
        normalizePage();
        renderCurrentView();
    }

    private void normalizePage() {
        if (currentItems.isEmpty()) {
            currentPage = 0;
            return;
        }
        int totalPages = totalPages(currentItems.size());
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }
    }

    private List<TaskItem> pageItems(List<TaskItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        int start = currentPage * PAGE_SIZE;
        if (start >= items.size()) {
            start = 0;
            currentPage = 0;
        }
        int end = Math.min(start + PAGE_SIZE, items.size());
        return items.subList(start, end);
    }

    private int totalPages(int total) {
        return (int) Math.ceil((double) total / PAGE_SIZE);
    }

    private String formatTaskLine(TaskItem item, int index) {
        String priority = item.priority().name();
        String status = item.status() == TaskStatus.DONE ? "[DONE]" : "[OPEN]";
        String due;
        if (item.dueTime() != null) {
            due = "due " + item.dueTime().format(TIME_FORMAT);
        } else if (item.dueDate() != null) {
            due = "due " + item.dueDate().format(DATE_FORMAT);
        } else {
            due = "no due date";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(index).append(") [").append(priority).append("] ");
        String title = item.title();
        if (title != null && !title.isBlank()) {
            builder.append(title.trim());
        } else {
            builder.append("(untitled)");
        }
        builder.append("  ").append(due).append("  ").append(status);

        if (item.tags() != null && !item.tags().isBlank()) {
            builder.append("  #").append(item.tags().trim());
        }

        return builder.toString();
    }

    private void adjustTodayBoxWidth(String dateLine, String headingLine, List<String> taskLines) {
        int interiorMax = Math.max(dateLine.length(), headingLine.length());
        for (String line : taskLines) {
            if (line.length() > interiorMax) {
                interiorMax = line.length();
            }
        }
        boxWidth = Math.max(MIN_BOX_WIDTH, interiorMax + 4);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        if (max <= 3) {
            return value.substring(0, max);
        }
        return value.substring(0, max - 3) + "...";
    }

    private void printBoxTop(String title) {
        String label = " " + title + " ";
        int available = boxWidth - topPadding.length() - 2;
        if (label.length() > available && available > 2) {
            label = " " + truncate(title, available - 1) + " ";
        }
        int dashCount = available - label.length();
        if (dashCount < 0) {
            dashCount = 0;
        }
        System.out.println(borderTopLeft + topPadding + label + repeat(borderHorizontal, dashCount) + borderTopRight);
    }

    private void printBoxSeparator() {
        System.out.println(borderSeparatorLeft + repeat(borderHorizontal, boxWidth - 2) + borderSeparatorRight);
    }

    private void printBoxBottom() {
        System.out.println(borderBottomLeft + repeat(borderHorizontal, boxWidth - 2) + borderBottomRight);
    }

    private void printBoxLine(String content) {
        int interiorWidth = boxWidth - 4;
        String truncated = truncate(content, interiorWidth);
        String padded = String.format(Locale.US, "%-" + interiorWidth + "s", truncated);
        System.out.println(borderVertical + " " + padded + " " + borderVertical);
    }

    private String repeat(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        return String.valueOf(ch).repeat(count);
    }

    private void printHeader(String title) {
        String text = "== " + title + " ==";
        System.out.println(AnsiColors.format(AnsiColors.HEADER, text, colorEnabled));
        System.out.println();
    }

    private void printPagination(int total) {
        if (total <= PAGE_SIZE) {
            return;
        }
        int totalPages = totalPages(total);
        String text = String.format(Locale.US, "Page %d/%d   (N next, P previous)", currentPage + 1, totalPages);
        System.out.println(AnsiColors.format(AnsiColors.SUBTLE, text, colorEnabled));
    }

    private void printFooter() {
        System.out.println();
        String footer = "[A]dd  [E]dit  [D]one  [S]earch  [T]oday  [?]Help  [Q]uit";
        System.out.println(AnsiColors.format(AnsiColors.SUBTLE, footer, colorEnabled));
        System.out.println(AnsiColors.format(AnsiColors.SUBTLE,
                "Commands: search <text> | filter priority=P1 status=open due=week",
                colorEnabled));
    }

    private void showInfo(String message) {
        System.out.println(AnsiColors.format(AnsiColors.SUBTLE, message, colorEnabled));
    }

    private void showSuccess(String message) {
        System.out.println(AnsiColors.format(AnsiColors.SUCCESS, message, colorEnabled));
    }

    private void showError(String message) {
        System.out.println(AnsiColors.format(AnsiColors.ERROR, message, colorEnabled));
    }

    private String prompt(String prompt, boolean allowBlank) {
        while (true) {
            System.out.print(prompt);
            System.out.flush();
            try {
                String line = readLineWithFallback();
                if (line == null) {
                    running = false;
                    return null;
                }
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
                if (allowBlank) {
                    return "";
                }
                System.out.println("Please enter a value.");
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Input error", ex);
                return null;
            }
        }
    }

    private String promptWithDefault(String label, String current) {
        String prompt = String.format(Locale.US, "%s [%s]: ", label, current == null ? "" : current);
        String response = prompt(prompt, true);
        if (response == null || response.isBlank()) {
            return current == null ? "" : current;
        }
        return response;
    }

    private LocalDate promptDate(String label, LocalDate defaultValue) {
        while (true) {
            String response = prompt(label, true);
            if (response == null) {
                return defaultValue;
            }
            if (response.isBlank()) {
                return defaultValue;
            }
            try {
                return LocalDate.parse(response, DATE_FORMAT);
            } catch (DateTimeParseException ex) {
                showError("Invalid date. Use yyyy-MM-dd.");
            }
        }
    }

    private LocalDate promptDateWithDefault(String label, LocalDate current) {
        LocalDate fallback = current == null ? LocalDate.now() : current;
        while (true) {
            String response = prompt(String.format(Locale.US, "%s [%s]: ", label,
                    current == null ? "" : current.format(DATE_FORMAT)), true);
            if (response == null) {
                return fallback;
            }
            if (response.isBlank()) {
                return fallback;
            }
            try {
                return LocalDate.parse(response, DATE_FORMAT);
            } catch (DateTimeParseException ex) {
                showError("Invalid date. Use yyyy-MM-dd.");
            }
        }
    }

    private LocalTime promptTime(String label) {
        while (true) {
            String response = prompt(label, true);
            if (response == null || response.isBlank()) {
                return null;
            }
            try {
                return LocalTime.parse(response, TIME_FORMAT);
            } catch (DateTimeParseException ex) {
                showError("Invalid time. Use HH:mm.");
            }
        }
    }

    private LocalTime promptTimeWithDefault(String label, LocalTime current) {
        while (true) {
            String response = prompt(String.format(Locale.US, "%s [%s]: ", label,
                    current == null ? "" : current.format(TIME_FORMAT)), true);
            if (response == null) {
                return current;
            }
            if (response.isBlank()) {
                return current;
            }
            try {
                return LocalTime.parse(response, TIME_FORMAT);
            } catch (DateTimeParseException ex) {
                showError("Invalid time. Use HH:mm.");
            }
        }
    }

    private PriorityLevel promptPriority(String label, PriorityLevel defaultValue) {
        while (true) {
            String response = prompt(label + " ", true);
            if (response == null || response.isBlank()) {
                return defaultValue;
            }
            PriorityLevel parsed = parsePriority(response);
            if (parsed != null) {
                return parsed;
            }
            showError("Enter P1, P2, or P3.");
        }
    }

    private PriorityLevel parsePriority(String value) {
        String normalized = value.trim().toUpperCase(Locale.US);
        return switch (normalized) {
            case "P1", "1", "HIGH" -> PriorityLevel.P1;
            case "P2", "2", "MEDIUM" -> PriorityLevel.P2;
            case "P3", "3", "LOW" -> PriorityLevel.P3;
            default -> null;
        };
    }

    private TaskStatus promptStatus(String label, TaskStatus current) {
        while (true) {
            String response = prompt(String.format(Locale.US, "%s [%s]: ", label, current.name().toLowerCase(Locale.US)), true);
            if (response == null || response.isBlank()) {
                return current;
            }
            TaskStatus status = parseStatus(response);
            if (status != null) {
                return status;
            }
            showError("Enter open or done.");
        }
    }

    private TaskStatus parseStatus(String value) {
        String normalized = value.trim().toLowerCase(Locale.US);
        return switch (normalized) {
            case "open", "todo", "notdone", "pending" -> TaskStatus.OPEN;
            case "done", "complete", "completed" -> TaskStatus.DONE;
            default -> null;
        };
    }

    private Long promptForId(String prompt) {
        while (true) {
            String response = prompt(prompt, true);
            if (response == null || response.isBlank()) {
                return null;
            }
            try {
                long value = Long.parseLong(response);
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ex) {
                showError("Please enter a valid number.");
                continue;
            }
            showError("Please enter a positive number.");
        }
    }

    private boolean confirm(String prompt) {
        String response = prompt(prompt, true);
        return response != null && response.equalsIgnoreCase("y");
    }

    private <T> T executeWithRetry(String label, SqlCallable<T> callable) {
        SQLException failure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return callable.call();
            } catch (SQLException ex) {
                failure = ex;
                LOGGER.log(Level.WARNING, label + " attempt " + attempt + " failed: " + ex.getMessage());
                sleep(attempt * 150L);
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, label + " failed unexpectedly", ex);
                showError(label + " failed: " + ex.getMessage());
                return null;
            }
        }
        if (failure != null) {
            showError(label + " failed: " + failure.getMessage());
        }
        return null;
    }
    private void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void clearScreen() {
        System.out.print("\u001B[H\u001B[2J");
        System.out.flush();
    }

    @FunctionalInterface
    private interface SqlCallable<T> {
        T call() throws SQLException;
    }
}
















