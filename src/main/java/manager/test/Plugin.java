package manager.test;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import manager.protocol.FrameDecoder;
import manager.protocol.FrameEncoder;
import manager.protocol.ConfigFrame;
import manager.protocol.ConfigFrame.ConfigCode;
import manager.protocol.DataFrame;
import manager.protocol.Frame;
import util.MyByteBufUtil;

public class Plugin {
    
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup(1);
    ServerBootstrap pluginServerBootstrap = new ServerBootstrap();
    
    public Plugin() throws InterruptedException {
        
        pluginServerBootstrap.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .option(ChannelOption.SO_BACKLOG, 10000)
//         .handler(new LoggingHandler(LogLevel.INFO))
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
                 ChannelPipeline p = ch.pipeline();
                 p.addLast(new FrameDecoder());
                 p.addLast(new FrameEncoder());
                 p.addLast(new Plugin2AgentHandler());
             }
         });

        // Start the server.
        pluginServerBootstrap.bind(8585).sync();
    }
    
    public static Session getPlugin2AgentSession(HttpRequest request, Channel clientChannel) {
        /*
         * /agent1/device1/test.jsp  --> /test.jsp, agent_id:agent1, device_id:device1
         * 
         */
        Session session = null;
        Url url = getUrl(request.getUri());
        if (url != null) {
            request.setUri(url.url);
            Connection cnxn = getAgentCnxn(url.agentId);
            if (cnxn != null) {
                session = cnxn.getSession(url.deviceId, clientChannel);
            }
        } 
        return session;
    }
    
    public static Session getWsSession(HttpRequest request, Channel clientChannel) {
        /*
         * /agent1/device1/test.jsp  --> /test.jsp, agent_id:agent1, device_id:device1
         * 
         */
        Url url = getUrl("");
        Session session = null;
        if (url != null) {
            Connection cnxn = getAgentCnxn(url.agentId);
            if (cnxn != null) {
                 session = cnxn.getWsSession(url.deviceId, clientChannel);
            }
        } 
        return session;
    }
    
    
    public static Connection getAgentCnxn(String agentId) {
        /*
         * agentId  -> agent ip , port
         * 
         */
        return Plugin2AgentCnxnMgnr.cnxnMap.get("default");
    }
    
    public static Url getUrl(String browserUrl) {
        return Url.getUrl(browserUrl);
    }

}

class Plugin2AgentHandler extends SimpleChannelInboundHandler<Frame> {
    
    static AttributeKey<Connection> CNXN_KEY = AttributeKey.valueOf("connection");
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Connection cnxn = new Connection(ctx.channel());
        ctx.channel().attr(CNXN_KEY).set(cnxn);                
        Plugin2AgentCnxnMgnr.cnxnMap.put("default", cnxn);
        System.out.println("usc channel " + ctx.channel() + " is create...");
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        
        if (msg instanceof DataFrame) {
            DataFrame data = (DataFrame) msg;
            int sId = data.getHeader().getSessionId();
            Session s = ctx.channel().attr(CNXN_KEY).get().sessionMap().get(sId);
            s.clientChannel().writeAndFlush(data.getPayload());
        } else if (msg instanceof ConfigFrame) {
            ConfigFrame config = (ConfigFrame) msg;
            if (ConfigCode.CLOSE_SESSION == config.getConfigCode()) {
                ctx.channel().attr(CNXN_KEY).get().delSession(config.getHeader().getSessionId());
            }
        }
        
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(CNXN_KEY).get().close();
    }
    
}

class Url {
    
    public static Pattern pattern = Pattern.compile("^/([\\w]+)/([\\w]+)(/[-/#\\.\\\\\\w]*)");
    String agentId;
    String deviceId;
    String url;
    
    public static Url getUrl(String browserUrl) {
        Matcher m = pattern.matcher(browserUrl);
        if (m.find()) {
            Url url = new Url();
            url.agentId = m.group(1);
            url.deviceId = m.group(2);
            url.url = m.group(3);
            return url;
        } else {
            Url url = new Url();
            url.url = "/";
            url.agentId = "agent1";
            url.deviceId = "device1";
            return url;
        }
    }
    
    public String toString() {
        return this.agentId + "," + this.deviceId + "," + this.url;
    }
}

