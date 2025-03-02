package project_two.server;

import project_two.additional.Packet;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;

public class Server{
    private static final int PORT = 27001;
    private TreeMap<Short, Packet> packets = new TreeMap<>();
    private HashSet<Short> ackedPackets = new HashSet<>();
    private DatagramChannel CHANNEL;
    private static final int BUFFER_SIZE = 516;
    private long ENCRYPTION_KEY;
    private short SWS = 8, LAR = -1, LFS = 0;
    private boolean DROP_PACKETS = false;
    private String OUTPUT_PATH;
    private long TIMEOUT = 5; //milliseconds

    public Server() throws IOException {
        CHANNEL = DatagramChannel.open();
        CHANNEL.socket().bind(new InetSocketAddress(PORT));
        CHANNEL.configureBlocking(false);
        System.out.println("Server started on port " + PORT);
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        Server server = new Server();
        server.run();
    }

    public void run() throws IOException, InterruptedException {
        // Wait for something to happen
        //We can handle this in a linear fashion by assuming that the first packet we get will either be a rrq or a wrq
        OUTPUT_PATH = "src/project_two/output/test_file.txt";

        while(true){
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            InetSocketAddress clientAddress = (InetSocketAddress) CHANNEL.receive(buffer);

            if(clientAddress != null){
                handleKey();
                //Convert byte[] to packet, then process according to wrq or rrq
                //Dynamically obtain packets rather, needed to hit end condition

                int bytesRead = buffer.position();
                byte[] receivedBytes = new byte[bytesRead];
                buffer.rewind();
                buffer.get(receivedBytes);

                Packet packet = new Packet(receivedBytes);

                if (packet.getPacketType() == Packet.PACKET_TYPE.KEY){
                    ENCRYPTION_KEY = packet.getKey();
                }

                if(packet.getPacketType() == Packet.PACKET_TYPE.READ){
                    //Newish stuff
                    sendDataAndReceiveAck(packet.getFilename(), clientAddress);
                    break;
                } else if (packet.getPacketType() == Packet.PACKET_TYPE.WRITE) {
                    receiveDataPackets();
                    break;
                }
            }
        }
    }

    public void handleKey(){
        ByteBuffer keyBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    }

    public void receiveDataPackets() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        //ByteBuffer buffer = null;
        while(true){
            buffer.clear();
            InetSocketAddress clientAddress = (InetSocketAddress) CHANNEL.receive(buffer);
            if(clientAddress != null){

                //Dynamically obtain packets rather, needed to hit end condition
                int bytesRead = buffer.position();
                byte[] receivedBytes = new byte[bytesRead];
                buffer.rewind();
                buffer.get(receivedBytes);
                byte[] decryptedBytes = decrypt(receivedBytes, ENCRYPTION_KEY);
                Packet packet = new Packet(decryptedBytes);
                packets.put(packet.getBlockNum(), packet);
                buffer.clear();
                buffer.flip();

                ByteBuffer ackBuffer = ByteBuffer.wrap(packet.getBlockNumByteArr());
                CHANNEL.send(ackBuffer, clientAddress);

                if (packet.isLastDataPacket() && packet.getBlockNum() == (short)(packets.size()-1)) {
                    saveFile();
                    break;
                }
            }
        }
    }


    public void saveFile(){
        byte[] bytes = new byte[getTotalBytesInPackets()];
        for (short i = 0; i < packets.size(); i++) {
            System.arraycopy(packets.get(i).getData(), 0, bytes, 512*i, packets.get(i).getData().length);
            packets.get(i);
        }
        try (FileOutputStream fos = new FileOutputStream(OUTPUT_PATH)) {
            fos.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getTotalBytesInPackets(){
        int sum = 0;
        for (short i = 0; i < packets.size(); i++) {
            sum += packets.get(i).getData().length;
        }
        return sum;
    }


    public void sendDataAndReceiveAck(String filePath, InetSocketAddress clientAddress) throws IOException, InterruptedException {
        //load the file and write the data map
        loadFile(filePath);
        while(true){
            ByteBuffer buffer = ByteBuffer.allocate(516);
            while (packets.size() != ackedPackets.size()){
                if ((LFS - LAR) < SWS){
                    //If within window send frame
                    sendFrame(LFS, buffer, DROP_PACKETS, clientAddress);
                    LFS++;
                    Thread.sleep(TIMEOUT);
                } else {
                    receiveAck(buffer, clientAddress);
                }
            }
            break;
        }
    }
    public void sendFrame(short blockNum, ByteBuffer buffer, boolean dropsEnable, InetSocketAddress clientAddress) throws IOException {

        if(!dropsEnable){
            sendFrameNoDrop(blockNum, buffer, clientAddress);
            return;
        }
        int randomNumber = new Random().nextInt(100);
        if(randomNumber < 99){
            sendFrameNoDrop(blockNum, buffer, clientAddress);
        }
        return;
    }
    public void sendFrameNoDrop(short blockNum, ByteBuffer buffer, InetSocketAddress clientAddress) throws IOException {
        //If we have sent all the packets, return.
        if(blockNum >= packets.size()){
            return;
        }
        buffer.clear();

        byte[] encryptedData = encrypt(packets.get(blockNum).getRawFrame(), ENCRYPTION_KEY);

        buffer = ByteBuffer.wrap(encryptedData);;

        buffer.clear();

        CHANNEL.send(buffer, clientAddress);
    }

    private void loadFile(String filePath) {
        File file = new File(filePath);
        System.out.println(filePath);
        try(FileInputStream fis = new FileInputStream(file)){
            byte[] bytes = fis.readAllBytes();
            int chunkSize = 512; // chunk size to divide
            short blockNum = 0;
            for(int i = 0; i < bytes.length; i += chunkSize){
                int newChunkSize = Math.min(bytes.length, i+chunkSize);
                byte[] chunk = Arrays.copyOfRange(bytes, i, newChunkSize);
                Packet packet = new Packet(chunk, blockNum);
                packets.put(blockNum, packet);
                blockNum ++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void receiveAck(ByteBuffer buffer, InetSocketAddress clientAddress) throws IOException {
        // Allocate 2 bytes to receive the ack, from here we add it to acked packets and update variables.
        ByteBuffer ackBuffer = ByteBuffer.allocate(2);
        InetSocketAddress serverAddress;
        while ((serverAddress = (InetSocketAddress) CHANNEL.receive(ackBuffer)) != null) {
            ackBuffer.flip();
            byte[] ackArr = ackBuffer.array();
            short ack = (short) ((ackArr[1] << 8) | (ackArr[0] & 0xff));

            // Update ack status
            packets.get(ack).ackPacket();
            ackedPackets.add(ack);
        }
        while (ackedPackets.contains((short)(LAR + 1))) {
            LAR++;
        }

        for (short i = LAR; i <= LFS; i++) {
            if (!ackedPackets.contains(i)) {
                if (i == -1) {
                    //Edge case that occurs if the firs packet is dropped
                    sendFrame((short) 0, buffer, DROP_PACKETS, clientAddress);
                } else {
                    sendFrame(i, buffer, DROP_PACKETS, clientAddress); //Hopefully retransmit un-acked frames
                }
            }
        }
    }
    public static byte[] encrypt(byte[] data, long key) {
        byte[] encrypted = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            encrypted[i] = (byte) (data[i] ^ (key & 0xFF));
            key = (key >> 8) | ((key & 0xFF) << 56);
        }
        return encrypted;
    }

    public static byte[] decrypt(byte[] encryptedData, long key) {
        return encrypt(encryptedData, key);
    }


}
