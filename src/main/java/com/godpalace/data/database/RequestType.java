package com.godpalace.data.database;

public enum RequestType {
    PUT(0), LOAD(1);

    private final int value;

    RequestType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static RequestType fromValue(int value) {
        for (RequestType type : RequestType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }

        return null;
    }
}
