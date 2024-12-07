import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String serverAddress = "64.187.248.231";
        int port = 2234;

        try (Socket socket = new Socket(serverAddress, port)) {
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));



            String serverResponse;
            while ((serverResponse = input.readLine()) != null) {
                System.out.println(serverResponse);
                if (serverResponse.startsWith("Authentication successful")) {
                    break;
                }
                String userInput = consoleInput.readLine();
                output.println(userInput);
            }



            String userInput;
            while (true) {
                System.out.print("You: ");
                userInput = consoleInput.readLine();
                output.println(userInput);

                if ("exit".equalsIgnoreCase(userInput)) {
                    System.out.println("Exiting...");
                    break;
                }

                if (userInput.startsWith("@sendfile")) {
                    sendFile(userInput, socket);
                }

                serverResponse = input.readLine();
                System.out.println(serverResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFile(String command, Socket socket) {
        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            System.out.println("Invalid file transfer command. Use @sendfile filepath");
            return;
        }

        String filePath = parts[1];

        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             OutputStream outputStream = socket.getOutputStream()) {
            File file = new File(filePath);
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);


            output.println((int) file.length());


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("File sent successfully!");
        } catch (IOException e) {
            System.out.println("File transfer failed: " + e.getMessage());
        }
    }

}


