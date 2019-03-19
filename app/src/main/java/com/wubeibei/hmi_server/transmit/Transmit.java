package com.wubeibei.hmi_server.transmit;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.alibaba.fastjson.JSONObject;
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
import java.util.Objects;

import static com.wubeibei.hmi_server.transmit.Class.HMI.AIR_GRADE_OFF;
import static com.wubeibei.hmi_server.transmit.Class.HMI.AIR_GRADE_SIX_GEAR;
import static com.wubeibei.hmi_server.transmit.Class.HMI.AIR_MODEL_AWAIT;
import static com.wubeibei.hmi_server.transmit.Class.HMI.DRIVE_MODEL_AUTO_AWAIT;
import static com.wubeibei.hmi_server.transmit.Class.HMI.Ord_Alam_POINTLESS;
import static com.wubeibei.hmi_server.transmit.Class.HMI.Ord_SystemRuningStatus_ONINPUT;
import static com.wubeibei.hmi_server.transmit.Class.HMI.POINTLESS;
import static com.wubeibei.hmi_server.transmit.Class.HMI.eBooster_Warning_OFF;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_Alam;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_DangerAlarm;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_Demister_Control;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_DoorLock;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_Driver_model;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_FANPWM_Control;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_HighBeam;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_LeftTurningLamp;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_LowBeam;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_RearFogLamp;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_RightTurningLamp;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_SystemRuningStatus;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_TotalOdmeter;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_air_grade;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_air_model;
import static com.wubeibei.hmi_server.transmit.bean.IntegerCommand.HMI_Dig_Ord_eBooster_Warning;
import static com.wubeibei.hmi_server.util.ByteUtil.bytesToHex;
import static com.wubeibei.hmi_server.util.ByteUtil.subBytes;

public class Transmit {
    private final static String TAG = "Transmit";
    private final static int MESSAGELENGTH = 14;
    private final int PORT = 4001;   // port号
    private final static String IP = "192.168.1.60"; // ip地址
    private boolean threadFlag = true; // 接收线程是否关闭

    public static void main(String[] args) {
        new Transmit();
    }

    private static class Holder{
        static Transmit instance = new Transmit();
    }

    private Transmit() {
        init();
        start();
    }

    public void setADAndRCUFlag(boolean flag) {
        try {
            ((BaseClass) Objects.requireNonNull(NAME_AND_CLASS.get("HMI"))).setFlag(flag);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    public void HostToCAN(String clazz, int field, Object o) {
        BaseClass baseClass = (BaseClass) NAME_AND_CLASS.get(clazz);
        if (baseClass == null) {
            LogUtil.d(TAG, "类转换错误");
            return;
        }
        if (baseClass instanceof HMI)
            ((HMI) baseClass).changeStatus(field, o);
    }

    // 发送队列线程
    private class SendToCan implements Runnable {
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
    public void Can_init() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(HMI_Dig_Ord_HighBeam, POINTLESS);//远光灯
        map.put(HMI_Dig_Ord_LowBeam, POINTLESS);//近光灯
        map.put(HMI_Dig_Ord_LeftTurningLamp, POINTLESS);//左转向灯
        map.put(HMI_Dig_Ord_RightTurningLamp, POINTLESS);//右转向灯
        map.put(HMI_Dig_Ord_RearFogLamp, POINTLESS);//后雾灯
        map.put(HMI_Dig_Ord_DangerAlarm, POINTLESS);//警示灯
        map.put(HMI_Dig_Ord_air_model, AIR_MODEL_AWAIT);//制冷，制热
        map.put(HMI_Dig_Ord_Alam, Ord_Alam_POINTLESS);//低速报警
        map.put(HMI_Dig_Ord_Driver_model, DRIVE_MODEL_AUTO_AWAIT);//驾驶模式
        map.put(HMI_Dig_Ord_DoorLock, POINTLESS);//门锁控制
        map.put(HMI_Dig_Ord_air_grade, AIR_GRADE_SIX_GEAR);//空调档位
        map.put(HMI_Dig_Ord_eBooster_Warning, eBooster_Warning_OFF);//制动液面报警
        map.put(HMI_Dig_Ord_FANPWM_Control, AIR_GRADE_OFF);//风扇PWM占空比控制信号
        map.put(HMI_Dig_Ord_Demister_Control, POINTLESS);//除雾控制
        map.put(HMI_Dig_Ord_TotalOdmeter, 0);//总里程
        map.put(HMI_Dig_Ord_SystemRuningStatus, Ord_SystemRuningStatus_ONINPUT);//HMI控制器运行状态
        HMI HMI_Class = (HMI) NAME_AND_CLASS.get("HMI");

        for (Map.Entry<Integer, Integer> it : map.entrySet()) {
            int field = it.getKey();
            Object o = it.getValue();
            HMI_Class.changeStatus(field, o);
        }

        byte[] bytes = HMI_Class.getBytes();
        LogUtil.d(TAG, "车辆初始化：" + bytesToHex(bytes));
        UDP_send(bytes);
    }

    private void start() {
        new Thread(new SendToCan()).start(); // 开启发送线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                UDP_receive_Can();
            }
        }).start(); // 开启接收线程
    }

    public static Transmit getInstance() {
        return Holder.instance;
    }

    public void sendToPad(JSONObject jsonObject, int target) {

    }

    // 接收CAN总线
    private void UDP_receive_Can() {
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
        }finally {
            UDP_receive_Can();
        }
    }

    // 发到CAN总线
    private void UDP_send(byte[] sendMsgs) {
        DatagramSocket datagramSocket = null;
        DatagramPacket datagramPacket;
        try {
            datagramSocket = new DatagramSocket();
            datagramPacket = new DatagramPacket(sendMsgs, sendMsgs.length, InetAddress.getByName(IP), PORT);
            datagramSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }

    // 消息标识符
    private ArrayList<Pair<String, ? extends BaseClass>> list = new ArrayList<>(Arrays.asList(
            new Pair<>("00000361", new BCM1()),
            new Pair<>("000004cf", new AD4()),
            new Pair<>("000004c0", new ESC3()),
            new Pair<>("00000331", new PCGL1()),
            new Pair<>("00000333", new PCGR1()),
            new Pair<>("00000383", new HMI()),
            new Pair<>("00000235", new OBU5()),
            new Pair<>("00000236", new HAD5()),
            new Pair<>("00000237", new HAD6()),
            new Pair<>("00000260", new BMS1()),
            new Pair<>("00000421", new VCU4()),
            new Pair<>("00000465", new BMS7()),
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
    }

    // 处理收到的byte数组
    private void dispose(byte[] receMsgs) {
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
