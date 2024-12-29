package com.taskManagers.taskapp;
// Define the Task class

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Scanner;

//import org.junit.platform.reporting.shadow.org.opentest4j.reporting.events.core.Data;

public class Task {
    Scanner scanner = new Scanner(System.in);
    public void displayOptions() {
        
        clearScreen();
        // To read user input
        System.out.println("Welcome to Task Manager!");
        System.out.println("Options: ");
        System.out.println("1. See today's priorities");
        System.out.println("2. See all priorities in order"); // Alert user for tasks that are due today
        System.out.println("3. Update priorities"); // Change status of task
        System.out.println("4. Search task by TaskNumber:");
        System.out.println("5. Show data or progress of tasks completed");
        System.out.println("6. Exit");
        
        System.out.print("Enter your choice: ");
        int option = 0;
       
        while(option != 6) {
            while (true) { // Loop until valid input is provided
                System.out.print("Enter a number between 1 and 7: ");

                
                if (scanner.hasNextInt()) { // Check if input is an integer
                    option = scanner.nextInt();
                    if (option >= 1 && option <= 7) { // Check if it's in the valid range
                        break; // Exit loop if valid
                    } else {
                        System.out.println("Invalid option. Please enter a number between 1 and 7.");
                    }
                } else {
                    System.out.println("Invalid input. Please enter an integer.");
                    scanner.nextInt(); // Consume the invalid input
                }
            }
            if (option == 1) {
                clearScreen();
                System.out.println("Today's priorities: ");
                String date = TodaysDate();
                displayTodaysPriority(date);
                int enter = 0;
                while(enter != 1){
                    System.out.println("Press 1 to go back to main menu");
                    enter = scanner.nextInt();
                }
            
            }
            if(option == 2) {
                clearScreen();
                System.out.println("All priorities list:");
                displayAllTasks();
                int enter = 0;
                while(enter != 1){
                    System.out.println("Press 1 to go back to main menu");
                    enter = scanner.nextInt();
                }
                
            }
            if(option == 3) {  
                clearScreen(); 
                System.out.println("Edit Task Manager");
                editTask();
                int enter = 0;
                while(enter != 1){
                    System.out.println("Press 1 to go back to main menu");
                    enter = scanner.nextInt();
                }
                
            }
            if(option == 4) {
                clearScreen();
                System.out.println("Search specific task by Number");
                System.out.println("Enter the task number you want to search:");
                int taskNumber = 0;
                while (true) {
                    if (scanner.hasNextInt()) { // Check if the input is an integer
                        taskNumber = scanner.nextInt();
                        if (taskNumber > 0) { // Validate range
                            break; // Exit the loop if valid
                        } else {
                            System.out.print("Invalid option. Please enter a valid Number: ");
                        }
                    } else {
                        System.out.print("Invalid input. Please enter a valid Number: ");
                        scanner.next(); // Discard invalid input
                    }
        
                }
                searchTask(taskNumber);
                int enter = 0;
                while(enter != 1){
                    System.out.println("Press 1 to go back to main menu");
                    enter = scanner.nextInt();
                }
                
            }
            if(option == 5) {
                clearScreen();
                System.out.println("IN PROGRESS..........");
                // showProgress();
                
            }
            if(option == 6) {
                System.out.println("Exiting Task Manager. Goodbye!");
                break;
            }
            option = 0;
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println("Welcome to Task Manager!");
            System.out.println("Options: ");
            System.out.println("1. See today's priorities");
            System.out.println("2. See all priorities in order"); // Alert user for tasks that are due today
            System.out.println("3. Update priorities"); // Change status of task
            System.out.println("4. Search task by TaskNumber:");
            System.out.println("5. Show data or progress of tasks completed");
            System.out.println("6. Exit");
            
        }
        scanner.close(); // Close the scanner

    }

    // Method to display all tasks

    public void displayAllTasks() {
        
        Database_Manager.viewDatabase();
    }

    // Method to display today's schedule with priority
    public void displayTodaysPriority(String date) {
        Database_Manager.viewTodaysTasks(date);
    }

