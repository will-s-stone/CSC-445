package project_two.additional;

import java.io.Serializable;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Packet {
    private byte[] data;
    private byte[] rawFrame;
    //private byte[] header;
    private short blockNum;
    private boolean acked;
    //For Op Codes
    public enum PACKET_TYPE {READ, WRITE, DATA, ACK, ERROR}
    private String filename;
    private PACKET_TYPE packetType;

    //The idea here is everytime a packet is sent or received, I build a packet object with the raw data then interact with the class methods for setting and getting.
    public Packet(byte[] rawFrame){
        //Do some stuff in here to get all the data from the raw frame. Eg. op codes, etc.
        //No need to account for modes as we are only supporting octet transmission.
        this.rawFrame = rawFrame;

        if(rawFrame[0] == 0 && rawFrame[1] == 1){
            this.packetType = PACKET_TYPE.READ;
            //256 byte filename, issues may arise with encoding
            byte[] filenameArr = Arrays.copyOfRange(rawFrame, 2, 258);
            this.filename = new String(filenameArr);
        } else if (rawFrame[0] == 0 && rawFrame[1] == 2){
            this.packetType = PACKET_TYPE.WRITE;
        }else if (rawFrame[0] == 0 && rawFrame[1] == 3){
            this.packetType = PACKET_TYPE.DATA;
            byte[] blockNumArr = {rawFrame[2], rawFrame[3]};
            ByteBuffer buffer = ByteBuffer.wrap(blockNumArr);
            this.blockNum = buffer.getShort();
            this.data = Arrays.copyOfRange(rawFrame, 4, 516);
        }else if (rawFrame[0] == 0 && rawFrame[1] == 4){
            packetType = PACKET_TYPE.ACK;
            byte[] blockNumArr = {rawFrame[2], rawFrame[3]};
            ByteBuffer buffer = ByteBuffer.wrap(blockNumArr);
            this.blockNum = buffer.getShort();
        }else if (rawFrame[0] == 0 && rawFrame[1] == 5){
            packetType = PACKET_TYPE.ERROR;
            throw new java.lang.Error("Packet Type is 'Error'. Refer to Packet Class.");
        } else {
            throw new java.lang.Error("Packet Type Unknown. Refer to Packet Class.");
        }
        this.acked = false;
    }

    public PACKET_TYPE getPacketType(){
        return packetType;
    }
    public boolean getAckStatus(){
        if(packetType == PACKET_TYPE.DATA){
            return acked;
        } else {
            // There's an edge case here, if I check a non-DATA packet for ack status...
            return false;
        }
    }
    public void ackPacket(){
        acked = true;
    }
    public byte[] getData(){
        return data;
    }
    public byte[] getRawFrame(){
        return rawFrame;
    }
    public short getBlockNum(){
        return blockNum;
    }
    public String getFilename(){
        return filename;
    }


}
