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
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class Server extends TFTP {
    int port;
    public static void main(String[] args) throws InterruptedException, IOException {
        Server server = new Server(1234);
        server.receiveFile("testing_file_123.txt", 69);


    }

    public Server(int port) {
        this.port = port;
    }
    public void receiveFile(String filepath, int windowSize) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.bind(new InetSocketAddress(port));

        TreeMap<Integer, byte[]> packetBuffer = new TreeMap<>();

        while (true) {
            //Make this a variable that is sent at first when a window size is requested.
            ByteBuffer buffer = ByteBuffer.allocate(512 + 2);
            SocketAddress clientAddress = channel.receive(buffer);
            buffer.flip();
            int blockNum = ((buffer.get() & 0xff) << 8) | (buffer.get() & 0xff);
            boolean isLastPacket = buffer.remaining() < (512 + 2);

            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            packetBuffer.put(blockNum, data);

            File file = new File(filepath);
            try(FileOutputStream fos = new FileOutputStream(file, true)) {
                while (packetBuffer.containsKey(blockNum)) {
                    byte[] packetData = packetBuffer.remove(blockNum);
                    fos.write(packetData);
                    if (isLastPacket) {
                        fos.close();
                    }
                    blockNum++;
                }
            }

            //buffer.get(data);
            //Get the block number and then store it in order



            String message = new String(data);
            System.out.println("Received message from " + clientAddress + ": " + message);
        }
    }

}
