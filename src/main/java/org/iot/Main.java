package org.iot;

import org.mariadb.jdbc.Configuration;
import org.mariadb.jdbc.Connection;

import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world from server");
        Connection con = null;

        String server = "127.0.0.1:3306";  // 본인의 wsl 서버 ip를 삽입
        String database = "iot";
        String user_name = "iot";
        String password = "hrd";

        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println(" Driver loading error : " + e.getMessage());
            e.printStackTrace();
        }

        try {
            con = org.mariadb.jdbc.Driver.connect(new Configuration.Builder().addHost("127.0.0.1", 3306).user("iot").password("hrd").database("iot").build());
            System.out.println("Connection Success!");
        } catch(SQLException e) {
            System.err.println("Error :" + e.getMessage());
            e.printStackTrace();
        }
        try {
            if(con != null)
                con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}