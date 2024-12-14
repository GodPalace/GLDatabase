package com.godpalace.test;

import com.godpalace.data.annotation.Data;
import com.godpalace.data.annotation.RemoteDatabase;
import com.godpalace.data.database.RemoteDatabaseEngine;

@RemoteDatabase
public class TestRemoteDatabase2 {
    @Data
    public static String name = "TestRemoteDatabase2";

    @Data
    public static int age = 14;

    public static void main(String[] args) throws Exception {
        RemoteDatabaseEngine.init(TestRemoteDatabase2.class, null,
                "localhost", 8080, "admin", "admin123");

        System.out.println(name);
        System.out.println(age);
    }
}
