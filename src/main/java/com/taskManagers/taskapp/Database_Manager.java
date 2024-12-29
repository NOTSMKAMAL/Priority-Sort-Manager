package com.taskManagers.taskapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

//Database_Manager class to connect to the database and read data from the tasks table
public class Database_Manager {


    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/task_manager";
    private static final String DB_USER = "notsmak";
    private static final String DB_PASSWORD = "200505";

    public static void viewDatabase() {
        Connection connection = null;
        Statement statement = null;

        try {
            // Step 1: Establish a connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the database!");

            // Step 2: Create a statement
            statement = connection.createStatement();

            // Step 3: Execute a query to read data
            String query = "SELECT * FROM tasks";
            ResultSet resultSet = statement.executeQuery(query);

            // Step 5: Process the result set
            System.out.println("Task Data:");
            while (resultSet.next()) {
                int taskNumber = resultSet.getInt("TaskNumber");
                String name = resultSet.getString("name");
                String dueDate = resultSet.getDate("dueDate").toString();
                String priority = resultSet.getString("priority");
                String status = resultSet.getString("status");
                String type = resultSet.getString("type");

                System.out.printf("Task: #%d | Name: %s | Due: %s | Priority: %s | Status: %s | Type: %s %n",
                        taskNumber, name, dueDate, priority, status, type);

            }

            // Step 6: Close the result set
            resultSet.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addToDatabase( String name, String dueDate, String priority, String status, String type) {
        Connection connection = null;
        Statement statement = null;

        try {
            // Step 1: Establish a connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the database!");

            // Step 2: Create a statement
            statement = connection.createStatement();

            // Step 3: Execute a query to insert data
            String query = "INSERT INTO tasks (name, dueDate, priority, status, type) VALUES ('" + name + "', '" + dueDate + "', '" + priority + "', '" + status + "', '" + type + "')";
            statement.executeUpdate(query);

            System.out.println("Task added to the database!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean taskFinder(int taskNum) {

    Connection connection = null;
    Statement statement = null;
    boolean exists = false;

    try {
        // Step 1: Establish a connection
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("Connected to the database!");

        // Step 2: Create a statement
        statement = connection.createStatement();

        // Step 3: Execute a query to check if task exists
        String query = "SELECT COUNT(*) FROM tasks WHERE TaskNumber = " + taskNum;
        ResultSet resultSet = statement.executeQuery(query);

        // Step 4: Process the result set
        if (resultSet.next()) {
            exists = resultSet.getInt(1) > 0;
        }

        // Step 5: Close the result set
        resultSet.close();
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        // Close the resources
        try {
            if (statement != null)
                statement.close();
            if (connection != null)
                connection.close();
            System.out.println("Connection closed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    return exists;
}

    public static void viewTask(int taskNum) {
        Connection connection = null;
        Statement statement = null;

        try {
            // Step 1: Establish a connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the database!");

            // Step 2: Create a statement
            statement = connection.createStatement();

            // Step 3: Execute a query to read data
            String query = "SELECT * FROM tasks WHERE TaskNumber = " + taskNum;
            ResultSet resultSet = statement.executeQuery(query);

            // Step 5: Process the result set
            while (resultSet.next()) {
                int taskNumber = resultSet.getInt("TaskNumber");
                String name = resultSet.getString("name");
                String dueDate = resultSet.getDate("dueDate").toString();
                String priority = resultSet.getString("priority");
                String status = resultSet.getString("status");
                String type = resultSet.getString("type");

                System.out.printf("Task: #%d | Name: %s | Due: %s | Priority: %s | Status: %s | Type: %s %n",
                        taskNumber, name, dueDate, priority, status, type);
            }

            // Step 6: Close the result set
            resultSet.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteFromDatabase(int taskNum) {
        Connection connection = null;
        Statement statement = null;

        try {
            // Step 1: Establish a connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the database!");

            // Step 2: Create a statement
            statement = connection.createStatement();

            // Step 3: Execute a query to delete data
            String query = "DELETE FROM tasks WHERE TaskNumber = " + taskNum;
            statement.executeUpdate(query);

            System.out.println("Task deleted from the database!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the resources
            try {
                if (statement != null)
                    statement.close();
                if (connection != null)
                    connection.close();
                System.out.println("Connection closed.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static void markAsDone(int taskNum) {
        Connection connection = null;
        Statement statement = null;

        try {
            // Step 1: Establish a connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the database!");

            // Step 2: Create a statement
            statement = connection.createStatement();

            // Step 3: Execute a query to update data
            String query = "UPDATE tasks SET status = 'DONE' WHERE TaskNumber = " + taskNum;
            statement.executeUpdate(query);

            System.out.println("Task marked as done in the database!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the resources
            try {
                if (statement != null)
                    statement.close();
                if (connection != null)
                    connection.close();
                System.out.println("Connection closed.");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    public static void viewTodaysTasks(String date) {
        Connection connection = null;
        Statement statement = null;

        try {
            // Step 1: Establish a connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the database!");

            // Step 2: Create a statement
            statement = connection.createStatement();

            // Step 3: Execute a query to read data
            String query = "SELECT * FROM tasks WHERE dueDate = '" + date + "'";
            ResultSet resultSet = statement.executeQuery(query);

            // Step 5: Process the result set
            System.out.println("Today's Tasks:");
            while (resultSet.next()) {
                int taskNumber = resultSet.getInt("TaskNumber");
                String name = resultSet.getString("name");
                String dueDate = resultSet.getDate("dueDate").toString();
                String priority = resultSet.getString("priority");
                String status = resultSet.getString("status");
                String type = resultSet.getString("type");

                System.out.printf("Task #%d: | Name: %s | Due: %s | Priority: %s | Status: %s | Type: %s%n",
                        taskNumber, name, dueDate, priority, status, type);
            }

            // Step 6: Close the result set
            resultSet.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
}   
public void data(){
    
}
}
