package org.iot;


import org.sqlite.SQLiteConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    private Connection connection;
    private boolean isOpened;
    String dbFileName = "iot";
    String database = "iot";
    String tableName = "testTable";

    public static void main(String[] args) {
        System.out.println("Hello world from server");

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean open() {
        try { // 읽기 전용
            SQLiteConfig config = new SQLiteConfig();
            config.setReadOnly(true);
            this.connection = DriverManager.getConnection("jdbc:sqlite:/" + this.dbFileName, config.toProperties());
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        isOpened = true;
        return true;
    }

    public boolean close() {
        if (!this.isOpened) {
            return true;
        }
        try {
            this.connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}