package project_two.server;

import project_two.additional.Packet;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class Server {
    int port;
    public static void main(String[] args) throws InterruptedException, IOException {
        Server server = new Server(1234);
        server.receiveFile("I'll add this later", 69);


    }

    public Server(int port) {
        this.port = port;
    }
    public void receiveFile(String filepath, int windowSize) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port);
             Socket socket = serverSocket.accept();
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {

            // Receive file name and length from the client
            String fileName = dataInputStream.readUTF();
            long fileLength = dataInputStream.readLong();

            // Create a new file on the server
            File file = new File("received_" + fileName);
            try (FileOutputStream fileOutputStream = new FileOutputStream(file);
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

                // Receive file contents from the client and write to the file
                byte[] buffer = new byte[1024];
                int bytesRead;
                while (fileLength > 0 && (bytesRead = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileLength))) != -1) {
                    bufferedOutputStream.write(buffer, 0, bytesRead);
                    fileLength -= bytesRead;
                }
                bufferedOutputStream.flush();
                System.out.println("File received successfully.");

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
