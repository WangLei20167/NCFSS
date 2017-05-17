package utils;

/**
 * Created by Administrator on 2017/5/6 0006.
 * 这是一个int和byte[]数组相互转化的工具
 */

public class IntAndBytes {
    //int转byte[]
    public static byte[] int2byte(int res) {
        byte[] targets = new byte[4];

        targets[0] = (byte) (res & 0xff);// 最低位
        targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
        targets[2] = (byte) ((res >> 16) & 0xff);// 次高位
        targets[3] = (byte) (res >>> 24);// 最高位,无符号右移。
        return targets;
    }

    //byte[]转int
    public static int byte2int(byte[] res) {
        // 一个byte数据左移24位变成0x??000000，再右移8位变成0x00??0000
        int targets = (res[0] & 0xff) | ((res[1] << 8) & 0xff00) // | 表示安位或
                | ((res[2] << 24) >>> 8) | (res[3] << 24);
        return targets;
    }

    //java byte范围 -128到127，若是存的值为负返回int型正数
    public static int negByte2int(byte bt){
        if(bt<0){
            int abs=Math.abs(bt);
            return 256-abs;
        }else{
            return bt;
        }
    }
}
