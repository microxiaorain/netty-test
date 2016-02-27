package manager.protocol;


import java.io.IOException;

import io.netty.buffer.ByteBuf;
import manager.protocol.Header.OperationType;

public abstract class Frame {
    
    private Header header;
    
    public Header getHeader() {
        return header;
    }
    public void setHeader(Header header) {
        this.header = header;
    }

    public abstract ByteBuf getPayload();
    
    public Frame(Header header) {
        this.header = header;
    }
    
    public Frame(OperationType operType, int sessionId, String ip, int port, int payloadLength) {
        this.header = new Header(operType, ip, port, sessionId, payloadLength);
    }
    
    public static Frame getFromByteBuf(ByteBuf buf) throws IOException {
        final Header header = Header.fromByteBuf(buf.nioBuffer(0, Header.HEADER_LENGTH));
        buf.readerIndex(Header.HEADER_LENGTH);
        
        final Frame result;
        switch(header.getOperationType()) {
            case CONFIG : 
                result = new ConfigFrame(header, buf.readUnsignedShort());
                break;
            case DATA :
                result = new DataFrame(header, buf.copy());
                break;
            default :
                result = null;
                throw new IOException("Invalid operation type");
        }
        return result;
    }
    
}
