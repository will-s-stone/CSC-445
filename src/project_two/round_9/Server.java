package project_two.round_9;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private static final int PORT = 8080;
    public InetSocketAddress clientAddress;
    private int readOrWrite;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.startServer();
    }
    public void startServer() throws IOException {
        // Create Selector and register channel
        clientAddress = new InetSocketAddress(8080);
        Selector selector = Selector.open();

        DatagramChannel channel = DatagramChannel.open();

        channel.bind(clientAddress);

        channel.configureBlocking(false);

        readOrWrite = SelectionKey.OP_READ;
        channel.register(selector, readOrWrite, new SelectorData(clientAddress));
        System.out.println("Server started on port: " + PORT);

        while(true){
            // When this is first ran, it is waiting to read from the channel
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while(iterator.hasNext()){
                SelectionKey key = iterator.next();
                if (key.isReadable()){
                    handleRead(key);
                } else if (key.isWritable()){
                    handleWrite(key);
                }
                iterator.remove();
            }
        }
    }
    public void handleRead(SelectionKey key) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        SelectorData selectorData = (SelectorData) key.attachment();


        selectorData.buffer.clear();    // Prepare buffer for receiving
        selectorData.address = channel.receive(selectorData.buffer);
        if (selectorData.address != null) {  // Did we receive something?
            selectorData.buffer.flip();
            byte[] bytes = new byte[selectorData.buffer.remaining()];
            selectorData.buffer.get(bytes);
            String recievedString = new String(bytes);
            System.out.println(recievedString);

            // Register write with the selector

            //by manipulating this we can select when to read and write
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }
    public void handleWrite(SelectionKey key) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        SelectorData selectorData = (SelectorData) key.attachment();
        //selectorData.address = serverAddress;
        selectorData.buffer.flip(); // Prepare buffer for sending
        int bytesSent = channel.send(selectorData.buffer, selectorData.address);
        if (bytesSent != 0) { // Buffer completely written?
            // No longer interested in writes
            key.interestOps(SelectionKey.OP_READ);

            String response = "Server Class: Something might be working"; // Get the last received string
            System.out.println("OOOOOHHHH");
            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
            channel.send(responseBuffer, selectorData.address);
        }
    }



}
