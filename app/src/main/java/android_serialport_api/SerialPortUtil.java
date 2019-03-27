package android_serialport_api;

import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class SerialPortUtil {
    private static final String TAG = "SerialPortUtil";
    private static SerialPortUtil INSTANCE = null;
    private SerialPort serialPort;
    private OutputStream outputStream;
    private InputStream inputStream;
    private android_serialport_api.SCMDataReceiveListener SCMDataReceiveListener;

    private SerialPortUtil() {

    }

    public static SerialPortUtil getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SerialPortUtil();
        }
        return INSTANCE;
    }

    public void setSCMDataReceiveListener(android_serialport_api.SCMDataReceiveListener SCMDataReceiveListener) {
        this.SCMDataReceiveListener = SCMDataReceiveListener;
    }

    //打开串口
    public void openSerialPort() {
        Log.d(TAG, "openSerialPort");
        try {

            final String portPath = "/dev/ttyS1";
            final int baudRate = 9600;

            serialPort = new SerialPort(new File(portPath), baudRate, 0);
            outputStream = serialPort.getOutputStream();
            inputStream = serialPort.getInputStream();

            new ReadThread().start();

        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    //关闭串口
    public void closeSerialPort() {

        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (serialPort != null) {
                serialPort.close();
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    //发送数据
    public void sendDataToSerialPort(byte[] data) {

        try {

            int dataLength = data.length;
//            Log.d(TAG, "DATASIZE:" + String.valueOf(dataLength));

            if (dataLength > 0) {
                outputStream.write(data);
                outputStream.write('\n');
                outputStream.flush();
            }

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    //接收数据的线程
    public class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            Log.d(TAG, "ReadThread");
            while (!isInterrupted()) {
                int size;
                try {
//					Log.d(TAG,"=======");
                    byte[] buffer = new byte[1];
                    if (inputStream == null) return;
                    size = inputStream.read(buffer);
                    if (size > 0) {
                        /*监听数据接收*/
                        SCMDataReceiveListener.dataRecevie(buffer, size);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
            Log.d(TAG, "ReadThread重新启动");
            new ReadThread().start();
        }
    }

}