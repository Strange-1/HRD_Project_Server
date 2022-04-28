package org.iot;

import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        System.out.println("Hello world from server");
        String server = "127.0.0.1:3306";  // 본인의 wsl 서버 ip를 삽입
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