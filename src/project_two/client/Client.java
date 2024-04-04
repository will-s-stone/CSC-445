package project_two.client;

import project_two.additional.TFTP;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;

public class Client extends TFTP {
    private String HOST;
    private int PORT;
    TreeMap<Short, byte[]> packets = new TreeMap<>();

    //Could have the DatagramChannel as an instance var up here.
    DatagramChannel CHANNEL;
    InetSocketAddress ADDRESS;
    // Selector selector;

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

    public void start(String filePath, int windowSize) throws IOException {
        //load the file and write the data map
        loadFile(filePath);
        while(true){
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            for (short i = 0; i < packets.size(); i++) {
                sendFrame(i, buffer);
                receiveMessage();
            }
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
        buffer.put(packets.get(blockNum));
        //buffer = ByteBuffer.wrap(data.get(blockNum));
        CHANNEL.send(buffer, ADDRESS);
        System.out.println("Frame #" + blockNum + " ...");
    }



    public void loadFile(String filePath){
        File file = new File(filePath);
        short blockNum = 0;

        try (FileInputStream fis = new FileInputStream(file)) {
            //Make this a variable that is sent at first when a window size is requested.
            // + 2 for block number
            byte[] buffer = new byte[BLOCK_NUM_SIZE + BLOCK_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer, BLOCK_NUM_SIZE, BLOCK_SIZE)) != -1) {

                //buffer[0] = (byte) ((blockNum >> 8) & 0xFF);
                //buffer[1] = (byte) (blockNum & 0xFF);
                ByteBuffer blockNumBuffer = ByteBuffer.allocate(BLOCK_NUM_SIZE);
                blockNumBuffer.putShort(blockNum);
                byte[] bytes = blockNumBuffer.array();


                byte[] dataBlock = Arrays.copyOf(buffer, bytesRead + BLOCK_NUM_SIZE);
                // Copies the blockNum over to the dataBlock[]
                dataBlock[0] = bytes[0];
                dataBlock[1] = bytes[1];
                packets.put(blockNum, dataBlock);
                //ByteBuffer packet = ByteBuffer.wrap(buffer, 0, bytesRead + BLOCK_NUM_SIZE);
                //data.put(blockNum, packet.array().clone());
                blockNum++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }





}
