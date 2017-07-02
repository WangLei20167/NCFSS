/**
 * 常量类，存储的是Android手机开热点的的热点名字以及密码
 */
package wifi;

public class Constant {
    //定义创建热点的热点名字和密码
    public final static String HOST_SPOT_SSID = "HHKC";    //瀚海快传
    public final static String HOST_SPOT_PASS_WORD = "123456789";
    //作为热点时的监听端口
    public final static String TCP_ServerIP = "192.168.43.1";
    public final static int TCP_ServerPORT = 10000;

    //socket一端断开的标志
    public final static String END_STRING = "I am leaving";


    //服务端和客户端的标志位
    public final static String isServer = "isServer";
    public final static String isClient = "isClient";

    public final static int BUFFER_SIZE = 1024; //设置缓存区为1K

    //约定的指令编号 instruction
    public final static int PHONE_NAME = 0;
    public final static int JSON_FILE = 1;
    public final static int PIECE_FILE_NAME = 2;  //其中包含pieceNo
    public final static int PIECE_FILE = 3;
    public final static int END_FALG = 4;
}