class Session {
    private int sessionId;
//    private String browserIp;
//    private int browserPort;
    private String deviceIp;
    private int devicePort;
    private Channel b2pChannel;
    private Connection p2aCnxn;
    
    public Session(int sessionId, Channel b2pChannel, Connection p2aCnxn, String deviceIp, int devicePort) {
        this.sessionId = sessionId;
        this.b2pChannel = b2pChannel;
        this.p2aCnxn = p2aCnxn;
        this.deviceIp = deviceIp;
        this.devicePort = devicePort;
    }
    
    public Channel clientChannel() {
        return this.b2pChannel;
    }
    
    public Connection cnxn() {
        return this.p2aCnxn;
    }
    
    public int id() {
        return this.sessionId;
    }
    
    public String deviceIp() {
        return this.deviceIp;
    }
    
    public int devicePort() {
        return this.devicePort;
    }
    
    public String toString() {
        return "usc channel " + this.p2aCnxn.channel().hashCode() + "'s session : " +
                this.id() + "," + this.deviceIp + ":" + this.devicePort;
    }
    
    public void close() {
        this.p2aCnxn.channel().writeAndFlush(new ConfigFrame(this.id(), this.deviceIp(), this.devicePort(), ConfigCode.CLOSE_SESSION));
        this.b2pChannel.close();
        System.out.println(this.toString() + " is close.");
        
    }
    
    public void write(ByteBuf payload) {
        DataFrame data = new DataFrame(this.sessionId, this.deviceIp, this.devicePort, payload);
        this.p2aCnxn.channel().writeAndFlush(data);
    }
}

class Connection {
    
    private String agentIp;
    private int agentPort;
    private Channel p2aChannel;
    private Hashtable<Integer, Session> sessionMap = new Hashtable<Integer, Session>();
    
    public Connection(Channel p2aChannel) {
        this.p2aChannel = p2aChannel;
    }
    
    public Channel channel() {
        return this.p2aChannel;
    }
    
    public Hashtable<Integer, Session> sessionMap() {
        return this.sessionMap;
    }
    
    public void close() {
        Enumeration<?> sessions = sessionMap.keys();
        while (sessions.hasMoreElements()) {
            this.delSession(sessionMap.get(sessions.nextElement()));
        }
        Plugin2AgentCnxnMgnr.cnxnMap.remove("default");
        p2aChannel.close();
    }
    
    public Session getWsSession(String deviceId, Channel clientChannel) {
        String key = clientChannel.toString();
        Session session = sessionMap.get(key);
        if (session != null) {
            return session;
        }
        
        //get device ip and port by device id
        //TODO
        
        return addSession("10.74.68.81", 9002, clientChannel);
    }
    
    public Session getSession(String deviceId, Channel clientChannel) {
        String key = clientChannel.toString();
        Session session = sessionMap.get(key);
        if (session != null) {
            return session;
        }
        
        //get device ip and port by device id
        //TODO
        
        return addSession("10.74.68.81" /* 127.0.0.1 */, 82, clientChannel);
    }
    
    public Session addSession(String ip, int port, Channel clientChannel) {
//        String sessionId = generateSessionId(clientChannel);
        int sessionId = clientChannel.hashCode();
        Session s = new Session(sessionId, clientChannel, this, ip, port);
        
        this.p2aChannel.writeAndFlush(new ConfigFrame(sessionId, ip, port, ConfigCode.ADD_SESSION));
        
        sessionMap.put(sessionId, s);
        System.out.println(s.toString() + "is create.");
        return s;
    }
    
    public String generateSessionId(Channel ch) {
        String origin = Integer.toHexString(ch.hashCode());
        int left = 8 - origin.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < left ; i++) {
            sb.append("0");
        }
        sb.append(origin);
        return sb.toString();
    }
    
    public void delSession(int sId) {
        Session s = this.sessionMap.get(sId);
        if (s != null) {
            this.delSession(s);
        }
    }
    
    public void delSession(Session s) {
        s.close();
        this.sessionMap.remove(s.id());
    }
}

class Plugin2AgentCnxnMgnr {
    public static Hashtable<String, Connection> cnxnMap = new Hashtable<String, Connection>();    
}