import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_HOST = "64.187.248.231"; //64.187.247.149
    private static final int SERVER_PORT = 2234;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {


            System.out.println(in.readUTF());
            out.writeUTF(scanner.nextLine());
            System.out.println(in.readUTF());
            out.writeUTF(scanner.nextLine());
            System.out.println(in.readUTF());


            new Thread(() -> {
                try {
                    while (true) {
                        System.out.println(in.readUTF());
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();


            while (true) {
                System.out.println("Enter command (MESSAGE/FILE/EXIT):");
                String command = scanner.nextLine();
                if (command.equalsIgnoreCase("EXIT")) {
                    socket.close();
                    break;
                } else if (command.startsWith("MESSAGE")) {
                    out.writeUTF(command);
                } else if (command.startsWith("FILE")) {
                    System.out.println("Enter file path:");
                    String filePath = scanner.nextLine();
                    File file = new File(filePath);
                    if (file.exists() && file.canRead()) {
                        out.writeUTF(command);
                        out.writeInt((int) file.length());
                        try (FileInputStream fileIn = new FileInputStream(file)) {
                            byte[] buffer = new byte[(int) file.length()];
                            fileIn.read(buffer);
                            out.write(buffer);
                        }
                        System.out.println("File sent successfully.");
                    } else {
                        System.out.println("File not found or access denied.");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
