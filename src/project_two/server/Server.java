package project_two.server;

import project_two.additional.Packet;
import project_two.additional.TFTP;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class Server extends TFTP {
    int port;
    TreeMap<Short, Packet> packets = new TreeMap<>();
    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 1024;
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
        while(true){
            buffer.clear();
            InetSocketAddress clientAddress = (InetSocketAddress) CHANNEL.receive(buffer);
            if(clientAddress != null){
                buffer.flip();
                Packet packet = new Packet(buffer.array());

                System.out.println("Received message from " + clientAddress + ": " + new String(packet.getData()));

                buffer.clear();

                buffer.flip();

                ByteBuffer ackBuffer = ByteBuffer.wrap(packet.getBlockNumByteArr());
                CHANNEL.send(ackBuffer, clientAddress);
            }
        }
    }


    public void saveFile(){
        try (FileOutputStream fos = new FileOutputStream("src/project_two/output/test_file.txt")) {
            for (short i = 0; i < packets.size(); i++) {
                fos.write(packets.get(i).getData());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
