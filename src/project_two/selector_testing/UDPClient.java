package project_two.selector_testing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

public class UDPClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        try {
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);

            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            // Send a message to the server
            String message = "Hello, server!";
            buffer.put(message.getBytes());
            buffer.flip();
            channel.send(buffer, new InetSocketAddress(SERVER_HOST, SERVER_PORT));

            System.out.println("Message sent to server: " + message);

            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isReadable()) {
                        buffer.clear();
                        DatagramChannel serverChannel = (DatagramChannel) key.channel();
                        serverChannel.receive(buffer);
                        buffer.flip();
                        //String response = new String(buffer.array(), 0, buffer.limit());
                        int response = buffer.getInt();
                        System.out.println("Received message from server: " + response);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

