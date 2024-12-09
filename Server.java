import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 2234;
    private static final Map<String, String> USERS = new HashMap<>();
    private static final Map<String, Socket> ACTIVE_CLIENTS = new HashMap<>();

    static {

        USERS.put("user1", "pass1");
        USERS.put("user2", "pass2");
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started. Waiting for clients...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {


                out.writeUTF("Enter username:");
                String username = in.readUTF();
                out.writeUTF("Enter password:");
                String password = in.readUTF();

                if (USERS.containsKey(username) && USERS.get(username).equals(password)) {
                    this.username = username;
                    ACTIVE_CLIENTS.put(username, socket);
                    out.writeUTF("Authentication successful. You are online.");
                    System.out.println(username + " logged in.");
                    broadcastStatus();
                } else {
                    out.writeUTF("Authentication failed. Disconnecting.");
                    socket.close();
                    return;
                }


                while (true) {
                    String command = in.readUTF();
                    if (command.startsWith("MESSAGE")) {
                        String[] parts = command.split(" ", 3);
                        if (parts.length < 3) {
                            out.writeUTF("Invalid MESSAGE command. Format: MESSAGE <recipient> <message>");
                        } else {
                            String recipient = parts[1];
                            String message = parts[2];
                            sendMessage(recipient, message);
                        }
                    } else if (command.startsWith("FILE")) {
                        String[] parts = command.split(" ", 2);
                        if (parts.length < 2) {
                            out.writeUTF("Invalid FILE command. Format: FILE <recipient>");
                        } else {
                            String recipient = parts[1];
                            receiveAndSendFile(recipient, in);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(username + " disconnected.");
            } finally {
                if (username != null) {
                    ACTIVE_CLIENTS.remove(username);
                    broadcastStatus();
                }
            }
        }

        private void broadcastStatus() {
            String status = "Online users: " + String.join(", ", ACTIVE_CLIENTS.keySet());
            ACTIVE_CLIENTS.values().forEach(socket -> {
                try {
                    new DataOutputStream(socket.getOutputStream()).writeUTF(status);
                } catch (IOException ignored) {}
            });
        }

        private void sendMessage(String recipient, String message) {
            Socket recipientSocket = ACTIVE_CLIENTS.get(recipient);
            if (recipientSocket != null) {
                try {
                    new DataOutputStream(recipientSocket.getOutputStream())
                            .writeUTF("MESSAGE FROM " + username + ": " + message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    new DataOutputStream(socket.getOutputStream())
                            .writeUTF("User " + recipient + " is not online.");
                } catch (IOException ignored) {}
            }
        }

        private void receiveAndSendFile(String recipient, DataInputStream in) throws IOException {
            Socket recipientSocket = ACTIVE_CLIENTS.get(recipient);
            if (recipientSocket != null) {
                DataOutputStream out = new DataOutputStream(recipientSocket.getOutputStream());
                out.writeUTF("FILE FROM " + username);
                int fileSize = in.readInt();
                byte[] buffer = new byte[fileSize];
                in.readFully(buffer);
                out.writeInt(fileSize);
                out.write(buffer);
            } else {
                try {
                    new DataOutputStream(socket.getOutputStream())
                            .writeUTF("User " + recipient + " is not online.");
                } catch (IOException ignored) {}
            }
        }
    }
}