package manager.test;


import java.util.Iterator;
import java.util.Map.Entry;

import org.jboss.netty.handler.codec.http.HttpChunk;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocket00FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import util.MyByteBufUtil;

/**
 * Hello world!
 *
 */
public class Proxy 
{
    static final int PORT = Integer.parseInt(System.getProperty("port", "8383"));
    static final int WEBSOCKET_PORT = Integer.parseInt(System.getProperty("port", "9002"));
    
    public static void main(String[] args) throws Exception {
        Plugin p = new Plugin();
        initWebSocketServer();
        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        try {
            ServerBootstrap proxyServerBootstrap = new ServerBootstrap();
            proxyServerBootstrap.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 10000)
//             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     p.addLast(new HttpRequestDecoder());
                     p.addLast(new StringEncoder());
                     p.addLast(new HttpBrowser2ProxyHandler());
                 }
             });
 
            // Start the server.
            ChannelFuture f = proxyServerBootstrap.bind(PORT).sync();
 
            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    public static void initWebSocketServer() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        ServerBootstrap wsServerBootstrap = new ServerBootstrap();
        wsServerBootstrap.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .option(ChannelOption.SO_BACKLOG, 10000)
//         .handler(new LoggingHandler(LogLevel.INFO))
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
                 ChannelPipeline p = ch.pipeline();
//                 p.addLast(new HttpRequestDecoder());
                 p.addLast(new WsHandler());
             }
         });
 
          // Start the server.
        ChannelFuture f = wsServerBootstrap.bind(WEBSOCKET_PORT).sync();
 
    }
}

class WsHandler extends HttpBrowser2ProxyHandler {

    static AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("session");
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        
//        System.out.println("outer recv : " + msg.getClass().getName());
        
//        if (msg instanceof HttpRequest) {
//            System.out.println(msg.getClass().getName());
//            HttpRequest request = (HttpRequest) msg;
//
//            Session session = ctx.channel().attr(SESSION_KEY).get();
//            if (session == null) { 
//                session = Plugin.getWsSession(request, ctx.channel());
//                ctx.channel().attr(SESSION_KEY).set(session);
//            }
//            
//            String data = 
//                request.getMethod() + " " +
//                /*request.getUri()*/  "ws://cne.cisco.com:9002/ " + 
//                request.getProtocolVersion().text() + "\r\n";
//            
//            Iterator<Entry<String,String>> iterator = request.headers().iterator();
//            while(iterator.hasNext()) {
//                Entry<String,String> item = iterator.next();
//                data += (
//                        item.getKey() + ": " + item.getValue() + "\r\n");
//            }
//            data += "\r\n";
//            
//            session.write(MyByteBufUtil.str2buffer(data));
//        }
        
      Session session = ctx.channel().attr(SESSION_KEY).get();
      if (session == null) { 
          session = Plugin.getWsSession(null, ctx.channel());
          if (session == null) {
              throw new RuntimeException("usc connection not found.");
          }
          ctx.channel().attr(SESSION_KEY).set(session);
      } 
      session.write( ((ByteBuf)msg).copy() );
        
    }
    
//    @Override
//    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        System.out.println(ctx.channel() + "closed.");
//        ctx.channel().attr(SESSION_KEY).get().close();
//    }
    
}

class HttpBrowser2ProxyHandler extends SimpleChannelInboundHandler {
    
    static AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("session");
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            Session session = ctx.channel().attr(SESSION_KEY).get();
            if (session == null) { 
                session = Plugin.getPlugin2AgentSession(request, ctx.channel());
                if (session == null) {
                    throw new RuntimeException("usc connection not found.");
                }
                ctx.channel().attr(SESSION_KEY).set(session);
            } else {
                request.setUri(Url.getUrl(request.getUri()).url);
            }
            String data = 
                request.getMethod() + " " +
                request.getUri() + " " + 
                request.getProtocolVersion().text() + "\r\n";
            session.write(MyByteBufUtil.str2buffer(data));
            data = "";
            Iterator<Entry<String,String>> iterator = request.headers().iterator();
            while (iterator.hasNext()) {
                Entry<String,String> item = iterator.next();
                data += (
                        item.getKey() + ": " + item.getValue() + "\r\n");
            }
            session.write(MyByteBufUtil.str2buffer(data));
            session.write(MyByteBufUtil.str2buffer("\r\n"));
            
        } else if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            ByteBuf buf = content.content().copy();
            Session s = ctx.channel().attr(SESSION_KEY).get();
            s.write(buf);
        }
        
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("out channel " + ctx.channel() + " is closed.");
        Session s = ctx.channel().attr(SESSION_KEY).get();
        if (s != null) { s.close();}
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        if ("usc connection not found.".equals(cause.getMessage())) {
            FullHttpResponse msg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, MyByteBufUtil.str2buffer(cause.getMessage()));
            channel.writeAndFlush(msg.content());
        } else {
            System.out.println(cause);
        }
        channel.close();
    }
    
}



class Browser2ProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {
    
    static EventLoopGroup clientGroup;
    static Bootstrap clientBootstrap;
    static AttributeKey<Channel> b2pChannelKey = AttributeKey.valueOf("browser2proxy");
    static AttributeKey<Channel> p2sChannelKey = AttributeKey.valueOf("proxy2server");
    static {
        clientGroup = new NioEventLoopGroup(1);
        clientBootstrap = new Bootstrap();
        clientBootstrap.channel(NioSocketChannel.class).group(clientGroup)
                       .handler(new ChannelInitializer<SocketChannel>() {
                           @Override
                           public void initChannel(SocketChannel ch) throws Exception {
                               ChannelPipeline p = ch.pipeline();
                               p.addLast(new Proxy2ServerHandler());
                           }
                       });
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel proxy2serverChannel = clientBootstrap.connect("127.0.0.1", 8282).sync().channel();
        proxy2serverChannel.attr(b2pChannelKey).set(ctx.channel());
        ctx.channel().attr(p2sChannelKey).set(proxy2serverChannel);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        Channel ch = ctx.channel().attr(p2sChannelKey).get();
        ch.writeAndFlush(msg.copy());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel() + "closed.");
        ctx.channel().attr(p2sChannelKey).get().close();
        ctx.channel().close();
    }
}

class Proxy2ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    
    static AttributeKey<Channel> b2pChannelKey = AttributeKey.valueOf("browser2proxy");
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        Channel ch = ctx.channel().attr(b2pChannelKey).get();
        ch.writeAndFlush(msg.copy());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel() + "closed.");
        ctx.channel().attr(b2pChannelKey).get().close();
        ctx.channel().close();
        
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel());
    }
    
}




