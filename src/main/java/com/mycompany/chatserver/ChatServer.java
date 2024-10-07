/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.chatserver;

/**
 *
 * @author OS
 */
// File: ChatServer.java

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static Map<String, Set<ClientHandler>> groupMap = new HashMap<>(); // Quản lý các nhóm và thành viên nhóm

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
    
    

    // Xử lý việc tham gia nhóm
    public static void joinGroup(String groupName, ClientHandler client) {
        groupMap.putIfAbsent(groupName, new HashSet<>()); // Nếu nhóm chưa tồn tại, tạo nhóm mới
        Set<ClientHandler> groupMembers = groupMap.get(groupName);
        groupMembers.add(client); // Thêm client vào nhóm

        // Gửi thông báo cho tất cả thành viên trong nhóm
        broadcastToGroup(groupName, client.getUsername() + " đã tham gia nhóm.");
        client.sendMessage("Bạn đã tham gia nhóm: " + groupName);
    }

    // Gửi tin nhắn cho tất cả thành viên trong nhóm
    public static void broadcastToGroup(String groupName, String message) {
        Set<ClientHandler> groupMembers = groupMap.get(groupName);
        if (groupMembers != null) {
            for (ClientHandler member : groupMembers) {
                member.sendMessage("[Nhóm " + groupName + "] " + message);
            }
        }
    }

    // Gửi tin nhắn đến tất cả các client (tin nhắn toàn server)
    public static void broadcastToAll(String message) {
        for (ClientHandler client : clientHandlers) {
            client.sendMessage(message);
        }
    }

    // Xử lý khi một client rời khỏi server
    public static void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
        
        // Xóa client khỏi tất cả các nhóm
        for (Set<ClientHandler> groupMembers : groupMap.values()) {
            groupMembers.remove(client);
        }
    }
    
    public static void sendPrivateMessage(String targetUsername, String message, ClientHandler sender) {
    for (ClientHandler client : clientHandlers) {
        if (client.getUsername().equals(targetUsername)) {
            client.sendMessage(message); // Gửi tin nhắn cho client nhận
            sender.sendMessage("Bạn đã gửi tin nhắn cho " + targetUsername + ": " + message); // Phản hồi cho người gửi
            return;
        }
    }
    // Nếu không tìm thấy người dùng
    sender.sendMessage("Không tìm thấy người dùng với tên: " + targetUsername);
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
    
    public String getClientIP() {
    return socket.getInetAddress().getHostAddress();
    }


    @Override
public void run() {
    try {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Yêu cầu người dùng nhập tên
        out.println("Enter your username:");
        username = in.readLine();

        // Gửi thông báo chào mừng
        out.println("Welcome " + username + "!");
        ChatServer.broadcastToAll(username + " đã tham gia phòng chat.");

        String message;
        while ((message = in.readLine()) != null) {
            if (message.startsWith("MSG:")) {
                // Xử lý tin nhắn toàn server
                String msgToBroadcast = username + " (" + getClientIP() + "): " + message.substring(4);
                ChatServer.broadcastToAll(msgToBroadcast);
            } else if (message.startsWith("JOIN:")) {
                // Xử lý tham gia nhóm
                String groupName = message.substring(5);
                ChatServer.joinGroup(groupName, this);
            } else if (message.startsWith("GROUP:")) {
                // Xử lý tin nhắn nhóm
                String[] parts = message.split(":", 3); // GROUP:groupName:message
                String groupName = parts[1];
                String groupMessage = username + " (" + getClientIP() + "): " + parts[2];
                ChatServer.broadcastToGroup(groupName, groupMessage);
            } else if (message.startsWith("PRIVATE:")) {
                // Xử lý tin nhắn riêng
                String[] parts = message.split(":", 3); // PRIVATE:targetUsername:message
                String targetUsername = parts[1];
                String privateMessage = parts[2];
                ChatServer.sendPrivateMessage(targetUsername, username + " (" + getClientIP() + ") (tin nhắn riêng): " + privateMessage, this);
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        // Xử lý khi client ngắt kết nối
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

    public void sendMessage(String message) {
        out.println(message);
    }
}




