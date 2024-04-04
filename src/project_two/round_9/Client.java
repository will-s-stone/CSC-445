package project_two.round_9;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

public class Client {
    public InetSocketAddress serverAddress;
    private int readOrWrite;

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.processKey();
    }
    public void processKey() throws IOException {
        serverAddress = new InetSocketAddress("localhost", 8080);
        Selector selector = Selector.open();

        DatagramChannel channel = DatagramChannel.open();
        //DatagramChannel readChannel = DatagramChannel.open();

        channel.connect(serverAddress);
        //readChannel.connect(serverAddress);

        channel.configureBlocking(false);
        //readChannel.configureBlocking(false);

        readOrWrite = SelectionKey.OP_WRITE;
        channel.register(selector, readOrWrite, new SelectorData(serverAddress));
        //readChannel.register(selector, SelectionKey.OP_READ, new SelectorData(serverAddress));


        // Continuously wait for something to happen
        while(true){
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while(iterator.hasNext()){
                SelectionKey key = iterator.next();

                if (key.isReadable()){
                    handleRead(key);
                } else if (key.isWritable()){
                    handleWrite(key);
                    //flipReadOrWrite();
                }
                iterator.remove();
            }
        }
    }

    public void flipReadOrWrite(){
        if (readOrWrite == SelectionKey.OP_READ){
            readOrWrite = SelectionKey.OP_WRITE;
        } else if (readOrWrite == SelectionKey.OP_WRITE){
            readOrWrite = SelectionKey.OP_READ;
        } else {
            System.out.println("Error in flipReadOrWrite()");
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
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    public void handleWrite(SelectionKey key) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        SelectorData selectorData = (SelectorData) key.attachment();
        //selectorData.address = serverAddress;
        selectorData.buffer.flip(); // Prepare buffer for sending
        int bytesSent = channel.send(selectorData.buffer, selectorData.address);

        //by manipulating this we can select when to read and write
        key.interestOps(SelectionKey.OP_READ);

        String response = "Client Class: Something might be working"; // Get the last received string
        System.out.println("OOOOOHHHH");
        ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
        channel.send(responseBuffer, selectorData.address);
    }
}
