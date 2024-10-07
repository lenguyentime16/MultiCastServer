package com.mycompany.chatserver;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static Map<String, Set<ClientHandler>> groupMap = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Server đang chạy...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void joinGroup(String groupName, ClientHandler client) {
        groupMap.putIfAbsent(groupName, new HashSet<>());
        Set<ClientHandler> groupMembers = groupMap.get(groupName);
        groupMembers.add(client);

        broadcastToGroup(groupName, client.getUsername() + " (" + client.getClientIP() + ") đã tham gia nhóm.");
        client.sendMessage("Bạn đã tham gia nhóm: " + groupName);
    }

    public static void broadcastToGroup(String groupName, String message) {
        Set<ClientHandler> groupMembers = groupMap.get(groupName);
        if (groupMembers != null) {
            for (ClientHandler member : groupMembers) {
                member.sendMessage("[Nhóm " + groupName + "] " + message);
            }
        }
    }

    public static void broadcastToAll(String message) {
        for (ClientHandler client : clientHandlers) {
            client.sendMessage(message);
        }
    }

    public static void sendPrivateMessage(String targetUsername, String message, ClientHandler sender) {
        for (ClientHandler client : clientHandlers) {
            if (client.getUsername().equalsIgnoreCase(targetUsername)) {
                client.sendMessage("[Tin riêng từ " + sender.getUsername() + " (" + sender.getClientIP() + ")]: " + message);
                sender.sendMessage("Tin nhắn riêng đã gửi tới " + targetUsername);
                return;
            }
        }
        sender.sendMessage("Người dùng " + targetUsername + " không tồn tại.");
    }

    public static void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
        for (Set<ClientHandler> groupMembers : groupMap.values()) {
            groupMembers.remove(client);
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Enter your username:");
            username = in.readLine();

            out.println("Welcome " + username + "!");
            ChatServer.broadcastToAll(username + " (" + getClientIP() + ") đã tham gia phòng chat.");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("MSG:")) {
                    String msgToBroadcast = username + " (" + getClientIP() + "): " + message.substring(4);
                    ChatServer.broadcastToAll(msgToBroadcast);
                } else if (message.startsWith("JOIN:")) {
                    String groupName = message.substring(5);
                    ChatServer.joinGroup(groupName, this);
                } else if (message.startsWith("GROUP:")) {
                    String[] parts = message.split(":", 3);
                    String groupName = parts[1];
                    String groupMessage = username + " (" + getClientIP() + "): " + parts[2];
                    ChatServer.broadcastToGroup(groupName, groupMessage);
                } else if (message.startsWith("PRIVATE:")) {
                    String[] parts = message.split(":", 3);
                    String targetUsername = parts[1];
                    String privateMessage = parts[2];
                    ChatServer.sendPrivateMessage(targetUsername, privateMessage, this);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ChatServer.removeClient(this);
                socket.close();
                ChatServer.broadcastToAll(username + " đã rời khỏi phòng chat.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public String getClientIP() {
        return socket.getInetAddress().getHostAddress();
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
