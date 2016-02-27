package manager.test;

import java.util.Enumeration;
import java.util.Hashtable;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import manager.protocol.ConfigFrame;
import manager.protocol.DataFrame;
import manager.protocol.Frame;
import manager.protocol.FrameDecoder;
import manager.protocol.FrameEncoder;
import manager.protocol.ConfigFrame.ConfigCode;

public class Agent {
    
    static EventLoopGroup pluginClientGroup;
    static Bootstrap pluginClientBootstrap;
    static EventLoopGroup deviceClientGroup;
    static Bootstrap deviceClientBootstrap;
    static AttributeKey<AgentCnxn> PLUGIN_AGENT_CNXN_KEY = AttributeKey.valueOf("connection");
    static AttributeKey<AgentSession> AGENT_DEVICE_SESSION_KEY = AttributeKey.valueOf("deviceConnection");
    
    static {
        
        pluginClientGroup = new NioEventLoopGroup(1);
        deviceClientGroup = new NioEventLoopGroup(1);
        
        pluginClientBootstrap = new Bootstrap();
        pluginClientBootstrap.channel(NioSocketChannel.class).group(pluginClientGroup)
                       .handler(new ChannelInitializer<SocketChannel>() {
                           @Override
                           public void initChannel(SocketChannel ch) throws Exception {
                               ChannelPipeline p = ch.pipeline();
                               p.addLast(new FrameDecoder());
                               p.addLast(new FrameEncoder());
                               p.addLast(new AgentHandler());
                           }
                       });
        deviceClientBootstrap = new Bootstrap();
        deviceClientBootstrap.channel(NioSocketChannel.class).group(deviceClientGroup)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline p = ch.pipeline();
                                p.addLast(new DeviceHandler());
                            }
                        });
        
    }
    
    public static void main(String[] args) throws InterruptedException {
        Channel ch = pluginClientBootstrap.connect("127.0.0.1", 8585).sync().channel();
        ch.closeFuture().sync().await();
    }
}

class AgentHandler extends SimpleChannelInboundHandler<Frame> {
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        
        AgentCnxn agent2pluginCnxn = new AgentCnxn();
        ctx.channel().attr(Agent.PLUGIN_AGENT_CNXN_KEY).set(agent2pluginCnxn);
        agent2pluginCnxn.setChannel(ctx.channel());
        
        System.out.println(ctx.channel());
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        AgentCnxn cnxn = ctx.channel().attr(Agent.PLUGIN_AGENT_CNXN_KEY).get();
        if (msg instanceof ConfigFrame) {
            ConfigFrame config = (ConfigFrame) msg;
            String ip = config.getHeader().getDeviceIpStr();
            int port = config.getHeader().getDevicePort();
            int sessionId = config.getHeader().getSessionId();
            if (ConfigCode.ADD_SESSION == config.getConfigCode()) {
                cnxn.addSession(sessionId, ip, port);
            } else if (ConfigCode.CLOSE_SESSION == config.getConfigCode()) {
                cnxn.removeSession(sessionId);
            }
        } else if (msg instanceof DataFrame) {
            DataFrame data = (DataFrame) msg;
            AgentSession session = cnxn.getSession(data.getHeader().getSessionId());
            session.channel().writeAndFlush(data.getPayload().copy());
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        AgentCnxn cnxn = ctx.attr(Agent.PLUGIN_AGENT_CNXN_KEY).get();
        cnxn.close();
    }
    
}


class DeviceHandler extends SimpleChannelInboundHandler<ByteBuf> {
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("out channel "+ ctx.channel() + " is create.");
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        AgentSession s = ctx.channel().attr(Agent.AGENT_DEVICE_SESSION_KEY).get();
        DataFrame data = new DataFrame(s.id(), s.deviceIp(), s.devicePort(), msg.copy());
        s.agentCnxn().channel().writeAndFlush(data);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        AgentSession s = ctx.channel().attr(Agent.AGENT_DEVICE_SESSION_KEY).get();
        s.close();
    }
    
}

class AgentCnxn {
    
    private Channel plugin2agentCnxn;
    private Hashtable<Integer, AgentSession> sessionMap = new Hashtable<Integer, AgentSession>();
    
    public void addSession(int sId, String dIp, int port) throws InterruptedException {
        AgentSession s = sessionMap.get(sId);
        if (s != null) {
            return ;
        } else {
            Channel deviceChannel = Agent.deviceClientBootstrap.connect(dIp, port).sync().channel();
            
            s = new AgentSession(sId, dIp, port, deviceChannel, this);
            
            deviceChannel.attr(Agent.AGENT_DEVICE_SESSION_KEY).set(s);
            
            this.sessionMap.put(sId, s);
            
            System.out.println( s.toString() + " is create.");
        }
    }
    
    public void removeSession(int sId) {
        AgentSession s = this.sessionMap.get(sId);
        if (s != null) {
            s.close();
            this.sessionMap.remove(sId);
        }
    }
    
    public AgentSession getSession(int sId) {
        return this.sessionMap.get(sId);
    }

    public Channel channel() {
        return plugin2agentCnxn;
    }

    public void setChannel(Channel plugin2agentCnxn) {
        this.plugin2agentCnxn = plugin2agentCnxn;
    }
    
    public void close() {
        Enumeration<Integer> sessions = sessionMap.keys();
        while (sessions.hasMoreElements()) {
            this.removeSession(sessionMap.get(sessions.nextElement()).id());
        }
        this.plugin2agentCnxn.close();
    }
    
}

class AgentSession {
    
    private int sessionId;
    private String deviceIp;
    private int devicePort;
    private Channel agent2DeviceChannel;
    private AgentCnxn cnxn;
    
    public AgentSession(int sId, String dIp, int port, Channel deviceChannel, AgentCnxn agentCnxn) {
        this.sessionId = sId;
        this.deviceIp = dIp;
        this.devicePort = port;
        this.agent2DeviceChannel = deviceChannel;
        this.cnxn = agentCnxn;
    }
    
    public Channel channel() {
        return this.agent2DeviceChannel;
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
    
    public AgentCnxn agentCnxn() {
        return this.cnxn;
    }
    
    public String toString() {
        return "usc channel " + this.cnxn.channel().hashCode() + " 's session " + id() + "," + deviceIp() + ":" + devicePort(); 
    }
    
    public void close() {
        if (this.cnxn.channel().isOpen()) {
            this.cnxn.channel().writeAndFlush(new ConfigFrame(this.id(), this.deviceIp(), this.devicePort(), ConfigCode.CLOSE_SESSION));
        }
        this.agent2DeviceChannel.close();
        System.out.println(this.toString() + " is close.");
    }
    
}
