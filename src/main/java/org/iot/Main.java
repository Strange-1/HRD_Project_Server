package org.iot;


import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    private static Connection connection;
    private static boolean isOpened;
    private static final String dbFileName = "iot";
    String database = "iot.db";
    String tableName = "testTable";

    public static void main(String[] args) {
        System.out.println("== Iot@HRD Project: Parking lot ==");
        Debug.println(Main.class, "Initializing...");

        try {
            Class.forName("org.sqlite.JDBC");
            if ((connection = openDB()) != null) {
                isOpened = true;
                Debug.println(Main.class, "SQLite Connection: OK");
            }
            Server.startServer();


        } catch (Exception e) {
            e.printStackTrace();
        }
        while (!Server.isServerOn) ;
        while (Server.isServerOn) ;
        if (closeDB())
            Debug.println(Main.class, "SQLite Closure: OK");
    }

    public static Connection openDB() {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.setOpenMode(SQLiteOpenMode.READWRITE);
            return DriverManager.getConnection("jdbc:sqlite:" + dbFileName, config.toProperties());
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean closeDB() {
        if (!isOpened) {
            return true;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}