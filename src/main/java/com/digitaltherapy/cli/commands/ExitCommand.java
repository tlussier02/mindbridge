package com.digitaltherapy.cli.commands;

import com.digitaltherapy.cli.Command;

public class ExitCommand implements Command {
    private final Runnable onExit;

    public ExitCommand(Runnable onExit) {
        this.onExit = onExit;
    }

    @Override
    public void execute() {
        System.out.println("\nThank you for using Digital Therapy Assistant. Take care!");
        onExit.run();
    }

    @Override
    public String getName() {
        return "Exit";
    }

    @Override
    public String getDescription() {
        return "Exit the application";
    }
}
