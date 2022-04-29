package org.iot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    ExecutorService executorService;
    ServerSocket serverSocket;
    List<Client> connections = new Vector<>();
    public static boolean isServerOn = false;

    public void startServer() {
        executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("localhost", 7030));
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
                    if (!serverSocket.isClosed()) stopServer();
                    break;
                }
            }
        };
        executorService.submit(runnable);
    }

    void stopServer() {
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
    }

    class Client {
        Socket socket;

        Client(Socket socket) {
            this.socket = socket;
            receive();
        }

        void receive() {
            Runnable runnable = () -> {
                try {
                    while (true) {
                        byte[] byteArr = new byte[100];
                        InputStream inputStream = socket.getInputStream();
                        int readByteCount = inputStream.read(byteArr);
                        if (readByteCount == -1) throw new IOException();
                        String message = String.format("[요청 처리: %s: %s]", socket.getRemoteSocketAddress(), Thread.currentThread().getName());
                        Debug.println(Server.class, message);
                        String data = new String(byteArr, 0, readByteCount, StandardCharsets.UTF_8);
                        for (Client client :
                                connections) {
                            client.send(data);
                        }
                    }
                } catch (Exception e) {
                    try {
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
    }
}
