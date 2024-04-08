package project_two.client;

import project_two.additional.Packet;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Client{
    private int PORT = 27001;
    private String HOST = "localhost";
    private TreeMap<Short, Packet> packets = new TreeMap<>();
    private HashSet<Short> ackedPackets = new HashSet<>();
    private DatagramChannel CHANNEL;
    private InetSocketAddress ADDRESS;
    private static final int BUFFER_SIZE = 516;
    private long ENCRYPTION_KEY;
    private short SWS = 0, LAR = -1, LFS = 0;
    private boolean DROP_PACKETS = false;
    private String OUTPUT_PATH;
    private long TIMEOUT = 10; // milliseconds


    public static void main(String[] args) throws InterruptedException, IOException {
        Client client = new Client();
        client.run();
        //"C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt"
    }
    public Client() throws IOException {
        CHANNEL = DatagramChannel.open();
        CHANNEL.configureBlocking(false);
        ADDRESS = new InetSocketAddress(HOST, PORT);
        System.out.println("Client sending to " + HOST + PORT);
    }

    public void run() throws IOException, InterruptedException {
        //Start everything and figure out what to do...
        //Can stick this whole thing is a while true for testing purposes
        OUTPUT_PATH = "src/project_two/output/test_file.txt";
        Scanner scanner = new Scanner(System.in);
        System.out.println("What would you like to do? \n Your options include: \nWRQ = transfer a file to the server \nRRQ = request a file to be transferred from the server to you");
        String request = scanner.next();
        System.out.println("Please enter an encryption key...");
        ENCRYPTION_KEY = scanner.nextLong();
        sendEncryptionKey();

        if (request.equalsIgnoreCase("WRQ")){
            System.out.println("What file would you like to send?");
            String filename = scanner.next();
            if(filename.equalsIgnoreCase("y")){
                filename = "src/project_two/additional/practice_file.txt";
            }
            System.out.println("What would you like the send window size to be?");
            SWS = scanner.nextShort();

            byte[] WRQ = {0, 2};
            ByteBuffer buffer = ByteBuffer.wrap(WRQ);
            CHANNEL.send(buffer, ADDRESS);
            System.out.println("Would you like to drop 1% of packets? \n Yes or No?");
            String drop = scanner.next();
            if (drop.equalsIgnoreCase("yes")) DROP_PACKETS = true;
            System.out.println("Sent write request...");
            Thread.sleep(100);

            sendDataAndReceiveAck(filename);

        } else if (request.equalsIgnoreCase("RRQ")){
            //We only want to send acks when data comes in...
            //So we transmit the file we want and then receive data packets.
            System.out.println("What file do you want from the server?");
            String filename = scanner.next();
            if (filename.equalsIgnoreCase("y")){
                filename = "src/project_two/additional/practice_file.txt";
            }
            byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
            byte[] opCode = {0, 1};
            byte[] bytes = new byte[filenameBytes.length + opCode.length];

            System.arraycopy(opCode, 0, bytes, 0, opCode.length);
            System.arraycopy(filenameBytes, 0, bytes, opCode.length, filenameBytes.length);

            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            // Send the read request packet
            CHANNEL.send(buffer, ADDRESS);

            receiveDataPackets();
        }

    }
    public void sendEncryptionKey() throws IOException {
        byte[] key = longToByteArr(ENCRYPTION_KEY);
        byte[] keyPacket = new byte[key.length + 2];
        byte[] op = {0,6};
        System.arraycopy(op, 0, keyPacket, 0, 2);
        System.arraycopy(key, 0, keyPacket, 2, key.length);
        ByteBuffer buffer = ByteBuffer.wrap(keyPacket);
        CHANNEL.send(buffer, ADDRESS);
    }
    private static byte[] longToByteArr(long l){
        byte[] longBytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            longBytes[i] = (byte) (l >> (i * 8));
        }
        return longBytes;
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

                //System.out.println("Received message from " + clientAddress + ": " + new String(packet.getData()));

                buffer.clear();

                buffer.flip();

                ByteBuffer ackBuffer = ByteBuffer.wrap(packet.getBlockNumByteArr());
                CHANNEL.send(ackBuffer, clientAddress);

                if (packet.isLastDataPacket()) {
                    //System.out.println("AHHHHH");
                    saveFile();
                    break;
                }
            }
        }
    }

    public void sendDataAndReceiveAck(String filePath) throws IOException, InterruptedException {
        //load the file and write the data map
        loadFile(filePath);
        long timer = System.nanoTime();
        while(true){
            ByteBuffer buffer = ByteBuffer.allocate(516);
            while (packets.size() >= ackedPackets.size()){
                //Have to account for the fact that if packets are dropped, that we go back and send again,
                // but need to keep track of the non-acked frames.
                if ((LFS - LAR) < SWS){
                    //If within window send frame
                    sendFrame(LFS, buffer, DROP_PACKETS);
                    LFS++;
                    Thread.sleep(TIMEOUT);
                } else {
                    if(ackedPackets.size() != packets.size()){
                        receiveAck(buffer);
                    } else break;
                }
            }
            timer = System.nanoTime()-timer;
            long bits = getTotalBytesInPackets() * 8L;

            long throughput = bits / (timer / 1000000000);
            System.out.println("Throughput: " + throughput + " bits per second \nWindow size of: "  + SWS + "\nDropped packets: " + DROP_PACKETS);
            break;
        }
    }

    public void receiveAck(ByteBuffer buffer) throws IOException, InterruptedException {
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
            System.out.println("Received ack from: " + ack + ". The status of block number " + ack +  " is " + packets.get(ack).getAckStatus());
        }

        // Slide the window if possible
        while (ackedPackets.contains((short)(LAR + 1))) {
            LAR++;
        }
        System.out.println("LAR is now => " + LAR);

        // Retransmit un-acked frames
        for (short i = LAR; i <= LFS; i++) {
            if (!ackedPackets.contains(i)) {
                if (i == -1) {
                    // Edge case that occurs if the first packet is dropped
                    sendFrame((short) 0, buffer, DROP_PACKETS);
                    Thread.sleep(TIMEOUT);
                } else {
                    sendFrame(i, buffer, DROP_PACKETS); // Hopefully retransmit un-acked frames
                    Thread.sleep(TIMEOUT);
                }
            }
        }
    }

    public void sendFrame(short blockNum, ByteBuffer buffer, boolean dropsEnable) throws IOException {

        if(!dropsEnable){
            sendFrameNoDrop(blockNum, buffer);
            return;
        }
        int randomNumber = new Random().nextInt(100);
        if(randomNumber < 99){
            sendFrameNoDrop(blockNum, buffer);
        } else {
            System.out.println("Dropped packet #" + blockNum);
        }
        return;
    }
    public void sendFrameNoDrop(short blockNum, ByteBuffer buffer) throws IOException {
        //If we have sent all the packets, return.
        if(blockNum >= packets.size()){
            return;
        }
        buffer.clear();

        byte[] encryptedData = encrypt(packets.get(blockNum).getRawFrame(), ENCRYPTION_KEY);

        buffer = ByteBuffer.wrap(encryptedData);

        buffer.clear();

        CHANNEL.send(buffer, ADDRESS);
        System.out.println("Frame #" + blockNum + " Sent...");
    }


    private void loadFile(String filePath) {
        File file = new File(filePath);
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


    // For processing read requests
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