    public void editTask() {
        System.out.println("Options: ");
        System.out.println("1. Add a task");
        System.out.println("2. Update a task");
        System.out.println("3. Delete a task");
        System.out.println("4. Mark a task as done");
        System.out.println("5. Exit");
        System.out.print("Enter your choice: ");
        
        int option = 0;
        while (true) {
            if (scanner.hasNextInt()) { // Check if the input is an integer
                option = scanner.nextInt();
                if (option > 0 && option < 6) { // Validate range
                    break; // Exit the loop if valid
                } else {
                    System.out.print("Invalid option. Please enter a number between 1 and 5: ");
                }
            } else {
                System.out.print("Invalid input. Please enter a number between 1 and 5: ");
                scanner.next(); // Discard invalid input
            }
        }
        if(option == 1){
           
            System.out.println("Enter the name of the task:");
            String name = scanner.next();
            scanner.nextLine();
            System.out.println("Enter the due date of the task Format (YYYY-MM-DD):");
            String dueDate = scanner.next();
            scanner.nextLine();
            System.out.println("Enter the priority of the task:");
            System.out.println("Choose from the following options:");
            System.out.println("1.LOW");
            System.out.println("2.MEDIUM");
            System.out.println("3.HIGH");
            int choice = 0;
            while (true) {
                if (scanner.hasNextInt()) { // Check if the input is an integer
                    choice = scanner.nextInt();
                    if (choice > 0 && choice < 4) { // Validate range
                        break; // Exit the loop if valid
                    } else {
                        System.out.print("Invalid option. Please enter a number between 1 and 3: ");
                    }
                } else {
                    System.out.print("Invalid input. Please enter a number between 1 and 3: ");
                    scanner.next(); // Discard invalid input
                }
            }
            String priority = "";
            if(choice == 1){
                priority = "LOW";
            }
            if(choice == 2){
                priority = "MEDIUM";
            }
            if(choice == 3){
                priority = "HIGH";
            }
            scanner.nextLine();
            System.out.println("Enter the status of the task:");
            System.out.println("Choose from the following options:");
            System.out.println("1.DONE");
            System.out.println("2.NOT DONE");
            System.out.println("3.IN PROGRESS");
            choice = 0;
            while (true) {
                if (scanner.hasNextInt()) { // Check if the input is an integer
                    choice = scanner.nextInt();
                    if (choice > 0 && choice < 4) { // Validate range
                        break; // Exit the loop if valid
                    } else {
                        System.out.print("Invalid option. Please enter a number between 1 and 3: ");
                    }
                } else {
                    System.out.print("Invalid input. Please enter a number between 1 and 3: ");
                    scanner.next(); // Discard invalid input
                }
            }
            String status = "";
            if(choice == 1){
                status = "DONE";
            }
            if(choice == 2){
                status = "NOT DONE";
            }
            if(choice == 3){
                status = "IN PROGRESS";
            }

            scanner.nextLine();
            System.out.println("Enter the type of the task:");
            System.out.println("Choose from the following options:");
            System.out.println("1.SCHOOL");
            System.out.println("2.WORK");
            System.out.println("3.PERSONAL");
            choice = 0;
            while (true) {
                if (scanner.hasNextInt()) { // Check if the input is an integer
                    choice = scanner.nextInt();
                    if (choice > 0 && choice < 4) { // Validate range
                        break; // Exit the loop if valid
                    } else {
                        System.out.print("Invalid option. Please enter a number between 1 and 3: ");
                    }
                } else {
                    System.out.print("Invalid input. Please enter a number between 1 and 3: ");
                    scanner.next(); // Discard invalid input
                }
            }
            String type = "";
            if(choice == 1){
                type = "SCHOOL";
            }
            if(choice == 2){
                type = "WORK";
            }
            if(choice == 3){
                type = "PERSONAL";
            }
            scanner.nextLine();
            Database_Manager.addToDatabase(name, dueDate, priority, status, type);
            System.out.println("Task added successfully!");
           
        }
        if(option == 2){
        System.out.println("Enter the task number you want to update:");
        int taskNum = scanner.nextInt();
        scanner.nextLine();
        boolean if_exist = Database_Manager.taskFinder(taskNum);

        if (if_exist == false) {
            System.out.println("Task does not exist");

        }
        System.out.println("Task Found!");
        Database_Manager.viewTask(taskNum);
        System.out.println("Enter the new name of the task:");
        String name = scanner.next();
        System.out.println("Enter the new due date of the task Format (YYYY-MM-DD):");
        String dueDate = scanner.next();
        System.out.println("Enter the new priority of the task (OPTIONS: LOW, MEDIUM, HIGH):");
        String priority = scanner.next();
        System.out.println("Enter the new status of the task (OPTION:DONE,NOT DONE, IN PROGRESS):");
        String status = scanner.next();
        System.out.println("Enter the new type of the task (OPTIONS:HOME,WORK,SCHOOL,PERSONAL):");
        String type = scanner.next();
        Database_Manager.deleteFromDatabase(taskNum);
        Database_Manager.addToDatabase(name, dueDate, priority, status, type);
        System.out.println("Task updated successfully!");
    }
    if(option == 3){
        Database_Manager.viewDatabase();
        System.out.println("Enter the task number you want to delete:");
        int taskNum = 0;
        while (true) {
            if (scanner.hasNextInt()) { // Check if the input is an integer
                taskNum = scanner.nextInt();
                if (taskNum > 0) { // Validate range
                    break; // Exit the loop if valid
                } else {
                    System.out.print("Invalid option. Please enter a number between 1 and 5: ");
                }
            } else {
                System.out.print("Invalid input. Please enter a number between 1 and 5: ");
                scanner.next(); // Discard invalid input
            }

        }
        boolean if_exist = Database_Manager.taskFinder(taskNum);

        if (if_exist == false) {
            System.out.println("Task does not exist");
            
            
        }
        Database_Manager.deleteFromDatabase(taskNum);
        System.out.println("Task deleted successfully!");
    }
    if(option == 4){
        System.out.println("Enter the task number you want to mark as done:");
        int taskNum = 0;
        while (true) {
            if (scanner.hasNextInt()) { // Check if the input is an integer
                taskNum = scanner.nextInt();
                if (taskNum > 0) { // Validate range
                    break; // Exit the loop if valid
                } 
            } 
            else {
                System.out.print("Invalid input. Please enter a number between 1 and 5: ");
                scanner.next(); // Discard invalid input
            }
        }
        boolean if_exist = Database_Manager.taskFinder(taskNum);
        if(if_exist == false){
            System.out.println("Task does not exist");
          
            
        }
        Database_Manager.markAsDone(taskNum);
    }
  
        
    }

