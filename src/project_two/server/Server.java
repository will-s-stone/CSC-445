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
        server.receiveFile("testing_file_123.txt", 8);
    }

    public Server(int port) {
        this.port = port;
    }
    public void receiveFile(String filepath, int windowSize) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.bind(new InetSocketAddress(port));

        TreeMap<Short, byte[]> packetBuffer = new TreeMap<>();

        while (true) {
            //Make this a variable that is sent at first when a window size is requested.
            ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE + BLOCK_NUM_SIZE);
            SocketAddress clientAddress = channel.receive(buffer);
            buffer.flip();
            short blockNum = (short) (((buffer.get() & 0xff) << 8) | (buffer.get() & 0xff));
            System.out.println("Block: " + blockNum);
            boolean isLastPacket = buffer.remaining() < (BLOCK_SIZE + BLOCK_NUM_SIZE);

            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            packetBuffer.put(blockNum, data);

            File file = new File(filepath);
            try(FileOutputStream fos = new FileOutputStream(file, true)) {
                // This loop seems problematic
                while (packetBuffer.containsKey(blockNum)) {
                    byte[] packetData = packetBuffer.remove(blockNum);
                    fos.write(packetData);

                    // Send the ack back, in this case it's just the block number/sequence number of the last frame received
                    ByteBuffer ack = ByteBuffer.allocate(2);
                    ack.putShort(blockNum);
                    ack.flip();

                    ack.position(0);
                    channel.send(ack, clientAddress);
                    System.out.println("\n\n Ack sent => " + blockNum);

                    //if (isLastPacket) {fos.close();}
                    blockNum++;
                }
                if (isLastPacket) {fos.close();}
            }

            String message = new String(data);
            System.out.println("Received message from " + clientAddress + ": " + message);
        }
    }

}
