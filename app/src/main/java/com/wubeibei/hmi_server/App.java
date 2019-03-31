package com.wubeibei.hmi_server;

import android.app.Application;
import android.content.SharedPreferences;
import android.media.AudioManager;

import com.alibaba.fastjson.JSONObject;
import com.wubeibei.hmi_server.util.CrashHandler;

import static android_serialport_api.SreialComm.AUDIO_VOLUME;

/**
 * Created by fangju on 2018/12/28
 */
public class App extends Application {
    private static final String TAG = "App";
    private static App instance;
    private AudioManager audioManager;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        CrashHandler.getInstance().init(this);//注册本地日志
        audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
    }


    /**
     * 设置系统音量(来自485的信息)
     */
    public void setAudioVolume(JSONObject object) {
        int id = object.getIntValue("id");
        int data = object.getIntValue("data");
        if (id == AUDIO_VOLUME) {//音量
            if (data >= 0) {//音量值大于等于零
                if(data > 1&&data< 26){
                    data = (data*15)/26+1;
                }
                if(data >= 26){
                    data = 15;
                }
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, data, AudioManager.FLAG_PLAY_SOUND);
            }
        }
    }
}
