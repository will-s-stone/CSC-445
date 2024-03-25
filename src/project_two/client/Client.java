package project_two.client;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class Client {
    String host;
    int port;
    public static void main(String[] args) throws InterruptedException, IOException {
        Client client = new Client("localhost", 1234);
        client.sendFile("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt", 1024);

    }
    public Client(String host, int port){
        this.host = host;
        this.port = port;
    }

    public void sendFile(String filePath, int windowSize) throws IOException {
        try (Socket socket = new Socket(host, port);
             FileInputStream fileInputStream = new FileInputStream(filePath);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {

            // Send file name and length to the server
            File file = new File(filePath);
            dataOutputStream.writeUTF(file.getName());
            dataOutputStream.writeLong(file.length());

            // Send file contents to the server in chunks of 1024 bytes
            // When reading from the file, the read(buffer) method will read up to 1024 bytes
            // from the file into the buffer. If the file has fewer than 1024 bytes remaining,
            // it will read the remaining bytes and return the actual number of bytes read.
            // This way, the entire file can be sent in multiple chunks, each up to 1024 bytes in size,
            // without any limitation on the total size of the file.
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }
            dataOutputStream.flush();
            System.out.println("File sent successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
