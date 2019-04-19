package ser.esocket;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringEncoder;
import ser.esocket.rec.MessageReceiver;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class CommClient {

    public CommClient() {

    }

    private long reconnectDelay = 5000;

    private Channel channel;
    private Bootstrap bootstrap;
    private String ipAddress;
    private MessageReceiver messageReceiver;

    private boolean clientStatus = false;

    /**
     * 当前连接状态
     *
     * @return
     */
    public boolean getClientStatus() {
        return clientStatus;
    }

    public void initTcpClient(String ip, final MessageReceiver messageReceiver) throws Exception {
        ipAddress = ip;
        this.messageReceiver = messageReceiver;
        bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group).option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        ch.pipeline().addLast(new JsonObjectDecoder(), new StringEncoder());
                        ch.pipeline().addLast("handler", new SimpleChannelInboundHandler() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                super.channelActive(ctx);
                                clientStatus = true;
                                messageReceiver.onConnectionChanged(true);
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);
                                clientStatus = false;
                                messageReceiver.onConnectionChanged(false);
                                scheduleReconnect(ctx.channel().eventLoop());

                            }

                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {

                                messageReceiver.onReceive(channelHandlerContext, JSON.parseObject(((ByteBuf) o).toString(Charset.defaultCharset())));
                            }
                        });
                    }
                });
        bootstrap.bind(0).channel();
        connectServer();
    }

    private void connectServer() {
        ChannelFuture channelFuture = bootstrap.connect(ipAddress, 6666);
        channelFuture.addListener((ChannelFutureListener) channelFuture1 -> {
            if (channelFuture1.isSuccess()) {
                channel = channelFuture1.channel();
            } else {
                scheduleReconnect(channel.eventLoop());
                messageReceiver.onConnectionChanged(false);
            }

        });
    }

    /**
     * 延迟重连
     */
    private void scheduleReconnect(EventLoop eventLoop) {
        if (reconnectDelay > 0) {
            eventLoop.schedule(this::connectServer, reconnectDelay, TimeUnit.MILLISECONDS);
        }

    }

    /**
     * 重连等待时间(毫秒) 0不重连
     *
     * @param reconnectDelay
     */
    public void setReconnectDelay(long reconnectDelay) {
        if (reconnectDelay < 0) {
            reconnectDelay = 0;
        }
        this.reconnectDelay = reconnectDelay;
    }

    /**
     * 发送字符串 自动添加分隔符：\n\t
     *
     * @param string
     */
    public void sendMessage(String string) {
        System.out.println("SEND:" + string);
        channel.writeAndFlush(string + "\n\t");
    }
}
