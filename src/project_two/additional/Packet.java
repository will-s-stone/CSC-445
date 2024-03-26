package project_two.additional;

import java.io.Serializable;
import java.net.DatagramPacket;

public class Packet extends TFTP{
    private static final long serialVersionID = 1L;
    boolean IS_ACKED = false;
    public byte[] BLOCK_NUM = new byte[2];
    byte[] packetData = null;
    private int seqNum;
    private byte[] data;
    public Packet(byte[] packetData, int nextSeq, byte[] data){
        this.packetData = packetData;
        this.seqNum = seqNum;
        this.data = data;
    }
    public int getSeqNum(){
        return seqNum;
    }
    public byte[] getData() {
        return data;
    }

}
