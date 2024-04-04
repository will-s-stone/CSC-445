package project_two.selector_testing;

import project_two.additional.Packet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class UDPClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;
    private static final int BUFFER_SIZE = 1024;
    //private static final int BLOCK_NUM_SIZE  = ;
    private static final int DATA_BLOCK_SIZE = 512;
    private HashMap<Short, Packet> packets = new HashMap<>();

    public SelectionKey selectionKey;
    public Selector selector;

    public void sendFileAndReceiveAck(String filepath){
        loadFile(filepath);
        try {
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            Selector selector = Selector.open();

            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            for (short i = 0; i < packets.size(); i++) {

                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                // Send a message to the server
                byte[] packet = packets.get(i).getRawFrame();
                buffer.put(packet);
                buffer.flip();
                channel.send(buffer, new InetSocketAddress(SERVER_HOST, SERVER_PORT));
                System.out.println("Packet # " + i + " sent...");

                if(i == packets.size()-1){
                    i = 0;
                }

                // selector.select is evil........
                //selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    System.out.println("Something might have happened");

                    if (key.isReadable()) {
                        buffer.clear();
                        DatagramChannel serverChannel = (DatagramChannel) key.channel();
                        serverChannel.receive(buffer);
                        buffer.flip();
                        //String response = new String(buffer.array(), 0, buffer.limit());
                        short response = buffer.getShort();
                        System.out.println("Received message from server: " + response);
                        //ackReceived = true;
                    }
                    //if (key.isWritable()){
                        //break;
                    //}
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void revisedSendAndReceived(String filepath) {
        //loadFile(filepath);
        try {
            Selector selector = Selector.open();
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            //channel.bind(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            short blockNum = 0;
            while (true) {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) {
                        buffer.clear();
                        SocketAddress serverAddress = channel.receive(buffer);
                        buffer.flip();
                        System.out.println("Received: " + new String(buffer.array(), 0, buffer.limit()) + " from " + serverAddress);
                    }

                    if (key.isWritable()) {
                        String message = "Hello, Server!";
                        buffer.clear();
                        buffer.put(message.getBytes());
                        buffer.flip();
                        channel.send(buffer, new InetSocketAddress(SERVER_HOST, SERVER_PORT));
                        System.out.println("Sent: " + message);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void letsTryThisAgain() throws IOException {
        selector = Selector.open();
        DatagramChannel channel = DatagramChannel.open();
        //channel.connect(InetSocketAddress)

    }

    private void loadFile(String filePath){
        File file = new File(filePath);
        short blockNum = 0;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[512];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                Packet packet = new Packet(chunk, blockNum);
                packets.put(blockNum, packet);
                System.out.println("wowzaaaa " + blockNum);
                blockNum++;
            }
            fis.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        UDPClient client = new UDPClient();
        client.sendFileAndReceiveAck("src/project_two/additional/practice_file.txt");
        //client.revisedSendAndReceived("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt");
    }
}

