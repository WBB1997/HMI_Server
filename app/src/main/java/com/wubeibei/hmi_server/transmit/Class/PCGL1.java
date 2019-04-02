package com.wubeibei.hmi_server.transmit.Class;

import com.wubeibei.hmi_server.transmit.bean.IntegerCommand;
import com.wubeibei.hmi_server.transmit.bean.SendFlag;
import com.wubeibei.hmi_server.util.ByteUtil;
import com.wubeibei.hmi_server.util.LogUtil;

import java.util.HashMap;
import java.util.Map;

import static com.wubeibei.hmi_server.util.ByteUtil.countBits;

public class PCGL1 extends BaseClass {
    private static final String TAG = "PCGL1";
    private HashMap<Integer, MyPair<Integer>> fields = new HashMap<Integer, MyPair<Integer>>(){{
        put(0,new MyPair<>(3, IntegerCommand.PCG_Left_Work_Sts, SendFlag.DOOR | SendFlag.FRONTSCREEN)); // 左门状态信息
        put(3,new MyPair<>(3, IntegerCommand.PCG_Left_Error_Mode, SendFlag.DOOR)); // 左门故障模式
        put(6,new MyPair<>(2, IntegerCommand.PCG_Left_Anti_Pinch_Mode, SendFlag.DOOR)); // 左门防夹类型
        put(8,new MyPair<>(8, IntegerCommand.PCG_Left_Open_Count, SendFlag.DOOR)); // 左门开门角度信息
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
            case 3:
                return (int) countBits(bytes, 0, index, 3, getState());
            case 6:
                return (int) countBits(bytes, 0, index, 2, getState());
            case 8:
                return countBits(bytes, 0, index, 8, getState());
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
