package com.wubeibei.hmi_server.transmit.Class;

import com.alibaba.fastjson.JSONObject;
import com.wubeibei.hmi_server.transmit.Transmit;
import com.wubeibei.hmi_server.util.ByteUtil;
import com.wubeibei.hmi_server.util.LogUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.wubeibei.hmi_server.util.ByteUtil.countBits;


public abstract class BaseClass {
    private boolean flag = true;
    public abstract byte[] getBytes();
    public abstract String getTAG();
    public void setFlag(boolean flag) {
        this.flag = flag;
        if (!getTAG().equals("AD1AndRCU1") && flag)
            setBytes(getBytes());
    }

    public void setBytes(byte[] bytes){
        String TAG = getTAG();
        int state = getState();
        byte[] Local_bytes = getBytes();

        int index;
        int length;
        if(TAG.equals("AD4") && !Arrays.equals(bytes, Local_bytes)){
            Map.Entry<Integer, MyPair<Integer>> entry = getFields().entrySet().iterator().next();
            JSONObject jsonObject = new JSONObject();
            // id
            jsonObject.put("id", entry.getValue().getSecond().first);
            // data
            jsonObject.put("data", getValue(entry, bytes));
            // target
            int target = entry.getValue().getSecond().second;
            // 发回主函数
            Transmit.getInstance().sendToPad(jsonObject, target);
        }else {
            for (Map.Entry<Integer, MyPair<Integer>> entry : getFields().entrySet()) {
                index = entry.getKey();
                length = entry.getValue().getLength();

                if (flag || (countBits(Local_bytes, 0, index, length, state) != countBits(bytes, 0, index, length, state))) {
                    JSONObject jsonObject = new JSONObject();
                    // id
                    jsonObject.put("id", entry.getValue().getSecond().first);
                    // data
                    jsonObject.put("data", getValue(entry, bytes));
                    // target
                    int target = entry.getValue().getSecond().second;
                    // 发回主函数
                    Transmit.getInstance().sendToPad(jsonObject, target);
                }
            }
        }
        flag = false;
        System.arraycopy(bytes, 0, getBytes(), 0, bytes.length);
    }
    public abstract Object getValue(Map.Entry<Integer, MyPair<Integer>> entry, byte[] bytes);
    public abstract HashMap<Integer, MyPair<Integer>> getFields();
    public abstract int getState();
    void setBytes(int Byte_offset, int bit_index, boolean changed){
        ByteUtil.setBit(getBytes(), Byte_offset, bit_index, changed);
    }
}
