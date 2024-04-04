package project_two.selector_testing;

import project_two.additional.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class UDPServer {
    private static final int PORT = 1234;
    private static final int BUFFER_SIZE = 1024;

    public void receiveAndSendAck(){
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
                        buffer.clear();
                        Packet packet = new Packet(buffer.array());
                        //String message = new String(buffer.array(), 0, buffer.limit());
                        System.out.println("Received message from " + clientAddress + ": " + Arrays.toString(packet.getData()));

                        // Echo the message back to the client
                        buffer.flip();

                        buffer.clear();
                        System.out.println("Block number: " + packet.getBlockNum());

                        buffer.putShort(packet.getBlockNum());
                        //It's important to clear to reset the position of the buffer because this info is sent as the byte buffer.
                        buffer.clear();
                        clientChannel.send(buffer, clientAddress);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void revisedReceiveAndSendAck(){
        try {
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(PORT));

            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            System.out.println("Server started on port " + PORT);

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
                        DatagramChannel clientChannel = (DatagramChannel) key.channel();
                        SocketAddress clientAddress = clientChannel.receive(buffer);
                        buffer.flip();
                        System.out.println("Received from client: " + new String(buffer.array(), 0, buffer.limit()) + " from " + clientAddress);

                        // Send acknowledgment back to the client
                        buffer.flip();
                        clientChannel.send(buffer, clientAddress);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        UDPServer server = new UDPServer();
        //server.receiveAndSendAck();
        server.revisedReceiveAndSendAck();

    }
}
