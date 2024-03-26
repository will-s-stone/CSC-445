package project_two.server;

import project_two.additional.Packet;
import project_two.additional.TFTP;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeUnit;

public class Server {
    int port;
    public static void main(String[] args) throws InterruptedException, IOException {
        Server server = new Server(1234);
        server.receiveFile("I'll add this later", 69);

    }

    public Server(int port) {
        this.port = port;
    }
    public void receiveFile(String filepath, int windowSize) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.bind(new InetSocketAddress(port));

        while (true) {
            ByteBuffer buffer = ByteBuffer.allocate(512);
            SocketAddress clientAddress = channel.receive(buffer);
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String message = new String(data);
            System.out.println("Received message from " + clientAddress + ": " + message);
        }
    }

}
