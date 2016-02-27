package test.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;

public class LocalServer {
    
    private static final LocalAddress localServerAddr = new LocalAddress("xiaorain-local-server");
    
    public static void main(String[] args) throws InterruptedException {
        
        Thread serverThread = new Thread(new Runnable() {

            @Override
            public void run() {
                SimpleServer  s = new SimpleServer(LocalServerChannel.class);
                s.bootstrap().childHandler(new ChannelInitializer<LocalChannel>() {
                    @Override
                    public void initChannel(final LocalChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new SimpleClientHandler());
                    }
                });
                
                ChannelFuture f;
                
                try {
                    f = s.bootstrap().bind(localServerAddr).sync();
                    f.channel().closeFuture().sync().await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
               
                
            }
            
        });

        
        Thread clientThread = new Thread(new Runnable() {

            @Override
            public void run() {
                SimpleClient client = new SimpleClient(new LocalEventLoopGroup(), LocalChannel.class);
                
                client.bootstrap().handler(new ChannelInitializer<LocalChannel>() {
                        @Override
                        public void initChannel(LocalChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast("mine", new MyHandler());
                        }
                });
                
                try {
                    client.bootstrap().connect(localServerAddr).sync().channel().closeFuture().await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
            }
            
        });
        
        
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();

    }

}
