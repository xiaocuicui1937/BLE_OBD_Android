package com.castel.obd;

/**
 * Created by hh on 2017/2/16.
 * 通过调用.so库运行crc算法获取校验码
 */

public class OBD {
    static {
        System.loadLibrary("CRC");
    }
    public static native int CRC(String command);
}
