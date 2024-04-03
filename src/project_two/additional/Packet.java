package project_two.additional;

import java.io.Serializable;
import java.net.DatagramPacket;

public class Packet {
    private byte[] data;
    private byte[] rawFrame;
    private byte[] header;
    private short blockNum;
    private boolean acked;
    //For Op Codes
    public enum PACKET_TYPE {READ, WRITE, DATA, ACK, ERROR}
    private PACKET_TYPE packetType;
    public Packet(byte[] rawFrame){
        //Do some stuff in here to get all the data from the raw frame. Eg. op codes, etc.
        this.rawFrame = rawFrame;

        if(rawFrame[0] == 0 && rawFrame[1] == 1){
            packetType = PACKET_TYPE.READ;
        } else if (rawFrame[0] == 0 && rawFrame[1] == 2){
            packetType = PACKET_TYPE.WRITE;
        }else if (rawFrame[0] == 0 && rawFrame[1] == 3){
            packetType = PACKET_TYPE.DATA;
        }else if (rawFrame[0] == 0 && rawFrame[1] == 4){
            packetType = PACKET_TYPE.ACK;
        }else if (rawFrame[0] == 0 && rawFrame[1] == 5){
            packetType = PACKET_TYPE.ERROR;
        } else {
            throw new java.lang.Error("Packet Type Unknown. Refer to Packet Class.");
        }

        this.acked = false;
    }

    public PACKET_TYPE getPacketType(){
        return packetType;
    }

}
