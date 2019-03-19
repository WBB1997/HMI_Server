package com.wubeibei.hmi_server.transmit.Class;

import com.wubeibei.hmi_server.transmit.bean.IntegerCommand;
import com.wubeibei.hmi_server.transmit.bean.SendFlag;
import com.wubeibei.hmi_server.util.ByteUtil;
import com.wubeibei.hmi_server.util.LogUtil;

import java.util.HashMap;
import java.util.Map;

import static com.wubeibei.hmi_server.util.ByteUtil.countBits;


public class HAD6 extends BaseClass {
    private static final String TAG = "HAD6";
    private HashMap<Integer, MyPair<Integer>> fields = new HashMap<Integer, MyPair<Integer>>(){{
        put(0,new MyPair<>(2, IntegerCommand.HAD_PedestrianAvoidanceRemind, SendFlag.FRONTSCREEN)); // 行人避让提醒
        put(2,new MyPair<>(2, IntegerCommand.HAD_EmergencyParkingRemind, SendFlag.FRONTSCREEN)); // 紧急停车提醒
        put(4,new MyPair<>(2, IntegerCommand.HAD_StartingSitedepartureRemind,SendFlag.LOCALHOST | SendFlag.DOOR | SendFlag.FRONTSCREEN)); // 起始站出发提醒
        put(6,new MyPair<>(2, IntegerCommand.HAD_ArrivingSiteRemind, SendFlag.LOCALHOST | SendFlag.DOOR | SendFlag.FRONTSCREEN)); // 到站提醒
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
            case 2:
            case 4:
            case 6:
                return (int) countBits(bytes, 0, index, 2, getState());
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
