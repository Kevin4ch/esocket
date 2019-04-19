package ser;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.*;
import ser.esocket.CommClient;
import ser.esocket.rec.MessageReceiver;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class Client extends JFrame {

    private String dir;

    public static void main(String[] args) {
        Client frame = new Client();
        frame.setLayout(null);
        frame.label.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (frame.dir != null) {
                    frame.log.setText("正在初始化连接");
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            frame.init();
                        }
                    }, 500);
                } else {
                    frame.log.setText("请选择图片所在文件夹");
                }
            }
        });

        frame.log.setBounds(8, 5, 630, 30);
        frame.add(frame.log);

        frame.select.setBounds(5, 5 + 30, 100, 36);

        {
            frame.label.setText("开始");
            frame.label.setBounds(110 + frame.select.getX() + 100 + 5, 5 + 30, 50, 36);
        }
        {
            frame.interval.setText("2000");
            frame.add(frame.interval);
            frame.interval.setBounds(frame.select.getX() + 100 + 5, 5 + 30, 100, 36);
        }


        frame.select.setText("选择文件夹");
        frame.select.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = jFileChooser.showOpenDialog(frame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String filePath = jFileChooser.getSelectedFile().getAbsolutePath();
                    System.out.println(frame.dir = filePath);
                    frame.select.setText(frame.dir);
                    frame.canvas = new BoxCanvas(filePath);
                    frame.add(frame.canvas);
                    frame.canvas.setBounds(5, 72, 630, 400);

                }

            }
        });
        frame.add(frame.select);
        frame.add(frame.label);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.show();
    }

    private JButton label = new JButton();
    private JLabel log = new JLabel("-/-");
    private JButton select = new JButton();
    private JTextField interval = new JTextField();

    private BoxCanvas canvas;
    CommClient commClient = new CommClient();
    private String currentFileName;

    public void init() {

        try {
            log.setText("正在连接");
            commClient.initTcpClient("47.110.134.5", new MessageReceiver() {
                @Override
                public void onReceive(ChannelHandlerContext context, JSONObject jsonObject) {
                    System.out.println(jsonObject.toString());
                    //HttpResult result = jsonObject.toJavaObject(HttpResult.class);
                    if (jsonObject.getIntValue("respCode") != 0) {
                        System.out.println("####释放");
                        semaphore.release();
                    }
                    if (jsonObject.getIntValue("respCode") == 200) {
                        try {
                            canvas.setBox(currentFileName, jsonObject.getString("respContent"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        log.setText(jsonObject.getString("respMsg") + "！！！！");
                    }
                }

                @Override
                public void onConnectionChanged(boolean nStatus) {
                    if (nStatus) {
                        log.setText("已连接");
                        doWork();
                    } else {
                        log.setText("已断开");
                    }

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Semaphore semaphore = new Semaphore(0);

    private void doWork() {
        if (dir != null) {
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            File file = new File(dir);
                            File[] files = file.listFiles(pathname -> pathname.getName().endsWith("jpg"));
                            if (files == null || files.length == 0) {
                                return;
                            }
                            int index = 1;
                            for (File pathname : files) {
                                try {
                                    if (pathname.getName().endsWith("jpg")) {
                                        currentFileName = pathname.getName();
                                        System.out.println(pathname.length());
                                        log.setText((index++) + "/" + files.length + "处理：" + pathname);
                                        if (pathname.length() > (3 * 1024 * 1024)) {
                                            scaleImage(pathname, pathname, 0.3, "jpg");
                                        } else if (pathname.length() > (1224 * 1024)) {
                                            scaleImage(pathname, pathname, 0.5, "jpg");
                                        } else if (pathname.length() > (600 * 1024)) {
                                            scaleImage(pathname, pathname, 0.7, "jpg");
                                        } else if (pathname.length() > (350 * 1024)) {
                                            scaleImage(pathname, pathname, 0.9, "jpg");
                                        }

                                        BufferedImage image = ImageIO.read(pathname);
                                        canvas.setImage(image);
                                        log.setText("上传：" + pathname);
                                        commClient.sendMessage("{\"busCode\":" + System.currentTimeMillis() + ",\"base64\":\"" + readBase64(pathname) + "\"}");
                                        semaphore.acquire();
                                        Thread.sleep(Integer.valueOf(interval.getText()));
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    }
            ).start();
        }
    }

    public static void scaleImage(File file,
                                  File destinationPath, double scale, String format) {

        System.out.println(scale);
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(file);
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            width = parseDoubleToInt(width * scale);
            height = parseDoubleToInt(height * scale);

            Image image = bufferedImage.getScaledInstance(width, height,
                    Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics graphics = outputImage.getGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();

            ImageIO.write(outputImage, format, destinationPath);
        } catch (IOException e) {
            System.out.println("scaleImage方法压缩图片时出错了");
            e.printStackTrace();
        }

    }


    /**
     * 将double类型的数据转换为int，四舍五入原则
     *
     * @param sourceDouble
     * @return
     */
    private static int parseDoubleToInt(double sourceDouble) {
        int result = 0;
        result = (int) sourceDouble;
        return result;
    }

    public String readBase64(File file) throws Exception {
        FileInputStream baos = new FileInputStream(file);//io流
        BufferedInputStream inputStream = new BufferedInputStream(baos);
        int len = inputStream.available();

        byte[] bytes = new byte[len];
        inputStream.read(bytes);
        inputStream.close();
        return Base64.getEncoder().encodeToString(bytes).replaceAll("\n", "").replaceAll("\r", "");
    }

}
