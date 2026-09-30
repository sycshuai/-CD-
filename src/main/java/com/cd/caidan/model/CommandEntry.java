package com.cd.caidan.model;

/**
 * 一条指令配置：以什么身份执行、执行什么指令。
 */
public class CommandEntry {

    private final String type;    // player / console
    private final String command; // 指令内容（不含前导 /）

    public CommandEntry(String type, String command) {
        this.type = type;
        this.command = command;
    }

    public String getType() {
        return type;
    }

    public String getCommand() {
        return command;
    }

    public boolean isConsole() {
        return "console".equalsIgnoreCase(type);
    }
}
