package ser;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.*;
import ser.esocket.CommClient;
import ser.esocket.rec.MessageReceiver;
import ser.http.HttpResult;
import ser.http.HttpUtil;

public class GpuClient {


    public static final int BUS_CODE_PLATE = 10001;

    public static String PLATE_SERVER_URL = null;

    public static String PARAM_PICK = "{\"Context\": {\"SessionId\": \"test123\",\"Functions\": [100,101,102,103,104],\"Type\":1},\"Image\": {\"Data\": {\"BinData\": \"%s\"}}}";


    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0) {
            PLATE_SERVER_URL = String.format("http://%s:6501/process/single", "127.0.0.1");
            System.out.println("LOCAL:" + PLATE_SERVER_URL + ", Ser:" + args[0]);
        } else {
            System.out.println("IP NOT SET");
            return;
        }
        CommClient commClient = new CommClient();
        commClient.initTcpClient(args[0], new MessageReceiver() {
            @Override
            public void onReceive(ChannelHandlerContext context, JSONObject jsonObject) {
                System.out.println(jsonObject);
                requestPlateValue(commClient, jsonObject.getString("base64"), jsonObject.getLong("busCode"));
            }

            @Override
            public void onConnectionChanged(boolean nStatus) {
                System.out.println(nStatus);
                commClient.sendMessage("{\"gpu\":1}");
            }
        });
    }

    private static void requestPlateValue(CommClient context, String base, long code) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("SEND REQ");
                HttpResult result = HttpUtil.postString(PLATE_SERVER_URL, String.format(PARAM_PICK, base));
                result.setBusCode(code);
                System.out.println(result);
                context.sendMessage(result.toString());
            }
        }).start();
    }
}
