# Priority-Sort-Manage

Priority-Sort-Manage is a focused, keyboard-first task manager for the terminal. The goal is to give you a clean Today view, fast filtering, and a distraction-free editing flow whether you are on a Unicode-ready terminal or a plain Windows console.

## Features
- **Today dashboard** with prioritized list, multi-city weather summary, and pagination for long queues.
- **Fast command palette**: single-letter hotkeys for add, edit, done, search, filter, help, and quit.
- **Database-backed or in-memory storage**: connect to MySQL when available or continue offline with seeded demo data.
- **Prompted workflows**: add and edit flows clear the screen, collect inputs step by step, and confirm cancellations instantly.
- **Search and filtering**: free-text search plus filters on priority, due (today/week), and status (open/done).
- **Cross-platform terminal support**: auto-detects UTF-8 support to pick box-drawing or ASCII borders, keeping layouts readable on every console.

## Prerequisites
- Java 21 (managed automatically if you use the Gradle toolchain)
- MySQL 8.x (optional but recommended for persistence)
- Internet access for live weather (optional; the CLI falls back gracefully)

## Getting Started
```bash
# Clone the repository
git clone https://github.com/NOTSMAK/Priority-Sort-Manage.git
cd Priority-Sort-Manage

# Run the interactive CLI
./gradlew run

# Execute the automated tests
./gradlew test
```

## Configuration
Environment variables (or a `.env` file in the project root) control external services. All values are optional.

| Key | Description | Default |
| --- | --- | --- |
| `DB_URL` | JDBC connection string | `jdbc:mysql://localhost:3306/task_manager` |
| `DB_USER` | Database username | `root` |
| `DB_PASS` | Database password | *(blank)* |
| `OPENWEATHER_API_KEY` | OpenWeather API key | *(not set)* |
| `CITY` | Single city or `;`-delimited list (e.g. `Riverside,CA;Los Angeles,CA`) | `Riverside,CA;Los Angeles,CA` |
| `NO_COLOR` | Set to `true` to disable ANSI colors | `false` |

When the MySQL connection is unavailable, the application logs a warning and switches to the in-memory repository seeded with sample data. Weather output falls back from OpenWeather to wttr.in and ultimately to “unavailable”.

## Terminal Commands
- `A` / `add` – Guided task entry (blank input cancels at any step)
- `E` / `edit` – Modify task details by task number
- `D` / `done` – Mark a task complete
- `S` / `search <text>` – Search by title or tags
- `filter priority=P1 status=open due=week` – Combined filters
- `T` / `today` – Return to the Today view
- `?` / `help` – Show in-app command reference
- `Q` / `quit` – Exit the application

## Database Schema
The MySQL backend expects a `tasks` table similar to:
```sql
CREATE TABLE tasks (
    TaskNumber INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    dueDate DATE,
    priority ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'MEDIUM',
    status ENUM('NOT DONE','DONE') NOT NULL DEFAULT 'NOT DONE',
    type VARCHAR(255)
);
```

## Development
- Run `./gradlew run --console=plain` if you prefer Gradle output without progress indicators.
- New features should include corresponding JUnit tests (see `src/test/java`).
- The project targets Java 21; use the provided Gradle wrapper to avoid version drift.

---
Feel free to fork, customize the data layer, or plug the CLI into your own productivity tooling.
