package project_two.client;

import project_two.additional.TFTP;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class Client extends TFTP {
    String host;
    int port;
    TreeMap<Integer, byte[]> data = new TreeMap<>();

    public static void main(String[] args) throws InterruptedException, IOException {
        Client client = new Client("localhost", 1234);
        client.sendFile("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt", 1024);
        //client.loadFile("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt");
        System.out.println();
    }
    public Client(String host, int port){
        this.host = host;
        this.port = port;
    }

    public void sendFile(String filePath, int windowSize) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(true);
        channel.connect(new InetSocketAddress(host, port));
        loadFile(filePath);
        for(byte[] packetData : data.values()){
            ByteBuffer packet = ByteBuffer.wrap(packetData);
            channel.write(packet);
            System.out.println("Packet sent");
        }

        System.out.println("File sent successfully.");
    }

    //public void recieveAcks()

    public void loadFile(String filePath){
        File file = new File(filePath);
        int blockNum = 0;

        try (FileInputStream fis = new FileInputStream(file)) {
            //Make this a variable that is sent at first when a window size is requested.
            // + 2 for block number
            byte[] buffer = new byte[BLOCK_NUM_SIZE + BLOCK_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer, BLOCK_NUM_SIZE, BLOCK_SIZE)) != -1) {
                blockNum++;
                buffer[0] = (byte) ((blockNum >> 8) & 0xFF);
                buffer[1] = (byte) (blockNum & 0xFF);

                ByteBuffer packet = ByteBuffer.wrap(buffer, 0, bytesRead + BLOCK_NUM_SIZE);
                data.put(blockNum, packet.array().clone());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }





}
