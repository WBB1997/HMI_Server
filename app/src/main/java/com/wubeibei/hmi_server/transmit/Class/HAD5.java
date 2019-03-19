package com.wubeibei.hmi_server.transmit.Class;


import com.wubeibei.hmi_server.transmit.bean.IntegerCommand;
import com.wubeibei.hmi_server.transmit.bean.SendFlag;
import com.wubeibei.hmi_server.util.ByteUtil;
import com.wubeibei.hmi_server.util.LogUtil;

import java.util.HashMap;
import java.util.Map;

import static com.wubeibei.hmi_server.util.ByteUtil.countBits;

public class HAD5 extends BaseClass {
    private static final String TAG = "HAD5";
    private HashMap<Integer, MyPair<Integer>> fields = new HashMap<Integer, MyPair<Integer>>(){{
        put(0,new MyPair<>(8, IntegerCommand.HAD_CurrentDrivingRoadIDNum, SendFlag.LOCALHOST | SendFlag.DOOR | SendFlag.FRONTSCREEN)); // 当前行驶线路ID;
        put(8,new MyPair<>(8, IntegerCommand.HAD_NextStationIDNumb, SendFlag.LOCALHOST | SendFlag.DOOR | SendFlag.FRONTSCREEN)); // 下一个站点ID;
        put(24,new MyPair<>(16, IntegerCommand.HAD_DistanceForNextStation, SendFlag.LOCALHOST | SendFlag.DOOR | SendFlag.FRONTSCREEN)); // 距离下一个站点的距离;
        put(32,new MyPair<>(8, IntegerCommand.HAD_TimeForArrivingNextStation, SendFlag.LOCALHOST | SendFlag.DOOR | SendFlag.FRONTSCREEN)); // 预计到达下一个站点所需时间;
        put(40,new MyPair<>(8, IntegerCommand.HAD_StartingSiteNum, SendFlag.LOCALHOST | SendFlag.DOOR | SendFlag.FRONTSCREEN)); // 起始站点ID;
        put(48,new MyPair<>(8, IntegerCommand.HAD_EndingSiteNum, SendFlag.LOCALHOST | SendFlag.DOOR | SendFlag.FRONTSCREEN)); // 终点站点ID;
        put(56,new MyPair<>(8, IntegerCommand.HAD_PassingBySiteNum, SendFlag.LOCALHOST | SendFlag.DOOR | SendFlag.FRONTSCREEN)); // 途径站点ID;
    }};
    private byte[] bytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

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
                return (int) countBits(bytes, 0, index, 8, getState());
            case 8:
                return (int) countBits(bytes, 0, index, 8,getState());
            case 24:
                return countBits(bytes, 0, index, 16,getState()) * 0.1;
            case 32:
            case 40:
            case 48:
            case 56:
                return (int) countBits(bytes, 0, index, 8,getState());
            default:
                LogUtil.d(TAG, "数据下标错误");
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
