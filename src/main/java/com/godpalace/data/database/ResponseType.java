package com.godpalace.data.database;

public enum ResponseType {
    OK(1), ERROR(0), NO_DATA(-2), PASSWORD_ERROR(2);

    private final int value;

    ResponseType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
