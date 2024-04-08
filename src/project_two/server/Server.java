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

    TreeMap<Short, Packet> packets = new TreeMap<>();
    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 516;
    //private InetSocketAddress ADDRESS;
    private DatagramChannel CHANNEL;
    HashSet<Short> ackedPackets = new HashSet<>();
    private short SWS = 8, LAR = -1, LFS = 0;
    private boolean DROP_PACKETS = false;
    private long ENCRYPTION_KEY;

    private String OUTPUT_PATH;

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

                System.out.println("Received message from " + clientAddress + ": " + new String(packet.getData()));

                buffer.clear();

                buffer.flip();

                ByteBuffer ackBuffer = ByteBuffer.wrap(packet.getBlockNumByteArr());
                CHANNEL.send(ackBuffer, clientAddress);

                if (packet.isLastDataPacket()) {
                    System.out.println("AHHHHH");
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
                //Have to account for the fact that if packets are dropped, that we go back and send again,
                // but need to keep track of the non-acked frames.
                // change from 99% chance to 50% to replicate this
                if ((LFS - LAR) < SWS){
                    //If within window send frame
                    sendFrame(LFS, buffer, DROP_PACKETS, clientAddress);
                    LFS++;
                    Thread.sleep(0, 10);
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
        } else {
            System.out.println("It worked????");
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
        System.out.println("Frame #" + blockNum + " Sent...");
    }

    private void loadFile(String filePath) {
        File file = new File(filePath);
        System.out.println(filePath);
        try(FileInputStream fis = new FileInputStream(file)){
            byte[] bytes = fis.readAllBytes();
            //int bytesRead = 0;
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
        InetSocketAddress serverAddress = (InetSocketAddress) CHANNEL.receive(ackBuffer);
        // If something is here, let's get it
        if (serverAddress != null) {
            ackBuffer.flip();
            byte[] ackArr = ackBuffer.array();
            short ack = (short) ((ackArr[1] << 8) | (ackArr[0] & 0xff));
            //--------------------------------------------------
            // If we haven't received it yet, and it is within our window
            if(ack > LAR && ack <= LFS) {
                ackedPackets.add(ack);
                // While the next expected ack is less then or equal to the last frame sent and acked packets contains the ack after the last ack received.
                //short nextAck = (short) (LAR + 1);
                while ((LAR + 1) <= LFS && ackedPackets.contains((short)(LAR + 1))){
                    LAR++;
                    //ackedPackets.remove(LAR); //Unsure about this
                }
            }
            for (short i = LAR; i <= LFS; i++) {
                if(!ackedPackets.contains(i)){
                    if(i == -1){
                        //Edge case that occurs if the firs packet is dropped
                        sendFrame((short) 0, buffer, DROP_PACKETS, clientAddress);
                    } else {
                        sendFrame(i, buffer, DROP_PACKETS, clientAddress); //Hopefully retransmit un-acked frames
                    }
                }
            }
            //--------------------------------------------------
            //Update ack status
            packets.get(ack).ackPacket();
            ackedPackets.add(ack);

            System.out.println("Received ack from: " + ack + ". The status of block number " + ack +  " is " + packets.get(ack).getAckStatus());
        } else {System.out.println("Nothing quite yet"); }
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
