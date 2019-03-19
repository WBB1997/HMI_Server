package com.wubeibei.hmi_server.transmit;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.wubeibei.hmi_server.transmit.Class.AD1AndRCU1;
import com.wubeibei.hmi_server.transmit.Class.AD4;
import com.wubeibei.hmi_server.transmit.Class.BCM1;
import com.wubeibei.hmi_server.transmit.Class.BMS1;
import com.wubeibei.hmi_server.transmit.Class.BMS7;
import com.wubeibei.hmi_server.transmit.Class.BaseClass;
import com.wubeibei.hmi_server.transmit.Class.ESC3;
import com.wubeibei.hmi_server.transmit.Class.HAD5;
import com.wubeibei.hmi_server.transmit.Class.HAD6;
import com.wubeibei.hmi_server.transmit.Class.HMI;
import com.wubeibei.hmi_server.transmit.Class.OBU5;
import com.wubeibei.hmi_server.transmit.Class.PCGL1;
import com.wubeibei.hmi_server.transmit.Class.PCGR1;
import com.wubeibei.hmi_server.transmit.Class.VCU1;
import com.wubeibei.hmi_server.transmit.Class.VCU4;
import com.wubeibei.hmi_server.util.LogUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.wubeibei.hmi_server.util.ByteUtil.bytesToHex;
import static com.wubeibei.hmi_server.util.ByteUtil.subBytes;

public class Transmit {
    private final static String TAG = "Transmit";
    private final static int MESSAGELENGTH = 14;
    private final int PORT = 4001;   // port号
    private final static String IP = "192.168.1.60"; // ip地址
    private Context mContext;
    private boolean threadFlag = true; // 接收线程是否关闭

    public static void main(String[] args) {
        new Transmit();
    }

    private static class Holder{
        static Transmit instance = new Transmit();
    }

    private Transmit() {
        init();
    }

    public void setADAndRCUFlag(boolean flag) {
        ((BaseClass) NAME_AND_CLASS.get("HMI")).setFlag(flag);
    }

    // 主机发送数据给CAN总线
    public void hostToCAN(String clazz, int field, Object o) {
        BaseClass baseClass = (BaseClass) NAME_AND_CLASS.get(clazz);
        if (baseClass == null) {
            LogUtil.d(TAG, "类转换错误");
            return;
        }
        if (baseClass instanceof HMI)
            ((HMI) baseClass).changeStatus(field, o);
    }

    // 发送队列线程
    private class StartSend implements Runnable {

