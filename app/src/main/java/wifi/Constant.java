/**
* 常量类，存储的是Android手机开热点的的热点名字以及密码
* */
package wifi;

public class Constant {
    public final static String END = "end";
    //定义创建热点的热点名字和密码
    public final static String HOST_SPOT_SSID = "HHKC";    //瀚海快传
    public final static String HOST_SPOT_PASS_WORD = "123456789";
    //作为热点时的监听端口
    public final static String TCP_ServerIP="192.168.43.1";
    public final static int TCP_ServerPORT=10000;

    //socket一端断开的标志
    public final static String END_FLAG="I am leaving";


    //服务端和客户端的标志位
    public final static String isServer="isServer";
    public final static String isClient="isClient";

}