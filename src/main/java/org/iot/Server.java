package org.iot;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import javax.xml.transform.Result;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class Server {
    ExecutorService executorService;
    ServerSocket serverSocket;
    List<Client> connections = new Vector<>();
    HashMap<String, Long> sessions = new HashMap<>();
    Random random = new Random();
    Connection sqlConn;
    boolean isSqlOpen = false;
    public static boolean isServerOn = false;
    private static final String dbFileName = "iot";

    //IP: 34.68.12.173
    public void startServer() throws SQLException {
        random.setSeed(Instant.now().toEpochMilli());
        connectSQL();
        executorService = Executors.newFixedThreadPool(100);
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(7030));
        } catch (Exception e) {
            if (!serverSocket.isClosed()) stopServer();
            return;
        }
        Runnable runnable = () -> {
            Debug.println(Server.class, "서버 시작");
            isServerOn = true;
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    String message = String.format("[연결 수락: %s: %s]", socket.getRemoteSocketAddress(), Thread.currentThread().getName());
                    Debug.println(Server.class, message);
                    Client client = new Client(socket);
                    connections.add(client);
                    Debug.println(Server.class, String.format("[연결 갯수: %d]", connections.size()));
                } catch (Exception e) {
                    if (!serverSocket.isClosed()) {
                        try {
                            stopServer();
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    break;
                }
            }
        };
        executorService.submit(runnable);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Integer> listToRemove = new ArrayList<>();
                while (true) {
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (!isSqlOpen) break;
                    Calendar calendar = new Calendar.Builder().setInstant(Instant.now().toEpochMilli()).setTimeZone(TimeZone.getTimeZone("GMT+9")).build();
                    int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH) + 1;
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    Debug.println(Server.class, String.format("NOW: %d년 %d월 %d일 %d시, %s", year, month, day, hour, calendar.getTimeZone().getDisplayName()));
                    PreparedStatement statement = null;
                    ResultSet sqlResult = null;
                    try {
                        statement = sqlConn.prepareStatement("select * from reservation where status=?");
                        statement.setString(1, "ACTIVE");
                        sqlResult = statement.executeQuery();
                        listToRemove.clear();
                        while (sqlResult.next()) {
                            int sqlYear = Integer.parseInt(sqlResult.getString("year"));
                            int sqlMonth = Integer.parseInt(sqlResult.getString("month")) - 1;
                            int sqlDay = Integer.parseInt(sqlResult.getString("day"));
                            int sqlHour = Integer.parseInt(sqlResult.getString("hour"));
                            long sqlDateTime = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("GMT+9"))
                                    .setDate(sqlYear, sqlMonth, sqlDay)
                                    .setTimeOfDay(sqlHour, 0, 0).build().getTimeInMillis();
                            Debug.println(Server.class, "SQL time: " + sqlDateTime);
                            if (sqlDateTime - Instant.now().toEpochMilli() < 0) {
                                listToRemove.add(sqlResult.getInt("id"));
                            }
                        }
                        for (var i : listToRemove) {
                            statement = sqlConn.prepareStatement("update reservation set status=? where id=?");
                            statement.setString(1, "EXPIRED");
                            statement.setInt(2, i);
                            if (statement.executeUpdate() > 0)
                                Debug.println(Server.class, "some reservation have been expired. id: " + i);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    private void connectSQL() {
        try {
            Class.forName("org.sqlite.JDBC");
            if ((sqlConn = openDB()) != null) {
                isSqlOpen = true;
                sqlConn.setAutoCommit(true);
                Debug.println(Main.class, "SQLite Connection: OK");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Connection openDB() {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.setOpenMode(SQLiteOpenMode.READWRITE);
            return DriverManager.getConnection("jdbc:sqlite:" + dbFileName, config.toProperties());
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean closeDB() {
        if (!isSqlOpen) {
            return true;
        }
        try {
            sqlConn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        isSqlOpen = false;
        return true;
    }

    void stopServer() throws SQLException {
        try {
            Iterator<Client> iterator = connections.iterator();
            while (iterator.hasNext()) {
                Client client = iterator.next();
                client.socket.close();
                iterator.remove();
            }
            if (serverSocket != null && !executorService.isShutdown())
                serverSocket.close();
            if (executorService != null && !executorService.isShutdown())
                executorService.shutdown();
            Debug.println(Server.class, "[서버 멈춤]");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isServerOn = false;
        }
        if (closeDB())
            sqlConn.close();
    }


    class Client {
        Socket socket;
        private String userNumber = "";

        Client(Socket socket) {
            this.socket = socket;
            receive();
        }

        void receive() {
            Runnable runnable = () -> {
                try {
                    while (true) {
                        byte[] byteArr = new byte[1024];
                        InputStream inputStream = socket.getInputStream();
                        int readByteCount = inputStream.read(byteArr);
                        if (readByteCount == -1) throw new IOException();
                        String message = String.format("[요청 처리: %s: %s]", socket.getRemoteSocketAddress(), Thread.currentThread().getName());
                        Debug.println(Server.class, message);
                        String data = new String(byteArr, 0, readByteCount, StandardCharsets.UTF_8);
                        Debug.println(Server.class, "[" + (userNumber.isEmpty() ? "" : userNumber + "@") + socket.getInetAddress().toString().substring(1) + "]: " + data);
                        send(response(data));
                    }
                } catch (Exception e) {
                    try {
                        sessions.remove(userNumber);
                        connections.remove(Client.this);
                        String message = String.format("[클라이언트 통신 안됨: %s: %s]", socket.getRemoteSocketAddress(), Thread.currentThread().getName());
                        Debug.println(Server.class, message);
                        socket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            };
            executorService.submit(runnable);
        }

        void send(String data) {
            Runnable runnable = () -> {
                try {
                    byte[] byteArr = data.getBytes(StandardCharsets.UTF_8);
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(byteArr);
                    outputStream.flush();
                    Debug.println(Server.class, String.format("[회신 보냄: %s]", data));
                } catch (Exception e) {
                    try {
                        String message = String.format("[클라이언트 통신 안됨: %s: %s]", socket.getRemoteSocketAddress(), Thread.currentThread().getName());
                        Debug.println(Server.class, message);
                        connections.remove(Client.this);
                        socket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            };
            executorService.submit(runnable);
        }

        String response(String data) {
            JSONObject responseData = new JSONObject();
            JSONObject jsonObject;
            PreparedStatement statement;
            ResultSet queryResult;
            try {
                jsonObject = (JSONObject) new JSONParser().parse(data);
            } catch (ParseException e) {
                responseData.put("result", "NG");
                responseData.put("data", "JSON syntax error");
                return responseData.toJSONString();
            }
            try {
                switch (jsonObject.get("type").toString().toLowerCase(Locale.ROOT)) {
                    case "echo":
                        jsonObject.remove("type");
                        jsonObject.put("result", "OK");
                        return jsonObject.toJSONString();
                    case "login":
                        if (!(jsonObject.containsKey("id") && jsonObject.containsKey("pw"))) {
                            responseData.put("result", "NG");
                            responseData.put("data", "JSON syntax error");
                            return responseData.toJSONString();
                        }
                        statement = sqlConn.prepareStatement("select * from user where id=?;");
                        statement.setString(1, jsonObject.get("id").toString());
                        queryResult = statement.executeQuery();
                        if (queryResult.next() && queryResult.getString(2).equals(jsonObject.get("pw").toString())) {
                            userNumber = queryResult.getString(3);
                            responseData.put("result", "OK");
                            responseData.put("userNumber", userNumber);
                            Long sessionNumber = random.nextLong();
                            responseData.put("sessionNumber", sessionNumber);
                            sessions.put(userNumber, sessionNumber);
                        } else {
                            responseData.put("result", "NG");
                            responseData.put("data", "wrong account");
                        }
                        break;
                    case "logout":
                        sessions.remove(userNumber);
                        responseData.put("result", "OK");
                        break;
                    case "reservation":
                        if (vailidate(jsonObject)) {

                            try {
                                statement = sqlConn.prepareStatement("select * from reservation order by id desc");
                                queryResult = statement.executeQuery();
                                int nextId;
                                if (queryResult.next()) {
                                    nextId = queryResult.getInt("id") + 1;
                                    Debug.println(Server.class, "nextId: " + nextId);
                                } else
                                    nextId = 1;

                                statement = sqlConn.prepareStatement("select * from reservation where userNumber=? and status=?");
                                statement.setString(1, userNumber);
                                statement.setString(2, "ACTIVE");
                                queryResult = statement.executeQuery();
                                if (queryResult.next()) {
                                    responseData.put("result", "NG");
                                    responseData.put("data", "Another active reservation exists");
                                } else {
                                    statement = sqlConn.prepareStatement("insert into reservation values (?,?,?,?,?,?,?,?,?)");
                                    statement.setInt(1, nextId);           //id
                                    statement.setString(2, userNumber);     //userNumber
                                    statement.setInt(3, Math.toIntExact((long) jsonObject.get("year")));            //year
                                    statement.setInt(4, Math.toIntExact((long) jsonObject.get("month")));           //month
                                    statement.setInt(5, Math.toIntExact((long) jsonObject.get("day")));             //day
                                    statement.setInt(6, Math.toIntExact((long) jsonObject.get("hour")));            //hour
                                    statement.setInt(7, 0);                                                             //minute
                                    statement.setString(8, jsonObject.get("parkingSpot").toString());                    //position
                                    statement.setString(9, "ACTIVE");
                                    Debug.println(Server.class, statement.toString());
                                    statement.executeUpdate();
                                    responseData.put("result", "OK");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                responseData.put("result", "NG");
                                responseData.put("data", "SQL Error");
                            }
                        } else {
                            responseData.put("result", "NG");
                        }
                        break;
                    case "parkinglotstructure":
                        statement = sqlConn.prepareStatement("select * from parkinglotStructure ORDER BY position");
                        queryResult = statement.executeQuery();
                        responseData.put("result", "OK");
                        JSONArray parkinglotArray = new JSONArray();
                        while (queryResult.next()) {
                            JSONObject parkinglotRow = new JSONObject();
                            parkinglotRow.put("position", queryResult.getString("position"));
                            parkinglotRow.put("name", queryResult.getString("name"));
                            parkinglotArray.add(parkinglotRow);
                        }
                        responseData.put("data", parkinglotArray);
                        break;
                    case "mypage":
                        statement = sqlConn.prepareStatement("SELECT * FROM reservation where userNumber=? and status=? ORDER BY year ASC, month ASC, day ASC, hour ASC, minute ASC");
                        statement.setString(1, userNumber);
                        statement.setString(2, "ACTIVE");
                        queryResult = statement.executeQuery();
                        if (queryResult.next()) {
                            responseData.put("reservationCount", 1);
                            responseData.put("year", queryResult.getInt("year"));
                            responseData.put("month", queryResult.getInt("month"));
                            responseData.put("day", queryResult.getInt("day"));
                            responseData.put("hour", queryResult.getInt("hour"));
                            responseData.put("position", queryResult.getString("position"));
                        } else {
                            responseData.put("reservationCount", 0);
                        }
                        responseData.put("result", "OK");
                        break;
                    case "cancel":
                        statement = sqlConn.prepareStatement("SELECT * FROM reservation where userNumber=? and status=? ORDER BY year ASC, month ASC, day ASC, hour ASC, minute ASC");
                        statement.setString(1, userNumber);
                        statement.setString(2, "ACTIVE");
                        queryResult = statement.executeQuery();
                        if (queryResult.next()) {
                            int id = queryResult.getInt("id");
                            statement = sqlConn.prepareStatement("update reservation set status=? where id=?");
                            statement.setString(1, "CANCELLED");
                            statement.setInt(2, id);
                            if (statement.executeUpdate() > 0)
                                responseData.put("result", "OK");
                            else {
                                responseData.put("result", "NG");
                                responseData.put("data", "SQL Error");
                            }
                        } else {
                            responseData.put("result", "NG");
                            responseData.put("data", "No reservation exists");
                        }
                        break;
                    default: {
                        responseData.put("result", "NG");
                        responseData.put("data", "unknown type");
                    }
                }
            } catch (SQLException e) {
                responseData.put("result", "NG");
                responseData.put("data", "DB Server error");
                return responseData.toJSONString();
            }
            return responseData.toJSONString();
        }

        private boolean vailidate(JSONObject jsonObject) {
            if (jsonObject.containsKey("userNumber") && jsonObject.containsKey("sessionNumber")) {
                //TODO
                return true;
            } else
                return false;
        }
    }
}
