package project_two.client;

import project_two.additional.TFTP;

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
        client.sendFile("src/project_two/additional/practice_file.txt", 1024);

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
            byte[] buffer = new byte[512];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                ByteBuffer packet = ByteBuffer.wrap(buffer, 0, bytesRead);
                channel.write(packet);


            }
        }

        System.out.println("File sent successfully.");
    }

    public byte[] buildDataFrame(byte[] frameData){
        byte[] dataFrame = new byte[frameData.length];
        return dataFrame;
    }


}
