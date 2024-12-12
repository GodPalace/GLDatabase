package com.godpalace.data.example;

import com.godpalace.data.FileDatabase;
import com.godpalace.data.annotation.Data;
import com.godpalace.data.annotation.Database;

import java.util.Scanner;

@Database(path = "example.db")
public class ADatabase {
    @Data
    public String name;

    @Data
    public int age;

    @Data
    public boolean isBoy;

    @Data
    public float height;

    public ADatabase(String name, int age, boolean isBoy, float height) {
        this.name = name;
        this.age = age;
        this.isBoy = isBoy;
        this.height = height;
    }

    public static void main(String[] args) throws Exception {
        ADatabase aDatabase = new ADatabase("Tom", 20, true, 1.7F);

        System.out.println(aDatabase.name);
        System.out.println(aDatabase.age);
        System.out.println(aDatabase.isBoy);
        System.out.println(aDatabase.height);

        // Create a new instance of FileDatabase and initialize it with ADatabase instance
        FileDatabase.init(ADatabase.class, aDatabase);

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter name: ");
        aDatabase.name = scanner.nextLine();

        System.out.print("Enter age: ");
        aDatabase.age = Integer.parseInt(scanner.nextLine());

        System.out.print("Enter isBoy: ");
        aDatabase.isBoy = Boolean.parseBoolean(scanner.nextLine());

        System.out.print("Enter height: ");
        aDatabase.height = Float.parseFloat(scanner.nextLine());

        scanner.close();
        System.out.println("Data saved!");
    }
}
