package project_two.round_9;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class TCPServer {
    private static final int SERVER_PORT = 12345;
    private static final int BUFFER_SIZE = 1024;
    private InetSocketAddress ADDRESS;
    private DatagramChannel CHANNEL;

    public TCPServer() throws IOException {
        CHANNEL = DatagramChannel.open();
        CHANNEL.socket().bind(new InetSocketAddress(SERVER_PORT));
        CHANNEL.configureBlocking(false);
        System.out.println("Server started on port " + SERVER_PORT);
    }
    public static void main(String[] args) throws IOException {
        TCPServer server = new TCPServer();
        server.receiveStuffV2();
    }

    public void receiveStuffV2() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (true) {
            buffer.clear();
            InetSocketAddress clientAddress = (InetSocketAddress) CHANNEL.receive(buffer);
            if (clientAddress != null) {
                buffer.flip();
                String message = new String(buffer.array(), 0, buffer.limit());
                System.out.println("Received message from " + clientAddress + ": " + message);
                String response = "Server response to " + clientAddress;
                buffer.clear();
                buffer.put(response.getBytes());
                buffer.flip();
                //CHANNEL.send(buffer, clientAddress);
                //CHANNEL.send(buffer, clientAddress);
            }
        }

    }
    public void receiveStuff(){
        try {
            ADDRESS = new InetSocketAddress(SERVER_PORT);
            Selector selector = Selector.open();
            DatagramChannel channel = DatagramChannel.open();
            DatagramSocket socket = channel.socket();
            socket.setReuseAddress(true);
            socket.bind(ADDRESS);

            channel.configureBlocking(false);
            channel.bind(ADDRESS);
            int mode = SelectionKey.OP_READ;
            channel.register(selector, mode);

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
                        DatagramChannel clientChannel = (DatagramChannel) key.channel();
                        buffer.clear();
                        int bytesRead = channel.read(buffer);
                        if (bytesRead == -1) {
                            clientChannel.close();
                            break;
                        }
                        buffer.flip();
                        System.out.println("Received from client: " + new String(buffer.array(), 0, bytesRead));
                        mode = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
                    }

                    if (key.isWritable()) {
                        DatagramChannel clientChannel = (DatagramChannel) key.channel();
                        String message = "Hello, Client!";
                        buffer.clear();
                        buffer.put(message.getBytes());
                        buffer.flip();
                        clientChannel.write(buffer);
                        System.out.println("Sent to client: " + message);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
