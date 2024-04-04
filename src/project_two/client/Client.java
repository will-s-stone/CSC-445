package project_two.client;

import project_two.additional.Packet;
import project_two.additional.TFTP;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;

public class Client extends TFTP {
    private String HOST;
    private int PORT;
    TreeMap<Short, Packet> packets = new TreeMap<>();

    //Could have the DatagramChannel as an instance var up here.
    DatagramChannel CHANNEL;
    InetSocketAddress ADDRESS;


    public static void main(String[] args) throws InterruptedException, IOException {
        Client client = new Client("localhost", 12345);
        client.start("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt", 8);
        //client.loadFile("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt");
    }
    public Client(String host, int port) throws IOException {
        CHANNEL = DatagramChannel.open();
        CHANNEL.configureBlocking(false);
        this.HOST = host;
        this.PORT = port;
        ADDRESS = new InetSocketAddress(HOST, PORT);
    }

    public void start(String filePath, int windowSize) throws IOException, InterruptedException {
        //load the file and write the data map
        loadFile(filePath);
        while(true){
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            for (short i = 0; i < packets.size(); i++) {
                sendFrame(i, buffer);
                Thread.sleep(0, 10);
                receiveMessage();
            }
            break;
        }

    }

    public void receiveMessage() throws IOException {
        ByteBuffer ackBuffer = ByteBuffer.allocate(2);
        InetSocketAddress serverAddress = (InetSocketAddress) CHANNEL.receive(ackBuffer);
        if (serverAddress != null) {
            ackBuffer.flip();
            short ack = ackBuffer.getShort();
            System.out.println("Received ack from: " + ack);
        } else {System.out.println("Nothing quite yet"); }
    }

    public void sendFrame(short blockNum, ByteBuffer buffer) throws IOException {
        buffer.clear();
        buffer.put(packets.get(blockNum).getRawFrame());
        //buffer = ByteBuffer.wrap(data.get(blockNum));
        buffer.clear();
        CHANNEL.send(buffer, ADDRESS);
        System.out.println("Frame #" + blockNum + " ...");
    }



    private void loadFile(String filePath) {
        File file = new File(filePath);
        short blockNum = 0;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[512];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1 ) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                Packet packet = new Packet(chunk, blockNum);
                packets.put(blockNum, packet);
                System.out.println("Loaded in block number: " + blockNum);
                blockNum++;
            }
            fis.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }





}
