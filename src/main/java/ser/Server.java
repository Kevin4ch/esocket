package ser;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import ser.esocket.CommServer;
import ser.esocket.rec.MessageReceiver;
import ser.http.HttpResult;

import java.util.concurrent.ConcurrentHashMap;

public class Server {

    public static void main(String[] args) {
        Server server = new Server();
        server.makeServer();
    }


    private ChannelHandlerContext gpuClient = null;

    private ConcurrentHashMap<Long, ChannelHandlerContext> clientList = new ConcurrentHashMap<>();

    private void makeServer() {

        CommServer commServer = CommServer.getCommServer();
        try {
            commServer.initTcpServer(new MessageReceiver() {
                @Override
                public void onConnectionChanged(boolean nStatus) {

                }

                @Override
                public void onReceive(ChannelHandlerContext context, JSONObject message) {
                    try {
                        if (message.containsKey("gpu")) {
                            System.out.println("GPU Client Online");
                            gpuClient = context;
                        } else if (message.containsKey("respCode")) {
                            long id = message.getLong("busCode");
                            System.out.println("收到鉴别结果:" + id);
                            if (clientList.containsKey(id)) {
                                clientList.get(id).channel().writeAndFlush(message.toJSONString() + "\n\t");
                                clientList.remove(id);
                            } else {
                                System.out.println("未查到请求号所属客户端！");
                            }

                        } else {
                            long id = message.getLong("busCode");
                            if (gpuClient != null) {
                                clientList.put(id, context);
                                gpuClient.channel().writeAndFlush(message.toString() + "\n\t");
                                sendMessage(context, new HttpResult(0, "Wait").toString());
                            } else {
                                sendMessage(context, new HttpResult(1000, "GPU Server Down").toString());
                            }
                        }

                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        sendMessage(context, new HttpResult(1001, "args error").toString());
                    }
                }
            }, 6666);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void sendMessage(ChannelHandlerContext context, String message) {
        message = message + '\n' + '\t';
        context.channel().writeAndFlush(message);
    }

}
