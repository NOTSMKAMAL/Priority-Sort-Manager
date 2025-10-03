package com.taskManagers.taskapp;

public class Main {
    public static void main(String[] args) {
        TaskAppConfig config = TaskAppConfigLoader.load(args);
        WeatherService weatherService = new WeatherService(config);
        try (TaskStore taskStore = TaskStoreFactory.create(config)) {
            Task taskApp = new Task(config, taskStore, weatherService);
            taskApp.run();
        } catch (Exception ex) {
            System.err.println("Failed to start application: " + ex.getMessage());
        }
    }
}
