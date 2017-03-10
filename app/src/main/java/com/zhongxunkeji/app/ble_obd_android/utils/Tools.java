package com.zhongxunkeji.app.ble_obd_android.utils;


import com.castel.obd.OBD;

/**
 * Created by hh on 2017/2/16.
 * 工具类
 */

public class Tools {
    private static final String SERAILNUM = "215B20160000022";
   /**
    * 十进制转换为16进制字符串
    * @param code 10进制的整形
    * @return String 16进制的字符串
    * */
    private static String toHexString(int code){
        String res = Integer.toHexString(code);
        if (res.length()%2==1){//当字符串的长度不是2的正数数倍时，说明需要在字符串的高位补位
            res = "0"+res;
        }
        return res.toUpperCase();
    }
    /**
     * 发送查询vin(车辆识别代码)请求
     * @return String
     * //@@215B20160000022,QI,1,A07,*
     * */
    public static String qiParam(){
        String checkCode = "@@"+SERAILNUM+",QI,1,A07,*";
        return checkCode+toHexString(OBD.CRC(checkCode));
    }
    /**
     * 发送恢复出厂设置的指令
     * @return String
     * */
    public static String resetDefalutParam(){
        String checkCode = "@@"+SERAILNUM+",CI,RTD,*";
        return checkCode+toHexString(OBD.CRC(checkCode));
    }

    /**
     * 发送重启终端的指令
     * @return String 返回字符串
     * */
    public static String resetParam(){
        String checkCode = "@@"+SERAILNUM+",CI,RESET,*";
        return checkCode+toHexString(OBD.CRC(checkCode));
    }
}