        @Override
        public void run() {
            try {
                while (threadFlag) {
                    Pair<byte[], byte[]> tmp = ((HMI) NAME_AND_CLASS.get("HMI")).getPairByte();
                    for (int i = 0; i < 5; i++) {
                        Thread.sleep(200);
                        UDP_send(tmp.first);
                        Log.d(TAG,i + ":" + "主机向车辆CAN总线发的信息:" + bytesToHex(tmp.first));
                    }
                    Thread.sleep(200);
                    UDP_send(tmp.second);
                    Log.d(TAG, "主机向车辆CAN总线发的无意义信息:" + bytesToHex(tmp.second));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LogUtil.d(TAG, "发送队列退出");
        }
    }

    // 车辆初始化
    public void Can_init(Object obj) {
        HMI HMI_Class = (HMI) NAME_AND_CLASS.get("HMI");
        Map<Integer, Integer> map = (Map<Integer, Integer>) obj;
        for (Map.Entry<Integer, Integer> it : map.entrySet()) {
            int field = it.getKey();
            Object o = it.getValue();
            HMI_Class.changeStatus(field, o);
        }
        byte[] bytes = HMI_Class.getBytes();
        LogUtil.d(TAG, "车辆初始化：" + bytesToHex(bytes));
        UDP_send(bytes);
    }

//    public void setHandler(Context mContext, MyHandler handler) {
//        this.mContext = mContext;
//        this.handler = handler;
//        new Thread(new StartSend()).start(); // 开启发送线程
//        UDP_receive(); // 开启接收线程
//    }

    public static Transmit getInstance() {
        return Holder.instance;
    }

    // 回调
//    public void callback(JSONObject jsonObject, int target) {
//        //通过message 发给ui
//        Message msg = handler.obtainMessage();
//        msg.what = target;
//        msg.obj = jsonObject;
//        handler.sendMessage(msg);
//    }

    // 接收CAN总线
    private void UDP_receive() {
        byte[] receMsgs = new byte[MESSAGELENGTH];
        DatagramSocket datagramSocket;
        DatagramPacket datagramPacket;
        try {
            datagramSocket = new DatagramSocket(PORT);
            while (true) {
                datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
                datagramSocket.receive(datagramPacket);
                dispose(datagramPacket.getData());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        UDP_receive();
    }

    // 发到CAN总线
    private synchronized void UDP_send(final byte[] sendMsgs) {
        DatagramSocket datagramSocket = null;
        DatagramPacket datagramPacket;
        try {
            datagramSocket = new DatagramSocket();
            datagramPacket = new DatagramPacket(sendMsgs, sendMsgs.length, InetAddress.getByName(IP), PORT);
            datagramSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭socket
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }

    // 消息标识符
    private ArrayList<Pair<String, ? extends BaseClass>> list = new ArrayList<>(Arrays.asList(
//            new Pair<>("00000220", new VCU2()),
//            new Pair<>("000003e1", new EPB1()),
//            new Pair<>("00000300", new MCU1()),
//            new Pair<>("00000373", new EPS1()),
            new Pair<>("00000361", new BCM1()),
            new Pair<>("000004cf", new AD4()),
//            new Pair<>("00000227", new ESC2()),
            new Pair<>("000004c0", new ESC3()),
            new Pair<>("00000331", new PCGL1()),
            new Pair<>("00000333", new PCGR1()),
            new Pair<>("00000383", new HMI()),
//            new Pair<>("00000234", new OBU2()),
            new Pair<>("00000235", new OBU5()),
            new Pair<>("00000236", new HAD5()),
            new Pair<>("00000237", new HAD6()),
            new Pair<>("00000260", new BMS1()),
//            new Pair<>("00000420", new VCU3()),
            new Pair<>("00000421", new VCU4()),
            new Pair<>("00000465", new BMS7()),
//            new Pair<>("00000081", new AD_FeedBack()),
//            new Pair<>("00000080", new RCU_FeedBack()),
            new Pair<>("00000219", new AD1AndRCU1()),
            new Pair<>("00000222", new VCU1())
    ));
    // 消息标识符键值对，方便查找
    private Map<String, ? super BaseClass> FLAG_AND_CLASS = new HashMap<>();
    private Map<String, ? super BaseClass> NAME_AND_CLASS = new HashMap<>();

    // 初始化
    private void init() {
        for (Pair<String, ? extends BaseClass> pair : list) {
            FLAG_AND_CLASS.put(pair.first, pair.second);
            NAME_AND_CLASS.put(pair.second.getClass().getSimpleName(), pair.second);
        }
        ((HMI) NAME_AND_CLASS.get("HMI")).setNAME_AND_CLASS(NAME_AND_CLASS);
    }

    // 处理收到的byte数组
    private void dispose(final byte[] receMsgs) {
        String key;
        String check;
        key = bytesToHex(subBytes(receMsgs, 10, 4));
        check = bytesToHex(subBytes(receMsgs, 0, 2));
        LogUtil.d(TAG, "接收到的bytes:" + bytesToHex(receMsgs));
        if (!check.equals("aabb")) {
            return;
        }
        try {
            if (FLAG_AND_CLASS.containsKey(key))
                ((BaseClass) FLAG_AND_CLASS.get(key)).setBytes(subBytes(receMsgs, 2, 8));
            else
                LogUtil.d(TAG, "未找到消息表示符");
        } catch (NullPointerException e) {
            LogUtil.d(TAG, "消息流方向错误");
        }
    }
}
