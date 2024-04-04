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


    public void sendStuff(){
        try {
            ADDRESS = new InetSocketAddress(SERVER_HOST, SERVER_PORT);
            Selector selector = Selector.open();
            DatagramChannel channel = DatagramChannel.open();
            DatagramSocket socket = channel.socket();
            socket.setReuseAddress(true);
            socket.bind(ADDRESS);

            channel.configureBlocking(false);
            channel.bind(ADDRESS);
            channel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

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
                        int bytesRead = channel.read(buffer);
                        if (bytesRead == -1) {
                            channel.close();
                            break;
                        }
                        buffer.flip();
                        System.out.println("Received: " + new String(buffer.array(), 0, bytesRead));
                    }

                    if (key.isWritable()) {
                        String message = "Hello, Server!";
                        buffer.clear();
                        buffer.put(message.getBytes());
                        buffer.flip();
                        channel.write(buffer);
                        System.out.println("Sent: " + message);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
