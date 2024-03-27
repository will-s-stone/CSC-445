package project_two.additional;

import java.io.Serializable;
import java.net.DatagramPacket;

public class Packet extends TFTP implements Serializable{
    private static final long serialVersionID = 1L;
    private int seqNum;
    private byte[] data;
    public Packet(byte[] opCode, int seqNum, byte[] data){
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
