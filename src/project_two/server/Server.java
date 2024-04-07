package project_two.server;

import project_two.additional.Packet;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.TreeMap;

public class Server{

    TreeMap<Short, Packet> packets = new TreeMap<>();
    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 516;
    private InetSocketAddress ADDRESS;
    private DatagramChannel CHANNEL;

    public Server() throws IOException {
        CHANNEL = DatagramChannel.open();
        CHANNEL.socket().bind(new InetSocketAddress(PORT));
        CHANNEL.configureBlocking(false);
        System.out.println("Server started on port " + PORT);
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        Server server = new Server();
        server.start();
    }

    public void start() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        //ByteBuffer buffer = null;
        while(true){
            buffer.clear();
            InetSocketAddress clientAddress = (InetSocketAddress) CHANNEL.receive(buffer);
            if(clientAddress != null){

                //Dynamically obtain packets rather, needed to hit end condition
                int bytesRead = buffer.position();
                byte[] receivedBytes = new byte[bytesRead];
                buffer.rewind();
                buffer.get(receivedBytes);
                Packet packet = new Packet(receivedBytes);


                packets.put(packet.getBlockNum(), packet);

                System.out.println("Received message from " + clientAddress + ": " + new String(packet.getData()));

                buffer.clear();

                buffer.flip();

                ByteBuffer ackBuffer = ByteBuffer.wrap(packet.getBlockNumByteArr());
                CHANNEL.send(ackBuffer, clientAddress);

                if (packet.isLastDataPacket()) {
                    System.out.println("AHHHHH");
                    saveFile();
                    break;
                }
            }
        }
    }


    public void saveFile(){
        byte[] bytes = new byte[getTotalBytesInPackets()];
        for (short i = 0; i < packets.size(); i++) {
            System.arraycopy(packets.get(i).getData(), 0, bytes, 512*i, packets.get(i).getData().length);
            packets.get(i);
        }
        try (FileOutputStream fos = new FileOutputStream("src/project_two/output/test_file.txt")) {
            fos.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getTotalBytesInPackets(){
        int sum = 0;
        for (short i = 0; i < packets.size(); i++) {
            sum += packets.get(i).getData().length;
        }
        return sum;
    }

}
