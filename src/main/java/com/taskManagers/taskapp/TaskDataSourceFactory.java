package com.taskManagers.taskapp;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public final class TaskDataSourceFactory {

    private TaskDataSourceFactory() {
    }

    public static DataSource create(TaskAppConfig config) throws SQLException {
        String jdbcUrl = config.dbUrl();
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new SQLException("Database URL is not configured");
        }

        SimpleDataSource dataSource = new SimpleDataSource(jdbcUrl.trim(), config.dbUser(), config.dbPassword());
        try (Connection ignored = dataSource.getConnection()) {
            return dataSource;
        } catch (SQLException ex) {
            throw new SQLException("Unable to connect to database " + maskSensitiveInfo(jdbcUrl), ex);
        }
    }

    static String maskSensitiveInfo(String url) {
        if (url == null) {
            return "";
        }
        String lower = url.toLowerCase(Locale.ROOT);
        int passwordIndex = lower.indexOf("password=");
        if (passwordIndex == -1) {
            return url;
        }
        StringBuilder masked = new StringBuilder(url.length());
        masked.append(url, 0, passwordIndex).append("password=****");
        int separatorIndex = url.indexOf('&', passwordIndex);
        if (separatorIndex != -1) {
            masked.append(url.substring(separatorIndex));
        }
        return masked.toString();
    }

    private static final class SimpleDataSource implements DataSource, AutoCloseable {

        private final String url;
        private final String user;
        private final String password;
        private final Properties properties = new Properties();
        private volatile PrintWriter logWriter;
        private volatile int loginTimeoutSeconds;

        private SimpleDataSource(String url, String user, String password) {
            this.url = Objects.requireNonNull(url, "url");
            this.user = blankToNull(user);
            this.password = blankToNull(password);
            if (this.user != null) {
                properties.setProperty("user", this.user);
            }
            if (this.password != null) {
                properties.setProperty("password", this.password);
            }
        }

        @Override
        public Connection getConnection() throws SQLException {
            if (!properties.isEmpty()) {
                return DriverManager.getConnection(url, properties);
            }
            return DriverManager.getConnection(url);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override
        public PrintWriter getLogWriter() {
            return logWriter;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            this.logWriter = out;
            DriverManager.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            this.loginTimeoutSeconds = seconds;
            DriverManager.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() {
            return loginTimeoutSeconds;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLFeatureNotSupportedException("Not a wrapper for " + iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }

        @Override
        public void close() {
            // no pooled resources to release
        }

        private static String blankToNull(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return value;
        }
    }
}
