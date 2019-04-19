package ser.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HttpUtil {

    public static HttpResult postString(String url, String postValue) {
        HttpResult result = new HttpResult();
        try {

            StringEntity stringEntity = new StringEntity(postValue);
            stringEntity.setContentType("application/octet-stream");
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(stringEntity);
            BasicHttpParams httpparams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpparams, 3000);
            HttpConnectionParams.setSoTimeout(httpparams, 3000);
            DefaultHttpClient client = new DefaultHttpClient(httpparams);
            HttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            StringBuilder stringBuilder = new StringBuilder();
            if (statusCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String tmp = null;
                while ((tmp = reader.readLine()) != null) {
                    stringBuilder.append(tmp);
                }
                reader.close();
                result.setRespContent(stringBuilder.toString());
                result.setRespMsg("Success");
            } else {
                result.setRespContent("");
                result.setRespMsg(response.getStatusLine().getReasonPhrase());
            }
            result.setRespCode(statusCode);
        } catch (Exception e) {
            result.setRespCode(1000);
            result.setRespMsg(e.getMessage());
        }
        return result;
    }
}
