package com.wubeibei.hmi_server;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.wubeibei.hmi_server.transmit.Transmit;
import com.wubeibei.hmi_server.util.CrashHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Transmit transmit;
    private ServerSocket server = null;
    private static final int LOCAL_PORT = 5678;
    private static final boolean DEBUG = true; // 是否启动调试

    private final Map<String, Service> socketMap = new ConcurrentHashMap<>(); // 经过允许的客户端
    private final Map<String, Pair<String,String>> devicesMap = new HashMap<>(); // 允许连接的设备号集合
    private final ExecutorService mExecutorService = Executors.newCachedThreadPool(); // 线程池
    private final BlockingQueue<JSONObject> blockingQueue = new LinkedBlockingQueue<>(); // 消息队列

    private TextView LogTextView;
    private EditText Id;
    private EditText Data;

    private static final int First_Login = 1;
    private static final int Secondary_login = 2;
    private static final int Other = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        transmit = Transmit.getInstance();
        transmit.setBlockingQueue(blockingQueue); //设置回调阻塞队列
        LogTextView = findViewById(R.id.LogTextView);
        LogTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        Button button = findViewById(R.id.button);
        button.setText("发送");
        Id = findViewById(R.id.id);
        Data = findViewById(R.id.data);
        Id.setText("87");
        Data.setText("0");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Iterator<Map.Entry<String, Service>> it = socketMap.entrySet().iterator();
                final String str_Id = Id.getText().toString();
                final String str_Data = Data.getText().toString();
                for (Map.Entry<String, Service> entry : socketMap.entrySet()) {
                    final Service value = entry.getValue();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            value.sendmsg("{\"id\":" + str_Id + ",\"data\":" + str_Data + "}");
                        }
                    }).start();
                }
            }
        });
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
        // 开启热点
        setWifiApEnabled(true);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    // wifi热点开关
    public void setWifiApEnabled(boolean enabled) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (enabled) {
            //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
            wifiManager.setWifiEnabled(false);
        }
        if(isWifiApOpen(this))
            return;
        try {
            WifiConfiguration apConfig = new WifiConfiguration();
            //配置热点的名称
            apConfig.SSID ="hmi_host";
            //配置热点的密码(至少8位)
            apConfig.preSharedKey = "hmi_host";
            apConfig.allowedKeyManagement.set(4);
            //通过反射调用设置热点
            Method method = wifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            method.invoke(wifiManager, apConfig, enabled);//true开启热点 false关闭热点
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isWifiApOpen(Context context) {
        try {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            //通过放射获取 getWifiApState()方法
            Method method = manager.getClass().getDeclaredMethod("getWifiApState");
            //调用getWifiApState() ，获取返回值
            int state = (int) method.invoke(manager);
            //通过放射获取 WIFI_AP的开启状态属性
            Field field = manager.getClass().getDeclaredField("WIFI_AP_STATE_ENABLED");
            //获取属性值
            int value = (int) field.get(manager);
            //判断是否开启
            if (state == value) {
                return true;
            } else {
                return false;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 开启收发线程
    private void init() {
        // 获取账户列表
        getDevicesMap();
        // 车辆初始化
        new Thread(new Runnable() {
            @Override
            public void run() {
                transmit.Can_init();
            }
        }).start();

        // 开始接收Pad登陆
        new Thread(new Runnable() {
            @Override
            public void run() {
                ReceiveForPad();
            }
        }).start();

        // 开始将CAN发过来的消息转发给PAD
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        final JSONObject object = blockingQueue.take();
                        if (DEBUG)
                            showToText("收到CAN总线发往Pad的消息" + object.toString() + "。\n");
                        for (Map.Entry<String, Service> entry : socketMap.entrySet()) {
                            final Service value = entry.getValue();
                            if (DEBUG)
                                showToText("向" + value.getPadIpAddress() + "/" + value.getPadPort() + "发送：" + object.toString() + "。\n");
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    value.sendmsg(object.toJSONString());
                                }
                            }).start();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void ReceiveForPad() {
        try {
            server = new ServerSocket(LOCAL_PORT);
            if (DEBUG)
                showToText("服务器开始监听：" + server.getLocalPort() + "\n");
            while (true) {
                Socket client;
                client = server.accept();
                if (DEBUG)
                    showToText("客户端 :" + client.getInetAddress().getHostAddress() + "/" + String.valueOf(client.getPort()) + "请求连接。\n");
                mExecutorService.execute(new Service(client));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (server != null)
                    server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 重启接收程序
            ReceiveForPad();
        }
    }

    class Service implements Runnable {
        private Socket socket;
        private BufferedReader in = null;
        private PrintWriter out = null;
        private String msg = "";
        private String PadIpAddress;
        private int PadPort;
        private static final int CHECK = 1234;
        private boolean LoginSuccess = false;
        private volatile long lastSendTime;
        private long HeartBeatTime = 30 * 1000;
        private Thread HeartBeatThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(HeartBeatTime);
                        long differ = System.currentTimeMillis() - lastSendTime;
                        if (differ > HeartBeatTime) {
                            if (DEBUG)
                                showToText("客户端 :" + PadIpAddress + "/" + String.valueOf(PadPort) + "因为5分钟之内没有发送消息，自动断开连接\n当前时间差：" + String.valueOf(differ) +"\n");
                            closeConn();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Service(Socket socket) {
            this.socket = socket;
            PadIpAddress = socket.getInetAddress().getHostAddress();
            PadPort = socket.getPort();
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                lastSendTime = System.currentTimeMillis();
                if (DEBUG)
                    showToText(PadIpAddress + "/" + String.valueOf(PadPort) + "构造方法调用完毕\n");
                HeartBeatThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String getPadIpAddress() {
            return PadIpAddress;
        }

        public int getPadPort() {
            return PadPort;
        }

        // 接收消息
        @Override
        public void run() {
            try {
                while (true) {
                    if (DEBUG)
                        showToText(PadIpAddress + "/" + String.valueOf(PadPort) + "开始接受\n");
                    if ((msg = in.readLine()) != null) {
                        if (DEBUG)
                            showToText(PadIpAddress + "/" + String.valueOf(PadPort) + "发送消息: " + msg + "\n");
                        JSONObject jsonObject;
                        try {
                            jsonObject = (JSONObject) JSONObject.parse(msg);
                        }catch (JSONException e){
                            e.printStackTrace();
                            lastSendTime = System.currentTimeMillis();
                            if (DEBUG)
                                showToText("收到客户端：" + PadIpAddress + "/" + String.valueOf(PadPort) + "发送的心跳包。当前时间戳：" + lastSendTime +"\n");
                            continue;
                        }
                        int flag = jsonObject.getIntValue("flag");
                        switch (flag) {
                            case First_Login:
                                JSONObject sendObject = new JSONObject();
                                sendObject.put("id", CHECK);
                                if (checkAccount(jsonObject)) {
                                    sendObject.put("data", true);
                                    sendObject.put("msg", "登录成功");
                                    sendmsg(sendObject.toJSONString());
                                    socketMap.put(PadIpAddress, this);
                                    if (DEBUG)
                                        showToText("客户端 :" + PadIpAddress + "/" + String.valueOf(PadPort) + "登录成功；此时总连接：" + socketMap.size() + "。\n");
                                    LoginSuccess = true;
                                } else {
                                    sendObject.put("data", false);
                                    sendObject.put("msg","用户名或密码错误");
                                    sendmsg(sendObject.toJSONString());
                                    if (DEBUG)
                                        showToText("客户端: " + PadIpAddress + "/" + String.valueOf(PadPort) + "权限认证失败\n");
//                                    throw new IOException();
                                }
                                break;
                            case Secondary_login:
                                sendObject = new JSONObject();
                                sendObject.put("id", CHECK);
                                boolean PERMISSION = false;
                                if (checkAccount(jsonObject))
                                    PERMISSION = true;
                                sendObject.put("data", PERMISSION);
                                sendObject.put("msg", PERMISSION ? "管理员身份验证成功" : "管理员身份验证失败");
                                sendmsg(sendObject.toJSONString());
                                break;
                            case Other:
                                if(!LoginSuccess)
                                    break;
                                switch (jsonObject.getIntValue("id")) {
                                    case 0:
                                        transmit.setADAndRCUFlag(jsonObject.getBooleanValue("data"));
                                        break;
                                    case 1:
                                        JSONObject data = jsonObject.getJSONObject("data");
                                        transmit.HostToCAN(data.getString("clazz"), data.getIntValue("field"), data.get("o"));
                                        break;
                                    case 2:
                                        transmit.setOtherFlag(jsonObject.getBooleanValue("data"));

                                    default:
                                        break;
                                }
                                break;
                            default:
                                continue;
                        }
                        lastSendTime = System.currentTimeMillis();
                    }else {
                        if (DEBUG)
                            showToText(PadIpAddress + "/" + String.valueOf(PadPort) + "发送消息为空" + "\n");
                        closeConn();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                closeConn();
            }
        }

        private void closeConn(){
            socketMap.remove(PadIpAddress);
//            JSONObject object = new JSONObject();
//            object.put("id",2345);
//            object.put("data",false);
//            sendmsg(object.toJSONString());
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
                if (socket != null)
                    socket.close();
                if(HeartBeatThread.isAlive()) {
                    HeartBeatThread.interrupt();
                    if (DEBUG)
                        showToText("心跳包线程已退出\n");
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (DEBUG)
                showToText("客户端 :" + PadIpAddress + "/" + String.valueOf(PadPort) + "退出；此时总连接：" + socketMap.size() + "。\n");
        }

        // 发送消息
        void sendmsg(String msg) {
            if (DEBUG)
                showToText("向" + PadIpAddress + "/" + String.valueOf(PadPort) + "发送消息: " + msg + "\n");
            out.println(msg);
        }
    }

    private void getDevicesMap(){
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(this.getAssets().open("DeviceList.xml"));

            //返回文档的根(root)元素
            Element rootElement = document.getDocumentElement();

            //获取一个Node(DOM基本的数据类型)集合 (route)
            NodeList nodes = rootElement.getElementsByTagName("device");
            //遍历Note集合
            for (int i = 0; i < nodes.getLength(); i++) {
                Element personElement = (Element) nodes.item(i);
                NodeList nodeList = personElement.getChildNodes();
                String meid = nodeList.item(1).getTextContent();
                String account = nodeList.item(3).getTextContent();
                String password = nodeList.item(5).getTextContent();
                devicesMap.put(account, new Pair<>(password, meid));
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private static int textNum = 0;
    public void showToText(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(textNum > 50) {
                    textNum = 0;
                    LogTextView.setText("");
                }
                LogTextView.append(str);
                textNum++;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutorService.shutdown();
    }

    public boolean checkAccount(JSONObject jsonObject){
        String meid = jsonObject.getString("meid");
        String account = jsonObject.getString("account");
        String password = jsonObject.getString("password");
        Pair<String, String> pair = devicesMap.get(account);
        return devicesMap.containsKey(account) && pair != null && pair.first.equals(password) && pair.second.equals(meid);
    }
}
