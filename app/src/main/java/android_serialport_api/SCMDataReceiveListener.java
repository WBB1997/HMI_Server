package android_serialport_api;

public interface SCMDataReceiveListener {
    void dataRecevie(byte[] data, int size);
}