package org.iot;

import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        System.out.println("Hello world from server");
        String server = "jdbc:mariadb://127.0.0.7:3306/iot";
        String database = "iot";
        String user_name = "iot";
        String password = "hrd";
        var con = DriverManager.getConnection(server, user_name, password);
        if (con != null) {
            System.out.println("JDBC connection: OK");
            con.close();
        }
    }
}