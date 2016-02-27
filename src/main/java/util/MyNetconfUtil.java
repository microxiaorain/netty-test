package util;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;

import io.netty.channel.Channel;

public class MyNetconfUtil {
   
    public static void main(String[] args) throws NetconfDocumentedException {
//        Set<String> capabilities = new HashSet<String>();
////        capabilities.add("urn:ietf:params:netconf:base:1.1");
//        NetconfHelloMessage msg = NetconfHelloMessage.createServerHello(capabilities, 0L);
//        System.out.println(msg);
        Object obj = new Object();
        System.out.println(generateSessionId(obj));
        
        System.out.println(Integer.toHexString(100));
        
    }
    
    public static String generateSessionId(Object ch) {
        String origin = Integer.toHexString(ch.hashCode());
        int left = 8 - origin.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < left ; i++) {
            sb.append("0");
        }
        sb.append(origin);
        return sb.toString();
    }
    
}
