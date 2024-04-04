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
    TreeMap<Short, byte[]> packets = new TreeMap<>();
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
                String message = new String(buffer.array(), 0, buffer.limit());
                System.out.println("Received message from " + clientAddress + ": " + message);
                String response = "Server response to " + clientAddress;
                buffer.clear();
                buffer.put(response.getBytes());
                buffer.flip();

                byte[] ack = {0, 7};
                ByteBuffer ackBuffer = ByteBuffer.wrap(ack);
                //CHANNEL.send(ackBuffer, clientAddress);
            }
        }


    }

}
