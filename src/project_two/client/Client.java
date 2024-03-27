package project_two.client;

import project_two.additional.TFTP;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeUnit;

public class Client extends TFTP {
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
        int blockNum = 0;
        try (FileInputStream fis = new FileInputStream(file)) {
            //Make this a variable that is sent at first when a window size is requested.
            // + 4 for block number, should be 2 bytes for tftp, will change later.
            byte[] buffer = new byte[2 + 512];
            int bytesRead;
            while ((bytesRead = fis.read(buffer, 2, 512)) != -1) {
                blockNum++;
                buffer[0] = (byte) ((blockNum >> 8) & 0xFF);
                buffer[1] = (byte) (blockNum & 0xFF);

                ByteBuffer packet = ByteBuffer.wrap(buffer, 0, bytesRead + 2);
                channel.write(packet);
                System.out.println("Packet sent");
            }
        }

        System.out.println("File sent successfully.");
    }



}
