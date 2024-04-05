package project_two.client;

import jdk.jfr.Event;
import project_two.additional.Packet;
import project_two.additional.TFTP;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;

public class Client extends TFTP {
    private String HOST;
    private int PORT;
    TreeMap<Short, Packet> packets = new TreeMap<>();
    HashSet<Short> ackedPackets = new HashSet<>();
    DatagramChannel CHANNEL;
    InetSocketAddress ADDRESS;
    //SWS = Send Window Size
    //LAR = Last Ack Received
    //LFS = Last Frame Sent
    private short SWS = 8, LAR = 0, LFS = 0;
    private boolean DROP_PACKETS = false;


    public static void main(String[] args) throws InterruptedException, IOException {
        Client client = new Client("localhost", 12345);
        client.start("C:/Users/stone/main_dir/suny_oswego/spring_24/csc_445/code/CSC-445/src/project_two/additional/practice_file.txt", 8);
    }
    public Client(String host, int port) throws IOException {
        CHANNEL = DatagramChannel.open();
        CHANNEL.configureBlocking(false);
        this.HOST = host;
        this.PORT = port;
        ADDRESS = new InetSocketAddress(HOST, PORT);
    }

    public void start(String filePath, int windowSize) throws IOException, InterruptedException {
        //load the file and write the data map
        loadFile(filePath);
        while(true){
            ByteBuffer buffer = ByteBuffer.allocate(516);

            short i = 0;
            while (packets.size() != ackedPackets.size()){
                //Have to account for the fact that if packets are dropped, that we go back and send again,
                // but need to keep track of the non-acked frames.
                // change from 99% chance to 50% to replicate this
                if ((LFS - LAR) < SWS){
                    //If within window send frame
                    sendFrame(LFS, buffer, DROP_PACKETS);
                    LFS++;
                    Thread.sleep(10);
                } else {
                    receiveAck(buffer);
                }



                //sendFrame(i, buffer, true);
                //receiveAck();
                //i++;
            }

//            for (short i = 0; i < packets.size(); i++) {
//                sendFrame(i, buffer);
//                Thread.sleep(0, 10);
//                receiveAck();
//            }
            break;
        }

    }

    public void receiveAck(ByteBuffer buffer) throws IOException {

        ByteBuffer ackBuffer = ByteBuffer.allocate(2);
        InetSocketAddress serverAddress = (InetSocketAddress) CHANNEL.receive(ackBuffer);
        if (serverAddress != null) {
            ackBuffer.flip();
            byte[] ackArr = ackBuffer.array();
            short ack = (short) ((ackArr[1] << 8) | (ackArr[0] & 0xff));
            //--------------------------------------------------
            if(ack > LAR && ack <= LFS) {
                ackedPackets.add(ack);
                while ((LAR + 1) <= LFS && ackedPackets.contains(LAR + 1)){
                    LAR++;
                    //ackedPackets.remove(LAR); //Unsure about this
                }
            }
            for (short i = LAR; i <= LFS; i++) {
                if(!ackedPackets.contains(i)){
                    sendFrame(i, buffer, DROP_PACKETS); //Hopefully retransmit un-acked frames
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

}
