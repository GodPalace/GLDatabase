package com.godpalace.test;

import com.godpalace.data.server.RemoteDatabaseServer;

import java.io.File;
import java.net.InetSocketAddress;

public class TestRemoteDatabase {
    public static void main(String[] args) throws Exception {
        RemoteDatabaseServer server = new RemoteDatabaseServer(
                new InetSocketAddress("localhost", 8080),
                new File("database.db"));

        server.addUser("admin", "admin123");
        server.addData("name", "God Palace");
        server.addData("age", 25);

        server.start();
    }
}
