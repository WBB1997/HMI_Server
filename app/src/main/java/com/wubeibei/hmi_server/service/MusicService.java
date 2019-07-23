package com.wubeibei.hmi_server.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private final int MUSIC = 1;
    private final int STATION_MUSIC = 2;
    private final int CAR_START = 3;//车辆出发
    private final int ARRIVED = 2;//到站和下车 0x2
    private final int ARRIVING = 1;//即将到站 0x1
    private final int ARRIVING_TERMINAL_STATION = 5;//即将到达终点站
    private final int ARRIVED_TERMINAL_STATION = 6;//到达终点站
    private String basePath = Environment.getExternalStorageDirectory() + "/minibus/sound/";//默认路径
    private String fileName = "ShanghaiRouteInfo.xml";
    private MusicBinder mBinder;
    private MediaPlayer mediaPlayer;
    private List<File> musicList = null;//存放音乐的List
    private int currentRouteNum = 0;//默认路线号
    private int index = 0;//索引位
    private static Map<Integer, Map<Integer, String>> routeMap = new HashMap<>();//路线map
    private int mFlag = MUSIC;//判断当前播放的是哪一种音乐
    private int lastIndex = 0;//上一次的索引位
    private int lastProgress = 0;//上一次的音乐进度
    private boolean isFirstIndex = true;//判断是不是第一次播放
    private int currentVideoStatu = 0;//当前投风窗状态

    public MusicService() {
    }

    public void setRouteNum(int routeNum) {
        this.currentRouteNum = routeNum;
    }

    public void setCurrentVideoStatu(int currentVideoStatu) {
        this.currentVideoStatu = currentVideoStatu;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new MusicBinder();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mBinder.completionListener);
        mediaPlayer.setOnSeekCompleteListener(mBinder.seekCompleteListener);
        try {
            InputStream in = this.getAssets().open(fileName);
            routeMap = parseXMLWithPull(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initMusicData();
        initMediaPlayerFile(index);
        mBinder.play();
    }

    /**
     * 初始化音乐资源
     */
    private void initMusicData() {
        index = 0;
        mFlag = MUSIC;
        musicList = new ArrayList<>();
//        if (isFirstIndex) {
//            musicList.add(new File(basePath, "欢迎乘坐.wav"));
//            musicList.add(new File(basePath+"mute.mp3"));
//            isFirstIndex = false;
//        }
        musicList.add(new File(basePath, "1.mp3"));
        musicList.add(new File(basePath, "2.mp3"));
        musicList.add(new File(basePath, "3.mp3"));
    }

    /**
     * 初始化站点音乐信息
     *
     * @param playType
     * @param path
     * @param endPath
     */
    private boolean initStationMusicData(int playType, String path, String endPath) {
        if (!TextUtils.isEmpty(path)) {
            index = 0;
            mFlag = STATION_MUSIC;
            musicList = new ArrayList<>();
            if (playType == ARRIVED) {//到站和下车{XXX 到了，下车请注意}
                musicList.add(new File(basePath + path));
                musicList.add(new File(basePath + "到了.wav"));
                musicList.add(new File(basePath + "下车请注意.wav"));
                musicList.add(new File(basePath+"mute.mp3"));
            } else if (playType == ARRIVING) {//即将到站{前方即将到站 XXX，请做好下车准备}
                musicList.add(new File(basePath + "前方即将到站.wav"));
                musicList.add(new File(basePath + path));
                musicList.add(new File(basePath + "请做好下车准备.wav"));
                musicList.add(new File(basePath+"mute.mp3"));
            } else if (playType == ARRIVED_TERMINAL_STATION) {//到达终点站{终点站 XXX 到了，开门请当心，下车请注意}
                musicList.add(new File(basePath + "终点站.wav"));
                musicList.add(new File(basePath + path));
                musicList.add(new File(basePath + "到了.wav"));
                musicList.add(new File(basePath + "开门请当心下车请注意.wav"));
                musicList.add(new File(basePath+"mute.mp3"));
            } else if (playType == ARRIVING_TERMINAL_STATION) {//即将到达终点站{前方即将到达终点站 XXX， 请做好下车准备}
                musicList.add(new File(basePath + "前方即将到达终点站.wav"));
                musicList.add(new File(basePath + path));
                musicList.add(new File(basePath + "请做好下车准备.wav"));
                musicList.add(new File(basePath+"mute.mp3"));
            } else if (playType == CAR_START) {//车辆出发{欢迎乘坐东风Sharing Van无人驾驶车,本车开往XXX方向,下一站XXX}
                musicList.add(new File(basePath + "欢迎乘坐.wav"));
                musicList.add(new File(basePath + "本车开往.wav"));
                musicList.add(new File(basePath + endPath));
//                musicList.add(new File(basePath + "方向.wav"));
                musicList.add(new File(basePath + "下一站.wav"));
                musicList.add(new File(basePath + path));
                musicList.add(new File(basePath+"mute.mp3"));
            }
            return true;
        }
        return false;
    }

    /**
     * 设置资源
     *
     * @param index
     */
    private synchronized void initMediaPlayerFile(int index) {
        try {
            String path = musicList.get(index).getPath();
            Log.d(TAG, "initMediaPlayerFile: "+path);
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
        } catch (Exception e) {
            Log.d(TAG, "设置资源，准备阶段出错");
            e.printStackTrace();
        }
    }

    /**
     * 返回相应站点的信息
     *
     * @param stationNum
     * @return
     */
    private String getStaMusicInfo(int stationNum) {
        String info = "";
        for (Map.Entry<Integer, Map<Integer, String>> route : routeMap.entrySet()) {
            int routeNum = route.getKey();
            if (routeNum == currentRouteNum) {//为当前路线编号
                Map<Integer, String> mRoute = route.getValue();
                if (stationNum < mRoute.size() && stationNum >= 0) {//站点在范围之内
                    info = mRoute.get(stationNum);
                }
            }
        }
        return info;
    }

    /**
     * 返回当前路线的站点总数
     *
     * @return
     */
    private int getStationNum() {
        int num = 0;
        for (Map.Entry<Integer, Map<Integer, String>> route : routeMap.entrySet()) {
            int routeNum = route.getKey();
            if (routeNum == currentRouteNum) {//为当前路线编号
                Map<Integer, String> mRoute = route.getValue();
                num = mRoute.size();
            }
        }
        return num;
    }

    /**
     * 返回所有路线信息
     *
     * @return
     */
    public String printRouteInfo() {
        StringBuffer info = new StringBuffer();
        for (Map.Entry<Integer, Map<Integer, String>> route : routeMap.entrySet()) {
            info.append(route.toString() + "\n");
        }
        return info.toString();
    }

    /**
     * 解析路线XML文件
     *
     * @param in
     * @return 存取当前路线号的Map
     */
    private Map<Integer, Map<Integer, String>> parseXMLWithPull(InputStream in) {
        Map<Integer, Map<Integer, String>> routeMap = new HashMap<>();
        int routeCount = -1;//路线数
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = factory.newPullParser();
            xmlPullParser.setInput(in, "UTF-8");
            int id = -1;//站点号
            String path = "";//站点相应的音乐路径
            String chsName = "";//中文站点名
            Map<Integer, String> route = null;//路线号
            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String nodeName = xmlPullParser.getName();
                switch (eventType) {
                    //开始解析某个节点
                    case XmlPullParser.START_TAG: {
                        if ("Route".equals(nodeName)) {//路线号
                            routeCount++;
                            id = -1;
                            route = new HashMap<>();//初始化路线
                        } else if ("item".equals(nodeName)) {//站点
                            id++;
                        } else if ("chs".equals(nodeName)) {//站点中文名

                        } else if ("en".equals(nodeName)) {//站点英文名

                        } else if ("path".equals(nodeName)) {//站点音乐路径
                            path = xmlPullParser.nextText();
                            route.put(id, path);
                        }
                        break;
                    }
                    //完成解析某个节点
                    case XmlPullParser.END_TAG: {
                        if ("Route".equals(nodeName)) {
                            routeMap.put(routeCount, route);
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return routeMap;
    }

    public class MusicBinder extends Binder {

        /**
         * 设置路线号
         * @param routeNum
         */
        public void setLoRouteNum(int routeNum){
            setRouteNum(routeNum);
        }

        /**
         * 设置当前视频状态
         * @param currentVideoStatus
         */
        public void setVCurrentVideoStatus(int currentVideoStatus) {
            setCurrentVideoStatu(currentVideoStatus);
        }

        //准备数据
        public void prepareData(int playType, int stationNum) {
//            Log.d(TAG, "prepareData: " + playType + "  " + stationNum);
            int currentStationNum = getStationNum();//当前路线的总站点数
            if (stationNum == currentStationNum - 1) {//如果为终点站
                if (playType == ARRIVED) {//如果现在是已经到站
                    playType = ARRIVED_TERMINAL_STATION;//设置为到达终点站
                } else if (playType == ARRIVING) {
                    playType = ARRIVING_TERMINAL_STATION;//设置为即将到达终点站
                }
            }
            if (mediaPlayer.isPlaying() && mFlag == MUSIC) {//正在播放车载音乐
                lastIndex = index;
                lastProgress = getCurrentPosition();
//                Log.d(TAG, "正在播放车载音乐: index="+index+","+"lastIndex="+lastIndex+","+"lastProgress="+lastProgress);
            }
            String path = getStaMusicInfo(stationNum);//下一站点语音路径
            String endPath = getStaMusicInfo(currentStationNum - 1);//终点站语音路径
//            Log.d(TAG, "prepareData: "+ path + ":" + endPath);
            if (initStationMusicData(playType, path, endPath)) {
                try {
//                    if (mediaPlayer.isPlaying()) {//正在播放
//                        index = 0;
//                        mediaPlayer.reset();//重置播放器
//                    }
                    mediaPlayer.reset();//重置播放器
                    initMediaPlayerFile(index);
                    play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean playType2Music(){
            if(mFlag == MUSIC){
                return true;
            }
            return false;
        }

        public boolean isPlaying(){
            return mediaPlayer.isPlaying();
        }

        //播放
        public void play() {
//            if (index == 0) {
//                initMediaPlayerFile(index);
//            }
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
            Log.d(TAG, "play");
        }

        //下一曲
        private void nextMusic() {
            if ((mediaPlayer != null) && (index >= 0 && index < musicList.size())) {//List中还有音乐
                mediaPlayer.reset();
                initMediaPlayerFile(index);
                play();
            } else {//播放结束
//                Log.d(TAG, "nextMusic: "+"播放结束"+currentVideoStatu);
                if (mediaPlayer != null) {
                    mediaPlayer.reset();
                    if (mFlag == MUSIC) {
                        index = 0;
                        lastProgress = 0;//重新开始
                        initMusicData();//初始化车载音乐
                    } else {
                        initMusicData();//初始化车载音乐
                        index = lastIndex;//定位至上次播放的地方
                    }
                    initMediaPlayerFile(index);
                    seekToPosition(lastProgress);
                }
            }
        }

        //暂停
        public void pause() {
//            if(mFlag != MUSIC){//当前不在播放音乐
//                return;
//            }
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                Log.d(TAG, "pause");
            }
        }

        //关闭
        public void close() {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                Log.d(TAG, "close");
            }
        }

        //获取总进度
        public int getProgress() {
            int progress = 0;
            if (mediaPlayer != null) {
                progress = mediaPlayer.getDuration();
            }
            return progress;
        }

        //获取当前进度
        public int getCurrentPosition() {
            int currentPosition = 0;
            if (mediaPlayer != null) {
                currentPosition = mediaPlayer.getCurrentPosition();
            }
            return currentPosition;
        }

        //播放指定位置
        public void seekToPosition(int msec) {
            mediaPlayer.seekTo(msec);
        }

        /**
         *
         */
        private MediaPlayer.OnSeekCompleteListener seekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
//                Log.d(TAG, "onSeekComplete: ");
                //判断当前投风窗所处状态
                if(currentVideoStatu == 1){
                    return;
                }
                mp.start();
            }
        };

        /**
         * 播放结束
         */
        private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                index++;
                nextMusic();
            }
        };
    }

}
