package org.iot;

import java.sql.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world from server");
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306",
                    "iot",
                    "hrd"
            );

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "show databases"
            );

            while (rs.next()) {
                System.out.println(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}