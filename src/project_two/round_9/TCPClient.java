package project_two.round_9;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

public class TCPClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int BUFFER_SIZE = 1024;
    private InetSocketAddress ADDRESS;
    private DatagramChannel CHANNEL;
    public TCPClient() throws IOException {
        CHANNEL = DatagramChannel.open();
        CHANNEL.configureBlocking(false);
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        TCPClient client = new TCPClient();
        client.sendMessage("Hello", "localhost",12345);
        Thread.sleep(1000);
        client.receiveMessage();
        //client.receiveMessage();
    }


    public void sendMessage(String message, String serverAddress, int serverPort) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        CHANNEL.send(buffer, new InetSocketAddress(serverAddress, serverPort));
        System.out.println("Sent message to server: " + message);
    }

    public String receiveMessage() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        InetSocketAddress serverAddress = (InetSocketAddress) CHANNEL.receive(buffer);
        if (serverAddress != null) {
            buffer.flip();
            String message = new String(buffer.array(), 0, buffer.limit());
            System.out.println("Received message from server: " + message);
            return message;
        }
        System.out.println("Heyyyy");
        return null;

    }

}
