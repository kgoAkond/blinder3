package com.example.blind3.model;

public final class Contact {
    private final String name;
    private final String number;
    private final int key;
    private final int priority;
    private String desc;

    public Contact(String name, String number, int key, int priority) {
        this.name = name;
        this.number = number;
        this.key = key;
        this.priority = priority;
    }

    public Contact(String name, String number, int key, int priority, String desc) {
        this.name = name;
        this.number = number;
        this.key = key;
        this.priority = priority;
        this.desc = desc;
    }

    public String name() {
        return name;
    }

    public String number() {
        return number;
    }

    public int key() {
        return key;
    }

    public int priority() {
        return priority;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
