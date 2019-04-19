package ser.esocket.rec;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;

public interface MessageReceiver {
    void onReceive(ChannelHandlerContext context, JSONObject jsonObject);

    void onConnectionChanged(boolean nStatus);
}
