package project_two.client;

import project_two.additional.TFTP;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Client extends TFTP {
    String host;
    int port;
    TreeMap<Short, byte[]> data = new TreeMap<>();

    //Could have the DatagramChannel as an instance var up here.
    // DatagramChannel channel;
    // Selector selector;



    public static void main(String[] args) throws InterruptedException, IOException {
        Client client = new Client("localhost", 1234);
        client.sendFile("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt", 8);
        //client.loadFile("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt");
        System.out.println();
    }
    public Client(String host, int port){
        this.host = host;
        this.port = port;
    }

    public void sendFile(String filePath, int windowSize) throws IOException {
        int blockNumLastAckReceived = 0; //seqNum in book
        int blockNumLastFrameSent = 0;
        List<Short> ackedBlocks = new ArrayList<>();

        // Selector resource: https://www.baeldung.com/java-nio-selector
        DatagramChannel channel = DatagramChannel.open();
        //channel.configureBlocking(true);
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(host, port));
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);

        loadFile(filePath);
        int i = 0;

//        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
//        while (iterator.hasNext()) {
//            SelectionKey key = iterator.next();
//            iterator.remove();
//            if (key.isReadable()) {
//
//                ByteBuffer buffer = ByteBuffer.allocate(2);
//                channel.receive(buffer);
//                buffer.flip();
//
//                short blockNum = buffer.getShort();
//                ackedBlocks.add(blockNum);
//
//                //ackedBlocks.get(buffer.getShort());
//                System.out.println("Received Ack " + buffer.getShort());
//            }
//            key.cancel();
//        }

        for (byte[] packetData : data.values()) {
            // if(i < data.values().size()) won't be needed if I keep track of SWS, LAR, and LFS
            // Put Iterator block here?

            if (i < data.values().size()) {
                ByteBuffer packet = ByteBuffer.wrap(packetData);
                channel.write(packet);
                System.out.println("Packet sent");

            }
            i++;
        }
        //}
        //System.out.println("File sent successfully.");
    }

    //public void recieveAcks()

    public void loadFile(String filePath){
        File file = new File(filePath);
        short blockNum = 0;

        try (FileInputStream fis = new FileInputStream(file)) {
            //Make this a variable that is sent at first when a window size is requested.
            // + 2 for block number
            byte[] buffer = new byte[BLOCK_NUM_SIZE + BLOCK_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer, BLOCK_NUM_SIZE, BLOCK_SIZE)) != -1) {
                blockNum++;
                //buffer[0] = (byte) ((blockNum >> 8) & 0xFF);
                //buffer[1] = (byte) (blockNum & 0xFF);
                ByteBuffer blockNumBuffer = ByteBuffer.allocate(BLOCK_NUM_SIZE);
                blockNumBuffer.putShort(blockNum);
                byte[] bytes = blockNumBuffer.array();


                byte[] dataBlock = Arrays.copyOf(buffer, bytesRead + BLOCK_NUM_SIZE);
                // Copies the blockNum over to the dataBlock[]
                dataBlock[0] = bytes[0];
                dataBlock[1] = bytes[1];
                data.put(blockNum, dataBlock);
                //ByteBuffer packet = ByteBuffer.wrap(buffer, 0, bytesRead + BLOCK_NUM_SIZE);
                //data.put(blockNum, packet.array().clone());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }





}
