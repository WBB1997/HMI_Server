package com.wubeibei.hmi_server.transmit.Class;

import com.wubeibei.hmi_server.transmit.bean.IntegerCommand;
import com.wubeibei.hmi_server.transmit.bean.SendFlag;
import com.wubeibei.hmi_server.util.ByteUtil;
import com.wubeibei.hmi_server.util.LogUtil;

import java.util.HashMap;
import java.util.Map;

import static com.wubeibei.hmi_server.util.ByteUtil.countBits;

public class VCU4 extends BaseClass {
    private static final String TAG = "VCU4";
    private HashMap<Integer, MyPair<Integer>> fields = new HashMap<Integer, MyPair<Integer>>(){{
        put(16,new MyPair<>(16, IntegerCommand.can_RemainKm, SendFlag.LOCALHOST)); // 剩余里程数;
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
            case 16:
                return countBits(bytes, 0, index, 16, ByteUtil.Intel);
            default:
                LogUtil.d(TAG, "数据下标错误");
        }
        return null;
    }

    @Override
    public int getState() {
        return ByteUtil.Intel;
    }

    @Override
    public HashMap<Integer, MyPair<Integer>> getFields() {
        return fields;
    }
}
