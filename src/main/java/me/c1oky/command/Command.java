package me.c1oky.command;

public interface Command {

    String getName();

    String getDescription();

    String execute(final String[] args) throws InterruptedException;

    String[] getArgs();
}