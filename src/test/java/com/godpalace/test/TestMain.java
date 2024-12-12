package com.godpalace.test;

import com.godpalace.data.FileDatabase;
import com.godpalace.data.annotation.Data;
import com.godpalace.data.annotation.Database;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

@Database(path = "test.db")
public class TestMain {
    @Data
    public static AtomicInteger a = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        FileDatabase.init(TestMain.class, null);

        System.out.println(a);
        a.set(new Scanner(System.in).nextInt());
    }
}
