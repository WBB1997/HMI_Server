package com.wubeibei.hmi_server.transmit.Class;


import com.wubeibei.hmi_server.transmit.bean.IntegerCommand;
import com.wubeibei.hmi_server.transmit.bean.SendFlag;
import com.wubeibei.hmi_server.util.ByteUtil;
import com.wubeibei.hmi_server.util.LogUtil;

import java.util.HashMap;
import java.util.Map;

import static com.wubeibei.hmi_server.util.ByteUtil.countBits;

public class EPB1 extends BaseClass {
    private static final String TAG = "EPB1";
    private HashMap<Integer, MyPair<Integer>> fields = new HashMap<Integer, MyPair<Integer>>() {{
        put(8, new MyPair<>(2, IntegerCommand.EPB_Dig_Sata_Status, SendFlag.LOCALHOST)); // EPB状态
        put(14, new MyPair<>(2, IntegerCommand.EPB_Dig_Sata_Indication, SendFlag.LOCALHOST)); // EPB状态灯显示
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
            case 8:
            case 14:
                return (int) countBits(bytes, 0, index, 2, getState());
            default:
                LogUtil.d(TAG, "数据下标错误");
                break;
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
