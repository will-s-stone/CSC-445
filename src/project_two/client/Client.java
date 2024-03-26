package project_two.client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
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
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(true);
        channel.connect(new InetSocketAddress(host, port));

        File file = new File(filePath);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                ByteBuffer packet = ByteBuffer.wrap(buffer, 0, bytesRead);
                channel.write(packet);
                Thread.sleep(50); // Simulate network delay
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("File sent successfully.");
    }


}
