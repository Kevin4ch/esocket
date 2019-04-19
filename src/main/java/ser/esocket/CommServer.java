package ser.esocket;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringEncoder;
import ser.esocket.rec.MessageReceiver;

import java.nio.charset.Charset;

public class CommServer {

    private CommServer() {
    }

    private static CommServer commServer = null;

    public static CommServer getCommServer() {
        if (commServer == null) {
            commServer = new CommServer();
        }
        return commServer;
    }

    public void initUdpServer(MessageReceiver messageReceiver, int... prots) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup acceptGroup = new NioEventLoopGroup();
        bootstrap.group(acceptGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65525))
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        ch.pipeline().addLast(new JsonObjectDecoder(), new StringEncoder());
                        ch.pipeline().addLast("handler", new SimpleChannelInboundHandler() {

                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
                                System.out.println(o);
                                //messageReceiver.onReceive(channelHandlerContext, JSON.parseObject(byteBuf.toString(Charset.defaultCharset())));
                            }
                        });
                    }
                });

        for (int port : prots) {
            bootstrap.bind(port).sync().channel();
            System.out.println("Bind:" + port);
        }
    }

    public void initTcpServer(MessageReceiver messageReceiver, int... prots) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        NioEventLoopGroup acceptGroup = new NioEventLoopGroup();
        bootstrap.group(acceptGroup)
                .channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 2048)
                .option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new JsonObjectDecoder(), new StringEncoder());
                        ch.pipeline().addLast("handler", new SimpleChannelInboundHandler() {

                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {

                                messageReceiver.onReceive(channelHandlerContext, JSON.parseObject(((ByteBuf)o).toString(Charset.defaultCharset())));
                            }
                        });
                    }
                });

        for (int port : prots) {
            bootstrap.bind(port).sync().channel();
            System.out.println("Bind:" + port);
        }
    }
}
