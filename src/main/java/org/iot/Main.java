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
        System.out.println("Hello world from server");

        try {
            Class.forName("org.sqlite.JDBC");
            if ((connection = open()) != null) {
                isOpened = true;
                System.out.println("JDBC Connection: OK");
            }
            if (close())
                System.out.println("JDBC Closure: OK");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection open() {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.setOpenMode(SQLiteOpenMode.READWRITE);
            return DriverManager.getConnection("jdbc:sqlite:" + dbFileName, config.toProperties());
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean close() {
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