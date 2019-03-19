package com.wubeibei.hmi_server.transmit.Class;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.wubeibei.hmi_server.transmit.bean.IntegerCommand;
import com.wubeibei.hmi_server.transmit.bean.SendFlag;
import com.wubeibei.hmi_server.util.ByteUtil;
import com.wubeibei.hmi_server.util.LogUtil;

import java.util.HashMap;
import java.util.Map;

import static com.wubeibei.hmi_server.util.ByteUtil.countBits;


public class OBU5 extends BaseClass {
    private static final String TAG = "OBU5";
    private HashMap<Integer, MyPair<Integer>> fields = new HashMap<Integer, MyPair<Integer>>(){{
        put(0,new MyPair<>(4, IntegerCommand.OBU_LocalTime, SendFlag.LOCALHOST)); // 年;
        put(4,new MyPair<>(4, IntegerCommand.OBU_LocalTime, SendFlag.LOCALHOST)); // 月;
        put(8,new MyPair<>(5, IntegerCommand.OBU_LocalTime, SendFlag.LOCALHOST)); // 日;
        put(16,new MyPair<>(6, IntegerCommand.OBU_LocalTime, SendFlag.LOCALHOST)); // 时;
        put(24,new MyPair<>(6, IntegerCommand.OBU_LocalTime, SendFlag.LOCALHOST)); // 分;
        put(32,new MyPair<>(6, IntegerCommand.OBU_LocalTime, SendFlag.LOCALHOST)); // 秒;
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
            case 4:
            case 8:
            case 16:
            case 24:
            case 32:
                JSONObject time = new JSONObject();
                int year = 2018 + (int)countBits(bytes,0,0,4, getState());
                int month = (int)countBits(bytes,0,4,4,getState());
                int day = (int)countBits(bytes,0,8,5,getState());
                int hour = (int)countBits(bytes,0,16,5,getState());
                int minute = (int)countBits(bytes,0,24,6,getState());
                int second = (int)countBits(bytes,0,32,6,getState());
                try {
                    time.put("year", year);
                    time.put("month", month);
                    time.put("day", day);
                    time.put("hour", hour);
                    time.put("minute", minute);
                    time.put("second", second);
                }catch (JSONException e) {
                    e.printStackTrace();
                }
                return time;
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