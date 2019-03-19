package com.wubeibei.hmi_server.transmit.Class;


import com.wubeibei.hmi_server.transmit.bean.IntegerCommand;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.wubeibei.hmi_server.transmit.bean.SendFlag;
import com.wubeibei.hmi_server.util.ByteUtil;
import com.wubeibei.hmi_server.util.LogUtil;

import static com.wubeibei.hmi_server.util.ByteUtil.countBits;
import static com.wubeibei.hmi_server.util.ByteUtil.viewBinary;


public class AD4 extends BaseClass {
    private static final String TAG = "EPB1";
    private HashMap<Integer, MyPair<Integer>> fields = new HashMap<Integer, MyPair<Integer>>() {{
        put(0, new MyPair<>(1, IntegerCommand.HAD_GPS_LatLng, SendFlag.LOCALHOST)); // 东西经标识
        put(1, new MyPair<>(6, IntegerCommand.HAD_GPS_LatLng, SendFlag.LOCALHOST)); // 经度_分
        put(8, new MyPair<>(8, IntegerCommand.HAD_GPS_LatLng, SendFlag.LOCALHOST)); // 经度_度
        put(18, new MyPair<>(6, IntegerCommand.HAD_GPS_LatLng, SendFlag.LOCALHOST)); // 经度_秒
        put(24, new MyPair<>(10, IntegerCommand.HAD_GPS_LatLng, SendFlag.LOCALHOST)); // 经度_秒小数
        put(32, new MyPair<>(1, IntegerCommand.HAD_GPS_LatLng, SendFlag.LOCALHOST)); // 南北纬标识
        put(33, new MyPair<>(7, IntegerCommand.HAD_GPS_LatLng, SendFlag.LOCALHOST)); // 纬度_度
        put(40, new MyPair<>(6, IntegerCommand.HAD_GPS_LatLng, SendFlag.LOCALHOST)); // 纬度_分
        put(46, new MyPair<>(1, IntegerCommand.HAD_GPSPositioningStatus, SendFlag.LOCALHOST)); // GPS状态
        put(50, new MyPair<>(6, IntegerCommand.HAD_GPS_LatLng, SendFlag.LOCALHOST)); // 纬度_秒
        put(56, new MyPair<>(10, IntegerCommand.HAD_GPS_LatLng, SendFlag.LOCALHOST)); // 纬度_秒小数
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
    public Object getValue(Map.Entry<Integer, MyPair<Integer>> entry, byte[] bytes){
        int index = entry.getKey();
        double Degree;
        double Minute;
        double Second;
        double SecondDecimal;
        double lon = 0;
        double lat = 0;
        switch (index) {
            case 0:
            case 1:
            case 8:
            case 18:
            case 24:
            case 32:
            case 33:
            case 40:
            case 50:
            case 56:
                boolean EastOrWest = viewBinary(bytes[0], 0);
                Degree = countBits(bytes,0,8,8, getState());
                Minute = countBits(bytes,0,1,6,getState());
                Second = countBits(bytes,0,18,6,getState());
                SecondDecimal = countBits(bytes,0,24,10,getState());
                lon += Degree + Minute / 60 + (Second + SecondDecimal / 1000) / 3600;
                if(!EastOrWest)
                    lon = -lon;
                boolean NorthOrSouth = viewBinary(bytes[4], 0);
                Degree = countBits(bytes,0,33,7, getState());
                Minute = countBits(bytes,0,40,6,getState());
                Second = countBits(bytes,0,56,10,getState());
                SecondDecimal = countBits(bytes,0,24,10,getState());
                lat += Degree + Minute / 60 + (Second + SecondDecimal / 1000) / 3600;
                if(!NorthOrSouth)
                    lat = -lat;
                JSONObject lnglat = new JSONObject();
                try {
                    lnglat.put("lng", lon);
                    lnglat.put("lat", lat);
                }catch (JSONException e) {
                    e.printStackTrace();
                }
                return lnglat;
            case 46:
                return viewBinary(bytes[index / 8], index % 8);
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
