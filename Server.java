import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 2234;
    private static final Map<String, String> users = new HashMap<>();
    private static final List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        users.put("user1", "password1");
        users.put("user2", "password2");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private String username;
        private boolean authenticated = false;
        private BufferedReader input;
        private PrintWriter output;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);


                while (!authenticated) {
                    output.println("Enter username:");
                    String user = input.readLine();
                    output.println("Enter password:");
                    String pass = input.readLine();

                    if (users.containsKey(user) && users.get(user).equals(pass)) {
                        authenticated = true;
                        username = user;
                        output.println("Authentication successful. Welcome, " + username);
                        broadcast(username + " has joined the chat.");
                    } else {
                        output.println("Authentication failed. Try again.");
                    }
                }


                String message;
                while ((message = input.readLine()) != null) {
                    if ("exit".equalsIgnoreCase(message)) {
                        output.println("Goodbye!");
                        break;
                    } else if (message.startsWith("@sendfile")) {
                        receiveFile(message);
                    } else {
                        broadcast(username + ": " + message);
                    }
                }


                socket.close();
                clients.remove(this);
                broadcast(username + " has left the chat.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void receiveFile(String command) {
            try {
                output.println("Send file size:");
                int fileSize = Integer.parseInt(input.readLine());
                output.println("Send file data:");

                byte[] buffer = new byte[fileSize];
                socket.getInputStream().read(buffer, 0, fileSize);
                output.println("File received successfully!");
            } catch (IOException e) {
                output.println("File transfer failed.");
            }
        }

        private void broadcast(String message) {
            for (ClientHandler client : clients) {
                if (client != this) {
                    client.output.println(message);
                }
            }
        }
    }
}


