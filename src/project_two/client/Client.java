package project_two.client;

import project_two.additional.Packet;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Client{
    private String HOST;
    private int PORT;
    private static final int BUFFER_SIZE = 516;
    TreeMap<Short, Packet> packets = new TreeMap<>();
    HashSet<Short> ackedPackets = new HashSet<>();
    DatagramChannel CHANNEL;
    InetSocketAddress ADDRESS;
    //SWS = Send Window Size
    //LAR = Last Ack Received
    //LFS = Last Frame Sent
    private short SWS = 13, LAR = -1, LFS = 0;
    private boolean DROP_PACKETS = true;


    public static void main(String[] args) throws InterruptedException, IOException {
        Client client = new Client("localhost", 12345);
        client.run();
        //"C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt"
    }
    public Client(String host, int port) throws IOException {
        CHANNEL = DatagramChannel.open();
        CHANNEL.configureBlocking(false);
        this.HOST = host;
        this.PORT = port;
        ADDRESS = new InetSocketAddress(HOST, PORT);
    }

    public void run() throws IOException, InterruptedException {
        //Start everything and figure out what to do...
        //Can stick this whole thing is a while true for testing purposes
        Scanner scanner = new Scanner(System.in);
        System.out.println("What would you like to do");
        String request = scanner.next();
        if (request.equals("WRQ")){
            System.out.println("Filename?");
            String filename = scanner.next();
            //Should implement something to wait until the server responds
            byte[] WRQ = {0, 2};
            ByteBuffer buffer = ByteBuffer.wrap(WRQ);
            CHANNEL.send(buffer, ADDRESS);
            System.out.println("Sent write request...");
            Thread.sleep(1000);

            System.out.println("DIAMONDS ARE MADE UNDER PRESSURE");
            sendDataAndReceiveAck(filename);

        } else if (request.equals("RRQ")){
            //We only want to send acks when data comes in...
            //So we transmit the file we want and then receive data packets.
            System.out.println("What file do you want from the server?");
            String filename = scanner.next();
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
                Packet packet = new Packet(receivedBytes);


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

    public void sendDataAndReceiveAck(String filePath) throws IOException, InterruptedException {
        //load the file and write the data map
        loadFile(filePath);
        while(true){
            ByteBuffer buffer = ByteBuffer.allocate(516);
            while (packets.size() != ackedPackets.size()){
                //Have to account for the fact that if packets are dropped, that we go back and send again,
                // but need to keep track of the non-acked frames.
                if ((LFS - LAR) < SWS){
                    //If within window send frame
                    sendFrame(LFS, buffer, DROP_PACKETS);
                    LFS++;
                    Thread.sleep(10);
                } else {
                    receiveAck(buffer);
                }
            }
            break;
        }
    }

    public void receiveAck(ByteBuffer buffer) throws IOException {
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
                        sendFrame((short) 0, buffer, DROP_PACKETS);
                    } else {
                        sendFrame(i, buffer, DROP_PACKETS); //Hopefully retransmit un-acked frames
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

    public void sendFrame(short blockNum, ByteBuffer buffer, boolean dropsEnable) throws IOException {

        if(!dropsEnable){
            sendFrameNoDrop(blockNum, buffer);
            return;
        }
        int randomNumber = new Random().nextInt(100);
        if(randomNumber < 99){
            sendFrameNoDrop(blockNum, buffer);
        } else {
            System.out.println("It worked????");
        }
        return;
    }
    public void sendFrameNoDrop(short blockNum, ByteBuffer buffer) throws IOException {
        //If we have sent all the packets, return.
        if(blockNum >= packets.size()){
            return;
        }
        buffer.clear();

        buffer = ByteBuffer.wrap(packets.get(blockNum).getRawFrame());

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
        try (FileOutputStream fos = new FileOutputStream("src/project_two/output/test_file.txt")) {
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

}
