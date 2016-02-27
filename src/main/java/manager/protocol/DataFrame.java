package manager.protocol;

import io.netty.buffer.ByteBuf;
import manager.protocol.Header.OperationType;

public class DataFrame extends Frame {
    
    private ByteBuf payload;
    
    public DataFrame(Header header, ByteBuf payload) {
        super(header);
        this.payload = payload;
    }
    
    public DataFrame(int sId, String ip, int port, ByteBuf payload) {
        super(OperationType.DATA, sId, ip, port, payload.readableBytes());
        this.payload = payload;
    }
    
    @Override
    public ByteBuf getPayload() {
        return this.payload;
    }
}
