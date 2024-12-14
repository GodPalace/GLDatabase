package com.godpalace.test;

import com.godpalace.data.annotation.LocalDatabase;
import com.godpalace.data.database.FileDatabaseEngine;
import com.godpalace.data.annotation.Data;

import java.util.Scanner;

@LocalDatabase(path = ExampleDatabase.PATH)
public class ExampleDatabase {
    public static final String PATH = "example.db";

    @Data
    public String name;

    @Data
    public int age;

    @Data
    public boolean isBoy;

    @Data
    public float height;

    public ExampleDatabase(String name, int age, boolean isBoy, float height) {
        this.name = name;
        this.age = age;
        this.isBoy = isBoy;
        this.height = height;
    }

    public static void main(String[] args) throws Exception {
        ExampleDatabase exampleDatabase = new ExampleDatabase("Tom", 20, true, 1.7F);

        System.out.println(exampleDatabase.name);
        System.out.println(exampleDatabase.age);
        System.out.println(exampleDatabase.isBoy);
        System.out.println(exampleDatabase.height);

        // Create a new instance of FileDatabaseEngine and initialize it with ExampleDatabase instance
        FileDatabaseEngine.init(ExampleDatabase.class, exampleDatabase);

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter name: ");
        exampleDatabase.name = scanner.nextLine();

        System.out.print("Enter age: ");
        exampleDatabase.age = Integer.parseInt(scanner.nextLine());

        System.out.print("Enter isBoy: ");
        exampleDatabase.isBoy = Boolean.parseBoolean(scanner.nextLine());

        System.out.print("Enter height: ");
        exampleDatabase.height = Float.parseFloat(scanner.nextLine());

        scanner.close();
        System.out.println("Data saved!");
    }
}
