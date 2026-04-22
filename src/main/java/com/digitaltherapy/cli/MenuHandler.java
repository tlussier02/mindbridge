package com.digitaltherapy.cli;

import java.util.List;
import java.util.Scanner;

public class MenuHandler {
    private final Scanner scanner;
    private final List<Command> commands;
    private final String title;

    public MenuHandler(String title, Scanner scanner, List<Command> commands) {
        this.title = title;
        this.scanner = scanner;
        this.commands = commands;
    }

    public void display() {
        System.out.println("\n========================================");
        System.out.println("  " + title);
        System.out.println("========================================");
        for (int i = 0; i < commands.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, commands.get(i).getName());
        }
        System.out.println("========================================");
        System.out.print("Select an option: ");
    }

    public void handleInput() {
        display();
        try {
            String input = scanner.nextLine().trim();
            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= commands.size()) {
                commands.get(choice - 1).execute();
            } else {
                System.out.println("Invalid option. Please try again.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }
}
