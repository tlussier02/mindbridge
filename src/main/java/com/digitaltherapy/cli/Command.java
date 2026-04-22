package com.digitaltherapy.cli;

public interface Command {
    void execute();
    String getName();
    String getDescription();
}
