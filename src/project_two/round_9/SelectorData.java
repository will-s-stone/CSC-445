package project_two.round_9;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class SelectorData {
    // Can change so it extends TFTP
    public ByteBuffer buffer;
    public SocketAddress address;

    public SelectorData(SocketAddress address){
        this.address = address;
        buffer = ByteBuffer.allocate(1024);
    }

}
