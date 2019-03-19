package com.wubeibei.hmi_server;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.wubeibei.hmi_server.transmit.Transmit;
import com.wubeibei.hmi_server.util.LogUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Transmit transmit;
    private final int LocalPort = 4567;
    private ServerSocket server = null;
    private static final boolean DEBUG = true; // 是否启动调试

    private final Map<String, Service> socketMap = new HashMap<>(); // 所有的socket
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
    private void init(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                ReceiveForPad();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        JSONObject object = blockingQueue.take();
                        Iterator<Map.Entry<String, Service>> it = socketMap.entrySet().iterator();
                        while(it.hasNext()){
                            Map.Entry<String, Service> entry=it.next();
                            Service value = entry.getValue();
                            try {
                                value.sendUrgentData(0xFF);
                                value.sendmsg(object.toJSONString());
                            }catch (IOException e) {
                                if(DEBUG)
                                    LogTextView.append("\n" + value.getPadIpAddress() + "已掉线");
                                it.remove();
                            }
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
            server = new ServerSocket(LocalPort);
            if(DEBUG)
                LogTextView.append("服务器Ip:" + server.getInetAddress().getHostAddress() + "\n监控端口:" + LocalPort);
            Socket client;
            while (true) {
                client = server.accept();
                socketMap.put(client.getInetAddress().getHostAddress(), new Service(client));
                mExecutorService.execute(socketMap.get(client.getInetAddress().getHostAddress()));
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
        private String msg = "";
        private String PadIpAddress;
        private int PadPort;

        Service(Socket socket) {
            this.socket = socket;
            PadIpAddress = socket.getInetAddress().getHostAddress();
            PadPort = socket.getPort();
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                if (DEBUG)
                    LogTextView.append("\n" + "客户端 :" + PadIpAddress + "/" + String.valueOf(PadPort) + "加入；此时总连接：" + socketMap.size() + "（服务器发送）");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendUrgentData (int data) throws IOException {
            socket.sendUrgentData(data);
        }

        public String getPadIpAddress() {
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

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 发送消息
        public void sendmsg(String msg) {
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
