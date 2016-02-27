package manager.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import manager.protocol.Header.OperationType;

public class ConfigFrame extends Frame {
    
    private final static int PAYLOAD_LENGTH = 2;
    private ConfigCode code;
    
    public ConfigFrame(Header header, int configCode) {
        super(header);
        
        ConfigCode cc = ConfigCode.OTHER;
        for (ConfigCode item : ConfigCode.values()) {
            if (item.getCode() == configCode) {
                cc = item;
                break;
            }
        }
        this.code = cc;
    }
    
    public ConfigFrame(int sId, String ip, int port, ConfigCode configCode) {
        super(OperationType.CONFIG, sId, ip, port, PAYLOAD_LENGTH);
        this.code = configCode;
    }
    
    public static enum ConfigCode {
        OTHER(0),
        ADD_SESSION(1),
        CLOSE_SESSION(2);
        
        private int code;
        
        private ConfigCode(int code) {
            this.code = code;
        } 
        
        public int getCode() {
            return this.code;
        }
    }
    
    @Override
    public ByteBuf getPayload() {
        return Unpooled.copyShort(this.code.getCode());
    }
    
    public ConfigCode getConfigCode() {
        return this.code;
    }

}
