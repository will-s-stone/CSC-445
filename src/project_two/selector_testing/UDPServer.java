package project_two.selector_testing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

public class UDPServer {
    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        try {
            DatagramChannel channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(PORT));
            channel.configureBlocking(false);

            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);

            System.out.println("Server started on port " + PORT);

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isReadable()) {
                        DatagramChannel clientChannel = (DatagramChannel) key.channel();
                        buffer.clear();
                        InetSocketAddress clientAddress = (InetSocketAddress) clientChannel.receive(buffer);
                        buffer.flip();
                        String message = new String(buffer.array(), 0, buffer.limit());
                        System.out.println("Received message from " + clientAddress + ": " + message);

                        // Echo the message back to the client
                        buffer.flip();
                        buffer.clear();

                        buffer.putInt(69);
                        clientChannel.send(buffer, clientAddress);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
