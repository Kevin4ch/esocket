package ser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class BoxCanvas extends Canvas {

    File log;

    public BoxCanvas(String dir) {
        super();
        log = new File(dir + File.separator + "result.txt");
    }

    private int drawWidth = 0;
    private int drawHeight = 0;
    private BasicStroke basicStroke = new BasicStroke(1);

    private BufferedImage image;

    public void setImage(BufferedImage image) {
        notfound = false;
        this.image = image;
        rects.clear();
        prects.clear();
        repaint();
    }

    class Rect {
        int x;
        int y;
        int w;
        int h;
        String msg;
    }

    private ArrayList<Rect> rects = new ArrayList<>();
    private ArrayList<Rect> prects = new ArrayList<>();


    public void setBox(String fame, String resp) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(log, true));
        rects.clear();
        prects.clear();
        JSONObject obj = JSON.parseObject(resp);
        JSONArray cars = obj.getJSONObject("Result").getJSONArray("Vehicles");
        String carNams = "";

        if (cars != null && cars.size() > 0) {
            for (int i = 0; i < cars.size(); i++) {
                JSONObject car = cars.getJSONObject(i);
                JSONObject box = car.getJSONObject("Img").getJSONObject("Cutboard");

                Rect rect = new Rect();
                rect.x = box.getIntValue("X");
                rect.y = box.getIntValue("Y");
                rect.w = box.getIntValue("Width");
                rect.h = box.getIntValue("Height");


                JSONObject type = car.getJSONObject("ModelType");
                String name = type.getString("Brand");
                String subname = type.getString("SubBrand");
                rect.msg = name + "," + subname;
                carNams += name + subname + "[";
                rects.add(rect);
                JSONArray plats = car.getJSONArray("Plates");
                for (int j = 0; j < plats.size(); j++) {

                    JSONObject pObj = plats.getJSONObject(j);
                    String color = pObj.getJSONObject("Color").getString("ColorName");

                    String plate = pObj.getString("PlateText");
                    double score = pObj.getDouble("Confidence");


                    JSONObject pbox = pObj.getJSONObject("Cutboard");

                    Rect prect = new Rect();
                    prect.x = pbox.getIntValue("X") + rect.x;
                    prect.y = pbox.getIntValue("Y") + rect.y;
                    prect.w = pbox.getIntValue("Width");
                    prect.h = pbox.getIntValue("Height");
                    prect.msg = plate + String.format("(%.2f)", score);
                    prects.add(prect);
                    carNams += plate;
                }
                carNams += "]\t";

            }
        }
        writer.append(fame + '\t' + carNams + '\n');
        writer.close();
        if (rects.size() == 0 && prects.size() == 0) {
            notfound = true;
        }
        repaint();
    }

    boolean notfound = false;

    @Override
    public void paint(Graphics g1) {
        super.paint(g1);
        Graphics2D g = (Graphics2D) g1;
        g.setColor(Color.RED);
        g.setStroke(basicStroke);
        Font font = new Font("宋体", Font.BOLD, 18);
        g.setFont(font);
        if (image != null) {
            // resize image
            if (image.getWidth() > image.getHeight()) {
                drawWidth = getWidth();
                drawHeight = (int) (image.getHeight() / (float) image.getWidth() * drawWidth);
            } else {
                drawHeight = getHeight();
                drawWidth = (int) ((float) image.getWidth() / image.getHeight() * drawHeight);

            }
            float rate = drawWidth / (float) image.getWidth();
            g.drawImage(image, 0, 0, drawWidth, drawHeight, 0, 0, image.getWidth(),
                    image.getHeight(), this);
            for (Rect rect : rects) {
                g.drawRect((int) (rect.x * rate), (int) (rect.y * rate), (int) (rect.w * rate), (int) (rect.h * rate));
                g.drawString(rect.msg, rect.x * rate, rect.y * rate + 20);
            }
            for (Rect rect : prects) {
                g.drawRect((int) (rect.x * rate), (int) (rect.y * rate), (int) (rect.w * rate), (int) (rect.h * rate));
                g.drawString(rect.msg, rect.x * rate, rect.y * rate - 5);
            }
            if (notfound) {

                g.drawString("###未识别###", drawWidth / 2f, drawHeight / 2f);
            }
        }

    }
}
