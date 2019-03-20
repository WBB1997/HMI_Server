package com.wubeibei.hmi_server;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.wubeibei.hmi_server.transmit.Transmit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
    private static final boolean DEBUG = true; // 是否启动调试

    private final Map<String, Service> socketMap = new ConcurrentHashMap<>(); // 经过允许的客户端
    private final Set<String> devicesSet = new HashSet<>(); // 允许连接的设备号集合
    private final ExecutorService mExecutorService = Executors.newCachedThreadPool(); // 线程池
    private final BlockingQueue<JSONObject> blockingQueue = new LinkedBlockingQueue<>(); // 消息队列



    private TextView LogTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        transmit = Transmit.getInstance();
        transmit.setBlockingQueue(blockingQueue); //设置回调阻塞队列
        LogTextView = findViewById(R.id.LogTextView);
        init();
    }

    // 开启收发线程
    private void init() {
        // 获取设备列表
        getDevicesSet();
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
                        JSONObject object = blockingQueue.take();
                        if (DEBUG)
                            LogTextView.append("收到CAN总线发往Pad的消息" + object.toString() + "。\n");
                        Iterator<Map.Entry<String, Service>> it = socketMap.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<String, Service> entry = it.next();
                            Service value = entry.getValue();
                            if (DEBUG)
                                LogTextView.append("向" + value.getPadIpAddress() + "/" + value.getPadPort() + "发送：" + object.toString() + "。\n");
                            value.sendmsg(object.toJSONString());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void ReceiveForPad(){
        try {
            server = new ServerSocket();
            Socket client;
            if(DEBUG)
                LogTextView.append("服务器Ip:" + server.getInetAddress().getHostAddress() + "。\n");
            while (true) {
                client = server.accept();
                mExecutorService.execute(new Service(client));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
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
        private boolean PERMISSION = false;

        Service(Socket socket) {
            this.socket = socket;
            PadIpAddress = socket.getInetAddress().getHostAddress();
            PadPort = socket.getPort();
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                if (DEBUG)
                    LogTextView.append("客户端 :" + PadIpAddress + "/" + String.valueOf(PadPort) + "加入；此时总连接：" + socketMap.size() + "。\n");
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
                    if ((msg = in.readLine()) != null) {
//                        if(msg.equals("\n"))
//                            continue;
                        if (DEBUG)
                            LogTextView.append("客户端 :" + PadIpAddress + "/" + String.valueOf(PadPort) + "发送：" + msg + "。\n");
                        JSONObject jsonObject = (JSONObject) JSONObject.parse(msg);
                        // 如果是第一次进入，需要进行权限认证
                        if(!PERMISSION){
                            String mac = jsonObject.getString("MAC");
                            if(devicesSet.contains(mac)) {
                                PERMISSION = true;
                                JSONObject sendObject = new JSONObject();
                                sendObject.put("CHECK",true);
                                sendmsg(sendObject.toJSONString());
                            }
                        }else {
                            int id = jsonObject.getIntValue("id");
                            switch (id) {
                                case 0:
                                    transmit.setADAndRCUFlag(jsonObject.getBooleanValue("data"));
                                    break;
                                case 1:
                                    JSONObject data = jsonObject.getJSONObject("data");
                                    transmit.HostToCAN(data.getString("clazz"), data.getIntValue("field"), data.get("o"));
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    // 如果权限认证没有通过，那么拒绝登陆
                    if(!PERMISSION) {
                        if (DEBUG)
                            LogTextView.append("客户端 :" + PadIpAddress + "/" + String.valueOf(PadPort) + "权限不足，已强制退出。\n");
                        throw new IOException();
                    }else{
                        if(!socketMap.containsKey(PadIpAddress))
                            socketMap.put(PadIpAddress, this);
                    }
                }
            } catch (IOException e) {
                if (DEBUG)
                    LogTextView.append("客户端 :" + PadIpAddress + "/" + String.valueOf(PadPort) + "退出；此时总连接：" + socketMap.size() + "。\n");
                closeConn();
            }
        }

        private void closeConn(){
            socketMap.remove(PadIpAddress);
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        // 发送消息
        void sendmsg(String msg) {
            out.println(msg);
        }
    }

    private void getDevicesSet(){
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(this.getAssets().open("DeviceList.xml"));

            //返回文档的根(root)元素
            Element rootElement = document.getDocumentElement();

            //获取一个Node(DOM基本的数据类型)集合 (route)
            NodeList nodes = rootElement.getElementsByTagName("devices");
            //遍历Note集合
            for (int i = 0; i < nodes.getLength(); i++) {
                Node childNode = nodes.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    devicesSet.add(childNode.getTextContent());
                }
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
}
