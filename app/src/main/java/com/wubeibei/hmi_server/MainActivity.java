package com.wubeibei.hmi_server;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.wubeibei.hmi_server.transmit.Transmit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private Transmit transmit;
    private String PadIpAddress = null;
    private int PadPort = -1;
    private final int LocalPort = 4567;
    private ServerSocket server = null;
    private final Map<String, Socket> socketMap = new HashMap<>();
    private final ExecutorService mExecutorService = Executors.newCachedThreadPool(); // 创建一个线程池
    private static final boolean DEBUG = true; // 是否启动调试


    private TextView LogTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        transmit = Transmit.getInstance();
        LogTextView = findViewById(R.id.LogTextView);
        init();
    }

    private void init(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                receive();
            }
        }).start();
    }

    private void receive(){
        try {
            server = new ServerSocket(LocalPort);
            if(DEBUG)
                LogTextView.append("服务器Ip:" + server.getInetAddress().getHostAddress() + "\n监控端口:" + LocalPort);
            Socket client;
            while (true) {
                client = server.accept();
                socketMap.put(client.getInetAddress().getHostAddress(), client);
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
            receive();
        }
    }

    class Service implements Runnable {
        private Socket socket;
        private BufferedReader in = null;
        private String msg = "";

        Service(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // 客户端只要一连到服务器，便向客户端发送下面的信息。
                msg = "客户" + this.socket.getInetAddress().getHostAddress() + "加入；此时总连接：" + socketMap.size() + "（服务器发送）";
                if (DEBUG)
                    LogTextView.append("\n" + msg);
                sendmsg();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if ((msg = in.readLine()) != null) {
                        if (msg.trim().equals("exit")) {
                            System.out.println("GAME OVER");
                            socketMap.remove(socket.getInetAddress().getHostAddress());
                            in.close();
                            msg = "服务器" + socket.getInetAddress() + "退出；此时总连接：" + socketMap.size() + "（服务器发送）";
                            socket.close();
                            sendmsg();
                            break;
                        } else {
                            // 接收客户端发过来的信息msg，然后发送给客户端。
                            msg = socket.getInetAddress().getHostAddress() + "：" + msg + "（服务器发送）";
                            sendmsg();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 循环遍历客户端集合，给每个客户端都发送信息。
         */
        public void sendmsg() {
            // 在服务器上打印
//            System.out.println(msg);
//            // 遍历打印到每个客户端上
//            int num = mList.size();
//            for (int i = 0; i < num; i++) {
//                Socket mSocket = mList.get(i);
//                PrintWriter out = null;
//                try {
//                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);
//                    out.println(msg);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }
    }
}
