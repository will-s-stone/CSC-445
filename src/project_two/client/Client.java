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
        client.sendAndReceiveAck("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt", 8);
        //client.loadFile("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt");
    }
    public Client(String host, int port){
        this.host = host;
        this.port = port;
    }

    public void sendFile(String filePath, int windowSize) throws IOException {
        int blockNumLastAckReceived = 0; //seqNum in book
        int blockNumLastFrameSent = 0;
        List<Short> ackedBlocks = new ArrayList<>();
        Selector selector = Selector.open();
        // Selector resource: https://www.baeldung.com/java-nio-selector
        DatagramChannel channel = DatagramChannel.open();
        //channel.configureBlocking(true);
        channel.connect(new InetSocketAddress(host, port));
        channel.configureBlocking(false);

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

            //  \/ Pointless for now but will come in handy with the window size
            if (i < data.values().size()) {
                ByteBuffer packet = ByteBuffer.wrap(packetData);
                channel.write(packet);
                System.out.println("Packet sent");
            }


            //while(true){
                selector.selectNow(); // Non-blocking select?
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();

                while(keyIterator.hasNext()){
                    SelectionKey key = keyIterator.next();
                    if (key.isReadable()){
                        System.out.println("STUFF IS HERE!!!");
                        //keyIterator.remove();
                    }
                    keyIterator.remove();
                }
            //}
//            ByteBuffer ackBuffer = ByteBuffer.allocate(2);
//            channel.read(ackBuffer);
//
//            if (ackBuffer.position() > 0){
//                // Flip from writing to reading :)
//                ackBuffer.flip();
//                short ack = ackBuffer.getShort();
//                System.out.println("Received Ack on Client: " + ack);
//            } else {
//                System.out.println("Nada this go-round");
//            }
//            ackBuffer.clear();
            //i++;
        }
        //}
        //System.out.println("File sent successfully.");
    }

    //public void recieveAcks()

    public void sendAndReceiveAck(String filePath, int windowSize) throws IOException {
        loadFile(filePath);
        int bufferSize = 1024;
        Selector selector = Selector.open();
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(host, port));
        channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ, SelectionKey.OP_WRITE);

        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        while(true) {
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while(keyIterator.hasNext()){
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (!key.isValid()){
                    continue;
                }
                if (key.isConnectable()){
                    System.out.println("Connected to server.");
                }
                // Read what is coming back to us
                if (key.isReadable()){
                    buffer.clear();
                    short bytesRead = (short) channel.read(buffer);
                    if (bytesRead == -1){
                        channel.close();
                        break;
                    }
                    buffer.flip();
                    System.out.println("Received: " + new String(buffer.array(), 0, bytesRead));
                }

                if(key.isWritable()){
                    //Make dynamic with data
                    for (short i = 0; i < windowSize; i++) {
                        byte[] packetData = data.get(i);
                        buffer.clear();
                        buffer.put(packetData);
                        buffer.flip();
                        channel.write(buffer);
                        System.out.println("Packet number sent => " + i);
                    }

                }
            }
        }
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
