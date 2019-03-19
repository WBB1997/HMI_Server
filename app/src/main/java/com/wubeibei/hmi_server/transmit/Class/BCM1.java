package com.wubeibei.hmi_server.transmit.Class;



import com.wubeibei.hmi_server.transmit.bean.IntegerCommand;
import com.wubeibei.hmi_server.transmit.bean.SendFlag;
import com.wubeibei.hmi_server.util.ByteUtil;
import com.wubeibei.hmi_server.util.LogUtil;

import java.util.HashMap;
import java.util.Map;

import static com.wubeibei.hmi_server.util.ByteUtil.countBits;
import static com.wubeibei.hmi_server.util.ByteUtil.viewBinary;


public class BCM1 extends BaseClass {
    private final String TAG = "BCM1";
    // 属性
    private HashMap<Integer, MyPair<Integer>> fields = new HashMap<Integer, MyPair<Integer>>() {{
        put(0, new MyPair<>(1, IntegerCommand.BCM_Dig_Ord_HandLightCtr, SendFlag.LOCALHOST));// 手势灯光控制信号
        put(1, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_LeftTurningLamp, SendFlag.LOCALHOST));// 左转向状态信号
        put(2, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_RightTurningLamp, SendFlag.LOCALHOST));// 右转向状态信号
        put(3,  new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_HandLightCtr, SendFlag.LOCALHOST));// 手势灯光控制状态信号
        put(4, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_HighBeam, SendFlag.LOCALHOST)); // 远光灯状态信号
        put(5, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_LowBeam, SendFlag.LOCALHOST));// 近光灯状态信号
        put(6, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_RearFogLamp, SendFlag.LOCALHOST)); // 后雾灯状态信号
        put(7, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_DangerAlarmLamp, SendFlag.LOCALHOST));// 危险报警灯控制（双闪）状态信号
        put(8, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_BrakeLamp, SendFlag.LOCALHOST));// 制动灯状态信号
        put(9, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_BackupLamp, SendFlag.LOCALHOST));// 倒车灯状态信号
        put(10, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_SeatSensor1, SendFlag.LOCALHOST));// 座椅传感器1
        put(11, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_SeatSensor2, SendFlag.LOCALHOST));// 座椅传感器2
        put(12, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_SeatSensor3, SendFlag.LOCALHOST));// 座椅传感器3
        put(13, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_SeatSensor4, SendFlag.LOCALHOST));// 座椅传感器4
        put(14, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_SeatSensor5, SendFlag.LOCALHOST));// 座椅传感器5
        put(15, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_SeatSensor6, SendFlag.LOCALHOST));// 座椅传感器6
        put(16, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_BeltsSensor1, SendFlag.LOCALHOST));// 安全带传感器1
        put(17, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_BeltsSensor2, SendFlag.LOCALHOST));// 安全带传感器2
        put(18, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_BeltsSensor3, SendFlag.LOCALHOST));// 安全带传感器3
        put(19, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_BeltsSensor4, SendFlag.LOCALHOST));// 安全带传感器4
        put(20, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_BeltsSensor5, SendFlag.LOCALHOST));// 安全带传感器5
        put(21, new MyPair<>(1, IntegerCommand.BCM_Flg_Stat_BeltsSensor6, SendFlag.LOCALHOST)); // 安全带传感器6
        put(24, new MyPair<>(8, IntegerCommand.BCM_OutsideTemp, SendFlag.LOCALHOST)); // 车外温度
        put(32, new MyPair<>(8, IntegerCommand.BCM_InsideTemp, SendFlag.LOCALHOST)); // 车内温度
        put(45, new MyPair<>(3, IntegerCommand.BCM_ACBlowingLevel, SendFlag.LOCALHOST)); // 空调风量档位
        put(44, new MyPair<>(1, IntegerCommand.BCM_DemisterStatus, SendFlag.LOCALHOST)); // 除雾状态
    }};
    private byte[] bytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public BCM1() {
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public String getTAG() {
        return TAG;
    }

    @Override
    public void setBytes(byte[] bytes) {
        super.setBytes(bytes);
    }

    @Override
    public Object getValue(Map.Entry<Integer, MyPair<Integer>> entry, byte[] bytes) {
        int index = entry.getKey();
        switch (index) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 44:
                return viewBinary(bytes[index / 8], index % 8);
            case 24:
            case 32:
                return countBits(bytes,0,index,8, getState()) * 0.5 - 20;
            case 45:
                return (int)countBits(bytes,0,index,3, getState());
            default:
                LogUtil.d(TAG, "数据下标错误");
                break;
        }
        return null;
    }

    @Override
    public int getState() {
        return ByteUtil.Motorola;
    }

    @Override
    public HashMap<Integer, MyPair<Integer>> getFields() {
        return fields;
    }
}
