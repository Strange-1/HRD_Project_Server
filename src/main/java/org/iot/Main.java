package org.iot;


import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    private static Connection connection;
    private static boolean isOpened;

    static Server server;
    String database = "iot.db";
    String tableName = "testTable";

    public static void main(String[] args) {
        System.out.println("== Iot@HRD Project: Parking lot ==");
        Debug.println(Main.class, "Initializing...");

        try {
            server = new Server();
            server.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}