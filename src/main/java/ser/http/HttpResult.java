package ser.http;

public class HttpResult {
    private int respCode;
    private long busCode;
    private String respContent;
    private String respMsg;

    public HttpResult(int code, String resultMsg) {
        this.respCode = code;
        this.respMsg = resultMsg;
    }


    public HttpResult() {
    }

    public int getRespCode() {
        return respCode;
    }

    public long getBusCode() {
        return busCode;
    }

    public String getRespContent() {
        return respContent;
    }

    public String getRespMsg() {
        return respMsg;
    }

    public void setBusCode(long busCode) {
        this.busCode = busCode;
    }

    public void setRespCode(int respCode) {
        this.respCode = respCode;
    }

    public void setRespContent(String respContent) {
        this.respContent = respContent;
    }

    public void setRespMsg(String respMsg) {
        this.respMsg = respMsg;
    }

    @Override
    public String toString() {

        return "{\"respCode\":" + respCode + ",\"busCode\":" + busCode + ",\"respMsg\":\"" + respMsg + "\",\"respContent\":" + respContent + "}";
    }
}