    // Method to delete a task
    public void deleteTask(int taskNumber) {
        boolean if_exist = Database_Manager.taskFinder(taskNumber);
        if(if_exist == false){
            System.out.println("Task does not exist");
           
        }
        Database_Manager.deleteFromDatabase(taskNumber);
    }

    // Method to mark a task as done
    public void markAsDone(int taskNumber) {
        boolean if_exist = Database_Manager.taskFinder(taskNumber);
        if(if_exist == false){
            System.out.println("Task does not exist");
            
        }
        Database_Manager.markAsDone(taskNumber);
    }

    // Method to search for a task
    public void searchTask(int taskNumber) {
        boolean if_exist = Database_Manager.taskFinder(taskNumber);
        if(if_exist == false){
            System.out.println("Task does not exist");
        }
        System.out.println("Task Found!");
        Database_Manager.viewTask(taskNumber);
       
    }

    // Method to add a task
    public void addTask( String name, String dueDate, String priority, String status, String type) {
        Database_Manager.addToDatabase(name, dueDate, priority, status, type);
    }

public static void WeatherApp() {
        String urlString = "https://api.openweathermap.org/data/2.5/weather?lat=33.977948&lon=-117.333346&appid=91331&appid=1d8d27832ed11599a97afc0289dd4646";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Print weather data (JSON format)
                System.out.println("Weather data: " + response.toString());
            } else {
                System.out.println("Error: Unable to fetch weather data. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String TodaysDate() {
          LocalDate today = LocalDate.now();

        // Format the date (optional)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = today.format(formatter);
        return formattedDate;
    }
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
